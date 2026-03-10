package com.zerogoat.zero.llm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Abstract LLM client interface — supports Gemini, OpenAI, and Anthropic.
 */
interface LLMClient {

    /** Provider name (for display) */
    val providerName: String

    /** Send a text chat and get a response */
    suspend fun chat(
        systemPrompt: String,
        userMessage: String,
        conversationHistory: List<ChatMessage> = emptyList()
    ): LLMResponse

    /** Send a chat with a vision (image) attachment */
    suspend fun chatWithVision(
        systemPrompt: String,
        userMessage: String,
        imageBase64: String,
        imageMimeType: String = "image/jpeg"
    ): LLMResponse

    /** Stream text response */
    fun chatStream(
        systemPrompt: String,
        userMessage: String,
        conversationHistory: List<ChatMessage> = emptyList()
    ): Flow<String> = emptyFlow()

    /** Rough token estimate for a string */
    fun estimateTokens(text: String): Int = text.length / 4
}

data class ChatMessage(
    val role: String, // "user", "assistant", "system"
    val content: String
)

data class LLMResponse(
    val text: String,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val totalTokens: Int = inputTokens + outputTokens,
    val error: String? = null
) {
    val isSuccess: Boolean get() = error == null
}
