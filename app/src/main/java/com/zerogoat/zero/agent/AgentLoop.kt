package com.zerogoat.zero.agent

import android.accessibilityservice.AccessibilityService
import android.util.Log
import com.zerogoat.zero.accessibility.NodeActionPerformer
import com.zerogoat.zero.accessibility.UITreeExtractor
import com.zerogoat.zero.accessibility.ZeroAccessibilityService
import com.zerogoat.zero.llm.*
import com.zerogoat.zero.skills.SkillRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Core agent loop — Zero's brain.
 * Follows the cycle: capture UI → send to LLM → execute action → repeat.
 * Inspired by OpenClaw's ChatController event streaming architecture.
 */
class AgentLoop(
    private val llmClient: LLMClient,
    private val tokenTracker: TokenTracker,
    private val skillRegistry: SkillRegistry
) {
    companion object {
        private const val TAG = "AgentLoop"
        const val MAX_STEPS = 30
        const val SETTLE_DELAY_MS = 500L
    }

    private val _state = MutableStateFlow<AgentState>(AgentState.Idle)
    val state: StateFlow<AgentState> = _state

    private val _stepLog = MutableSharedFlow<StepLogEntry>(replay = 50)
    val stepLog: SharedFlow<StepLogEntry> = _stepLog

    private var currentJob: Job? = null
    private var isPaused = false
    private var awaitingConfirmation = false

    data class StepLogEntry(
        val step: Int,
        val thought: String,
        val action: String,
        val success: Boolean
    )

    /** Execute a user command */
    fun execute(task: String, scope: CoroutineScope) {
        if (_state.value !is AgentState.Idle) {
            Log.w(TAG, "Agent is busy, ignoring new task")
            return
        }

        tokenTracker.resetTask()
        currentJob = scope.launch(Dispatchers.Default) {
            runAgentLoop(task)
        }
    }

    /** Pause the agent */
    fun pause() {
        isPaused = true
        val current = _state.value
        if (current is AgentState.Acting) {
            _state.value = AgentState.Paused(current.step, "Paused by user")
        }
    }

    /** Resume the agent */
    fun resume() {
        isPaused = false
    }

    /** Cancel the current task */
    fun cancel() {
        currentJob?.cancel()
        currentJob = null
        _state.value = AgentState.Idle
        isPaused = false
        awaitingConfirmation = false
    }

    /** User confirmed a pending action (e.g., payment) */
    fun confirmAction() {
        awaitingConfirmation = false
    }

    /** User denied a pending action */
    fun denyAction() {
        awaitingConfirmation = false
        cancel()
    }

    /** Main agent loop */
    private suspend fun runAgentLoop(task: String) {
        _state.value = AgentState.Planning(task)
        Log.i(TAG, "Starting task: $task")

        // Check if a skill matches
        val skill = skillRegistry.matchSkill(task)
        val systemPrompt = skill?.customPrompt ?: PromptBuilder.SYSTEM_PROMPT

        val previousActions = mutableListOf<String>()
        var step = 0

        try {
            // Check if it can be handled locally (no API call)
            val localAction = skillRegistry.tryLocalExecution(task)
            if (localAction != null) {
                executeAction(localAction)
                _state.value = AgentState.Completed("Done locally", 1, 0)
                return
            }

            while (step < MAX_STEPS) {
                if (isPaused) {
                    delay(500)
                    continue
                }

                step++

                // 1. Get current UI tree
                val service = ZeroAccessibilityService.instance.value
                if (service == null) {
                    _state.value = AgentState.Failed(
                        "Accessibility Service not running. Enable it in Settings.",
                        step, tokenTracker.taskTokens.value
                    )
                    return
                }

                val root = service.getUIRoot()
                val uiTree = UITreeExtractor.extractCompactTree(root)

                // 2. Build prompt
                val userMessage = PromptBuilder.buildUserMessage(
                    task = task,
                    uiTree = uiTree,
                    stepNumber = step,
                    maxSteps = MAX_STEPS,
                    previousActions = previousActions
                )

                // 3. Call LLM
                val response = llmClient.chat(systemPrompt, userMessage)
                tokenTracker.record(response, llmClient.providerName)

                if (!response.isSuccess) {
                    _state.value = AgentState.Failed(
                        "LLM error: ${response.error}",
                        step, tokenTracker.taskTokens.value
                    )
                    return
                }

                // 4. Parse action
                val agentAction = ResponseParser.parse(response.text)
                if (agentAction == null) {
                    previousActions.add("(parse error)")
                    continue
                }

                _state.value = AgentState.Acting(step, MAX_STEPS, agentAction.thought, agentAction.action)
                _stepLog.emit(StepLogEntry(step, agentAction.thought, agentAction.action.toString(), true))

                // 5. Handle special actions
                when (val action = agentAction.action) {
                    is ActionType.Done -> {
                        _state.value = AgentState.Completed(action.summary, step, tokenTracker.taskTokens.value)
                        return
                    }
                    is ActionType.Fail -> {
                        _state.value = AgentState.Failed(action.reason, step, tokenTracker.taskTokens.value)
                        return
                    }
                    is ActionType.Confirm -> {
                        _state.value = AgentState.WaitingConfirmation(action.message, action)
                        awaitingConfirmation = true
                        while (awaitingConfirmation) {
                            delay(300)
                        }
                        // If cancelled during confirmation, the cancel() function sets state to Idle
                        if (_state.value is AgentState.Idle) return
                        continue
                    }
                    else -> {
                        // 6. Execute action
                        val success = executeAction(action)
                        previousActions.add("${agentAction.thought} → ${action::class.simpleName}${if (!success) " (FAILED)" else ""}")

                        // 7. Wait for screen to settle
                        delay(SETTLE_DELAY_MS)
                    }
                }
            }

            // Max steps reached
            _state.value = AgentState.Failed(
                "Reached maximum steps ($MAX_STEPS). Task may be too complex.",
                step, tokenTracker.taskTokens.value
            )

        } catch (e: CancellationException) {
            _state.value = AgentState.Idle
        } catch (e: Exception) {
            Log.e(TAG, "Agent loop error", e)
            _state.value = AgentState.Failed(
                "Error: ${e.message}",
                step, tokenTracker.taskTokens.value
            )
        }
    }

    /** Execute a single action on the device */
    private fun executeAction(action: ActionType): Boolean {
        val service = ZeroAccessibilityService.instance.value ?: return false
        val performer = NodeActionPerformer(service)

        return when (action) {
            is ActionType.Click -> performer.clickNode(action.target)
            is ActionType.LongClick -> performer.longClickNode(action.target)
            is ActionType.TypeText -> performer.typeText(action.target, action.text)
            is ActionType.Scroll -> performer.scroll(action.direction, action.target)
            is ActionType.Swipe -> service.performSwipe(
                action.startX.toFloat(), action.startY.toFloat(),
                action.endX.toFloat(), action.endY.toFloat(),
                action.durationMs
            )
            is ActionType.Back -> service.doGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            is ActionType.Home -> service.doGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            is ActionType.Recents -> service.doGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            is ActionType.Notifications -> service.doGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            is ActionType.LaunchApp -> service.launchApp(action.packageName)
            is ActionType.Wait -> { Thread.sleep(action.durationMs); true }
            is ActionType.Done -> true
            is ActionType.Fail -> false
            is ActionType.Confirm -> true
        }
    }
}
