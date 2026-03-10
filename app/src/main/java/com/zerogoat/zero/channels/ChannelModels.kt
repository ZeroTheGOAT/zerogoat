package com.zerogoat.zero.channels

/**
 * Shared data models for control channels (WhatsApp, Telegram).
 */
data class ChannelCommand(
    val channel: String,       // "whatsapp", "telegram"
    val senderId: String,      // Contact/chat identifier
    val message: String,       // The command text
    val timestamp: Long = System.currentTimeMillis()
)

data class ChannelResponse(
    val channel: String,
    val recipientId: String,
    val message: String,
    val status: String         // "executing", "done", "failed"
)

enum class ChannelStatus {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}
