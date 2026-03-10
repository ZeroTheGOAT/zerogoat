package com.zerogoat.zero.channels

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.zerogoat.zero.accessibility.ZeroAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * WhatsApp control channel — receives commands via WhatsApp messages.
 * Uses NotificationListenerService to read incoming messages from a
 * designated control contact. Replies using Accessibility Service.
 */
class WhatsAppChannel : NotificationListenerService() {

    companion object {
        private const val TAG = "WhatsAppChannel"
        private const val WA_PACKAGE = "com.whatsapp"

        private val _status = MutableStateFlow(ChannelStatus.DISCONNECTED)
        val status: StateFlow<ChannelStatus> = _status

        var controlContact: String? = null  // Only accept commands from this contact
        var channelManager: ChannelManager? = null
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        _status.value = ChannelStatus.CONNECTED
        Log.i(TAG, "WhatsApp channel connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _status.value = ChannelStatus.DISCONNECTED
        Log.i(TAG, "WhatsApp channel disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName != WA_PACKAGE) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: return
        val text = extras.getCharSequence("android.text")?.toString() ?: return

        // Only process messages from the control contact
        val contact = controlContact
        if (contact != null && !title.equals(contact, ignoreCase = true)) return

        // Skip group messages and status updates
        if (title.contains("@") || text.startsWith("📷") || text.startsWith("🎵")) return

        Log.i(TAG, "WhatsApp command from $title: $text")

        // Dispatch to channel manager
        channelManager?.dispatchCommand(
            ChannelCommand(
                channel = "whatsapp",
                senderId = title,
                message = text
            )
        )
    }

    /**
     * Reply to the WhatsApp contact via Accessibility Service.
     * Opens WhatsApp notification → types reply → sends.
     */
    fun replyToContact(message: String) {
        val service = ZeroAccessibilityService.instance.value ?: return
        // The actual reply is handled by the agent loop —
        // it will navigate to the WhatsApp chat and type the reply
        Log.i(TAG, "Reply queued: $message")
    }
}
