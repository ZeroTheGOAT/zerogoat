package com.zerogoat.zero.storage

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Persistent conversation memory — multi-session chat history.
 * Inspired by OpenClaw's ChatHistory + ChatSessionEntry architecture.
 *
 * Features:
 * - Multiple named sessions (like OpenClaw's session switching)
 * - Context window: sends last N messages to LLM for continuity
 * - Auto-summarization placeholder for long conversations
 * - JSON file-based storage (one file per session)
 */
class ConversationMemory(context: Context) {

    companion object {
        private const val TAG = "Memory"
        private const val MAX_CONTEXT_MESSAGES = 20
        private const val SESSIONS_FILE = "sessions.json"
    }

    private val storageDir = File(context.filesDir, "conversations").also { it.mkdirs() }

    // ===== Session Management =====

    /** Get all sessions, newest first */
    fun getAllSessions(): List<ChatSession> {
        val file = File(storageDir, SESSIONS_FILE)
        if (!file.exists()) return emptyList()

        return try {
            val array = JSONArray(file.readText())
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                ChatSession(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    createdAt = obj.getLong("createdAt"),
                    updatedAt = obj.getLong("updatedAt"),
                    modelId = obj.optString("modelId", null),
                    totalTokens = obj.optInt("totalTokens", 0),
                    messageCount = obj.optInt("messageCount", 0)
                )
            }.sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load sessions", e)
            emptyList()
        }
    }

    /** Create a new session */
    fun createSession(name: String = "New Chat"): ChatSession {
        val session = ChatSession(
            id = UUID.randomUUID().toString(),
            name = name
        )
        val sessions = getAllSessions().toMutableList()
        sessions.add(0, session)
        saveSessions(sessions)
        return session
    }

    /** Rename a session */
    fun renameSession(sessionId: String, newName: String) {
        val sessions = getAllSessions().toMutableList()
        val index = sessions.indexOfFirst { it.id == sessionId }
        if (index >= 0) {
            sessions[index] = sessions[index].copy(name = newName)
            saveSessions(sessions)
        }
    }

    /** Delete a session and its messages */
    fun deleteSession(sessionId: String) {
        val sessions = getAllSessions().filter { it.id != sessionId }
        saveSessions(sessions)
        File(storageDir, "$sessionId.json").delete()
    }

    // ===== Message Management =====

    /** Add a message to a session */
    fun addMessage(sessionId: String, message: ChatMessage) {
        val messages = getMessages(sessionId).toMutableList()
        messages.add(message)
        saveMessages(sessionId, messages)

        // Update session metadata
        val sessions = getAllSessions().toMutableList()
        val index = sessions.indexOfFirst { it.id == sessionId }
        if (index >= 0) {
            sessions[index] = sessions[index].copy(
                updatedAt = System.currentTimeMillis(),
                messageCount = messages.size,
                totalTokens = sessions[index].totalTokens + message.tokensUsed
            )
            saveSessions(sessions)
        }
    }

    /** Get all messages for a session */
    fun getMessages(sessionId: String): List<ChatMessage> {
        val file = File(storageDir, "$sessionId.json")
        if (!file.exists()) return emptyList()

        return try {
            val array = JSONArray(file.readText())
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                ChatMessage(
                    id = obj.getString("id"),
                    role = obj.getString("role"),
                    content = obj.getString("content"),
                    timestamp = obj.getLong("timestamp"),
                    tokensUsed = obj.optInt("tokensUsed", 0),
                    contentType = obj.optString("contentType", "text")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load messages for $sessionId", e)
            emptyList()
        }
    }

    /** Get context window for LLM — last N messages */
    fun getContextWindow(sessionId: String, maxMessages: Int = MAX_CONTEXT_MESSAGES): List<ChatMessage> {
        val messages = getMessages(sessionId)
        return if (messages.size <= maxMessages) {
            messages
        } else {
            messages.takeLast(maxMessages)
        }
    }

    /** Auto-generate a session name from the first message */
    fun autoNameSession(sessionId: String) {
        val messages = getMessages(sessionId)
        val firstUserMessage = messages.firstOrNull { it.role == "user" }
        if (firstUserMessage != null) {
            val name = firstUserMessage.content.take(40).let {
                if (firstUserMessage.content.length > 40) "$it…" else it
            }
            renameSession(sessionId, name)
        }
    }

    // ===== Private Storage =====

    private fun saveSessions(sessions: List<ChatSession>) {
        val array = JSONArray()
        for (s in sessions) {
            array.put(JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("createdAt", s.createdAt)
                put("updatedAt", s.updatedAt)
                put("modelId", s.modelId ?: "")
                put("totalTokens", s.totalTokens)
                put("messageCount", s.messageCount)
            })
        }
        File(storageDir, SESSIONS_FILE).writeText(array.toString())
    }

    private fun saveMessages(sessionId: String, messages: List<ChatMessage>) {
        val array = JSONArray()
        for (m in messages) {
            array.put(JSONObject().apply {
                put("id", m.id)
                put("role", m.role)
                put("content", m.content)
                put("timestamp", m.timestamp)
                put("tokensUsed", m.tokensUsed)
                put("contentType", m.contentType)
            })
        }
        File(storageDir, "$sessionId.json").writeText(array.toString())
    }

    /** Clear all conversation data */
    fun clearAll() {
        storageDir.listFiles()?.forEach { it.delete() }
    }
}
