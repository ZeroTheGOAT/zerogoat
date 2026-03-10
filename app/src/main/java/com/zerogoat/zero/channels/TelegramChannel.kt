package com.zerogoat.zero.channels

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Telegram Bot channel — polls a user-created Telegram bot for commands.
 * User creates a bot via @BotFather, enters the token, and commands sent
 * to the bot are forwarded to Zero for execution.
 */
class TelegramChannel(
    private val botToken: String,
    private val channelManager: ChannelManager
) {
    companion object {
        private const val TAG = "TelegramChannel"
        private const val BASE_URL = "https://api.telegram.org/bot"
        private const val POLL_INTERVAL_MS = 2000L
    }

    private val _status = MutableStateFlow(ChannelStatus.DISCONNECTED)
    val status: StateFlow<ChannelStatus> = _status

    private var pollingJob: Job? = null
    private var lastUpdateId: Long = 0

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .build()

    /** Start polling for new messages */
    fun start(scope: CoroutineScope) {
        _status.value = ChannelStatus.CONNECTING
        pollingJob = scope.launch(Dispatchers.IO) {
            Log.i(TAG, "Telegram channel starting...")
            _status.value = ChannelStatus.CONNECTED
            channelManager.updateStatus("telegram", ChannelStatus.CONNECTED)

            while (isActive) {
                try {
                    pollUpdates()
                    delay(POLL_INTERVAL_MS)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Poll error: ${e.message}")
                    delay(5000) // Back off on error
                }
            }
        }
    }

    /** Stop polling */
    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
        _status.value = ChannelStatus.DISCONNECTED
        channelManager.updateStatus("telegram", ChannelStatus.DISCONNECTED)
    }

    /** Send a message to a Telegram chat */
    fun sendMessage(chatId: Long, text: String) {
        val body = JSONObject().apply {
            put("chat_id", chatId)
            put("text", text)
            put("parse_mode", "Markdown")
        }

        val request = Request.Builder()
            .url("${BASE_URL}$botToken/sendMessage")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message: ${e.message}")
        }
    }

    private fun pollUpdates() {
        val url = "${BASE_URL}$botToken/getUpdates?offset=${lastUpdateId + 1}&timeout=30"
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return
        response.close()

        val json = JSONObject(body)
        if (!json.getBoolean("ok")) return

        val results = json.getJSONArray("result")
        for (i in 0 until results.length()) {
            val update = results.getJSONObject(i)
            lastUpdateId = update.getLong("update_id")

            val message = update.optJSONObject("message") ?: continue
            val text = message.optString("text", "").trim()
            if (text.isEmpty()) continue

            val chat = message.getJSONObject("chat")
            val chatId = chat.getLong("id")
            val senderName = message.optJSONObject("from")?.optString("first_name", "User") ?: "User"

            Log.i(TAG, "Telegram command from $senderName: $text")

            // Acknowledge receipt
            sendMessage(chatId, "🤖 Zero received: _${text}_\nExecuting...")

            // Dispatch to agent
            channelManager.dispatchCommand(
                ChannelCommand(
                    channel = "telegram",
                    senderId = chatId.toString(),
                    message = text
                )
            )
        }
    }
}
