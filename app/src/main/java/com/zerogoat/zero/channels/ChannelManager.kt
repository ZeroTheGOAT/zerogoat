package com.zerogoat.zero.channels

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages active control channels (WhatsApp, Telegram).
 * Receives commands from channels and dispatches them to the agent.
 */
class ChannelManager {

    companion object {
        private const val TAG = "ChannelManager"
    }

    private val _channels = MutableStateFlow<Map<String, ChannelStatus>>(emptyMap())
    val channels: StateFlow<Map<String, ChannelStatus>> = _channels

    private var onCommandReceived: ((ChannelCommand) -> Unit)? = null

    /** Set the command handler (called when a channel receives a command) */
    fun setCommandHandler(handler: (ChannelCommand) -> Unit) {
        onCommandReceived = handler
    }

    /** Dispatch a command from a channel to the agent */
    fun dispatchCommand(command: ChannelCommand) {
        Log.i(TAG, "Command from ${command.channel}: ${command.message}")
        onCommandReceived?.invoke(command)
    }

    /** Update channel status */
    fun updateStatus(channel: String, status: ChannelStatus) {
        _channels.value = _channels.value.toMutableMap().apply {
            put(channel, status)
        }
    }

    /** Send a response back to a channel */
    fun sendResponse(response: ChannelResponse) {
        Log.i(TAG, "Response to ${response.channel}: ${response.message}")
        // Actual sending is handled by the specific channel implementation
    }
}
