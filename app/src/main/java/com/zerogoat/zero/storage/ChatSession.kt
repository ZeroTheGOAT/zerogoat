package com.zerogoat.zero.storage

/**
 * Chat session data models — inspired by OpenClaw's ChatHistory and ChatSessionEntry.
 */
data class ChatSession(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val modelId: String? = null,
    val totalTokens: Int = 0,
    val messageCount: Int = 0
)

data class ChatMessage(
    val id: String,
    val role: String,       // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tokensUsed: Int = 0,
    val contentType: String = "text",  // "text", "image", "tool_call"
    val metadata: Map<String, String> = emptyMap()  // Extra info (step, action, etc.)
)
