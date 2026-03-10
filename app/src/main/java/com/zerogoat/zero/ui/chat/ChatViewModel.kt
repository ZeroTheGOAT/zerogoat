package com.zerogoat.zero.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zerogoat.zero.agent.AgentLoop
import com.zerogoat.zero.agent.AgentState
import com.zerogoat.zero.llm.*
import com.zerogoat.zero.skills.SkillRegistry
import com.zerogoat.zero.storage.PreferencesManager
import com.zerogoat.zero.storage.SecureKeyStore
import com.zerogoat.zero.storage.TaskHistory
import com.zerogoat.zero.voice.VoiceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the main chat screen.
 * Manages chat state, agent loop, and voice input.
 * Inspired by OpenClaw's MainViewModel architecture.
 */
class ChatViewModel(app: Application) : AndroidViewModel(app) {

    data class ChatMessage(
        val text: String,
        val isUser: Boolean,
        val isThinking: Boolean = false,
        val step: Int? = null,
        val tokensUsed: Int? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val keyStore = SecureKeyStore(app)
    private val prefs = PreferencesManager(app)
    private val taskHistory = TaskHistory(app)
    private val tokenTracker = TokenTracker()
    private val skillRegistry = SkillRegistry()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _agentState = MutableStateFlow<AgentState>(AgentState.Idle)
    val agentState: StateFlow<AgentState> = _agentState

    val voiceEngine = VoiceEngine(app)
    private var agentLoop: AgentLoop? = null

    init {
        // Initialize voice with command handler
        voiceEngine.initialize { command ->
            sendMessage(command)
        }

        // Add welcome message
        _messages.value = listOf(
            ChatMessage(
                text = "Hey! I'm Zero, your AI assistant. Tell me what to do — I can open apps, order food, change settings, and more. 🤖",
                isUser = false
            )
        )
    }

    /** Send a text command to Zero */
    fun sendMessage(text: String) {
        if (text.isBlank()) return

        // Add user message
        _messages.value = _messages.value + ChatMessage(text = text, isUser = true)
        _isProcessing.value = true

        // Create LLM client based on active provider
        val llmClient = createLLMClient()
        if (llmClient == null) {
            _messages.value = _messages.value + ChatMessage(
                text = "⚠️ No API key configured. Go to Settings to add one.",
                isUser = false
            )
            _isProcessing.value = false
            return
        }

        // Create and run agent loop
        agentLoop = AgentLoop(llmClient, tokenTracker, skillRegistry)
        val loop = agentLoop!!

        // Observe agent state
        viewModelScope.launch {
            loop.state.collect { state ->
                _agentState.value = state
                handleStateChange(state, text)
            }
        }

        // Observe step log
        viewModelScope.launch {
            loop.stepLog.collect { entry ->
                // Update thinking message
                val thinkingMsg = ChatMessage(
                    text = "🧠 Step ${entry.step}: ${entry.thought}",
                    isUser = false,
                    isThinking = true,
                    step = entry.step
                )
                // Replace last thinking message or add new
                val msgs = _messages.value.toMutableList()
                val lastThinking = msgs.indexOfLast { it.isThinking }
                if (lastThinking >= 0) {
                    msgs[lastThinking] = thinkingMsg
                } else {
                    msgs.add(thinkingMsg)
                }
                _messages.value = msgs
            }
        }

        // Execute
        loop.execute(text, viewModelScope)
    }

    /** Handle agent state changes */
    private fun handleStateChange(state: AgentState, originalCommand: String) {
        when (state) {
            is AgentState.Completed -> {
                _isProcessing.value = false
                // Remove thinking message and add completion
                val msgs = _messages.value.filter { !it.isThinking }.toMutableList()
                msgs.add(ChatMessage(
                    text = "✅ ${state.summary}\n📊 ${state.totalSteps} steps · ${state.tokensUsed} tokens · ${tokenTracker.formatCost()}",
                    isUser = false,
                    tokensUsed = state.tokensUsed
                ))
                _messages.value = msgs

                // Save to history
                taskHistory.add(TaskHistory.TaskEntry(
                    command = originalCommand,
                    result = state.summary,
                    steps = state.totalSteps,
                    tokensUsed = state.tokensUsed,
                    timestamp = System.currentTimeMillis(),
                    success = true
                ))

                // Speak result if voice engine is active
                voiceEngine.speak(state.summary)
            }
            is AgentState.Failed -> {
                _isProcessing.value = false
                val msgs = _messages.value.filter { !it.isThinking }.toMutableList()
                msgs.add(ChatMessage(
                    text = "❌ ${state.reason}\n📊 ${state.step} steps · ${state.tokensUsed} tokens",
                    isUser = false
                ))
                _messages.value = msgs

                voiceEngine.speak("Sorry, I couldn't complete that. ${state.reason}")
            }
            is AgentState.WaitingConfirmation -> {
                _messages.value = _messages.value + ChatMessage(
                    text = "⚠️ ${state.message}\n\nTap ✅ to confirm or ❌ to cancel.",
                    isUser = false
                )
            }
            else -> {}
        }
    }

    /** Confirm a pending action */
    fun confirmAction() {
        agentLoop?.confirmAction()
    }

    /** Deny/cancel a pending action */
    fun denyAction() {
        agentLoop?.denyAction()
        _isProcessing.value = false
    }

    /** Cancel current task */
    fun cancelTask() {
        agentLoop?.cancel()
        _isProcessing.value = false
        _messages.value = _messages.value + ChatMessage(
            text = "🛑 Task cancelled.",
            isUser = false
        )
    }

    /** Create an LLM client based on the active provider */
    private fun createLLMClient(): LLMClient? {
        return when (keyStore.activeProvider) {
            "gemini" -> keyStore.geminiApiKey?.let { GeminiClient(it) }
            "openai" -> keyStore.openaiApiKey?.let { OpenAIClient(it) }
            "anthropic" -> keyStore.anthropicApiKey?.let { AnthropicClient(it) }
            else -> keyStore.geminiApiKey?.let { GeminiClient(it) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.release()
    }
}
