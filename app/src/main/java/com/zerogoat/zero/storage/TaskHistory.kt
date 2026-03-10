package com.zerogoat.zero.storage

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local history of tasks executed by Zero.
 * Stores task command, steps, result, and token usage.
 */
class TaskHistory(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("zero_task_history", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_HISTORY = "history"
        private const val MAX_ENTRIES = 100
    }

    data class TaskEntry(
        val command: String,
        val result: String,
        val steps: Int,
        val tokensUsed: Int,
        val timestamp: Long,
        val success: Boolean
    )

    /** Add a task entry to history */
    fun add(entry: TaskEntry) {
        val history = getAll().toMutableList()
        history.add(0, entry) // Newest first
        if (history.size > MAX_ENTRIES) {
            history.removeAt(history.lastIndex)
        }
        save(history)
    }

    /** Get all history entries */
    fun getAll(): List<TaskEntry> {
        val json = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                TaskEntry(
                    command = obj.getString("command"),
                    result = obj.getString("result"),
                    steps = obj.getInt("steps"),
                    tokensUsed = obj.getInt("tokens"),
                    timestamp = obj.getLong("timestamp"),
                    success = obj.getBoolean("success")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Clear all history */
    fun clear() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun save(entries: List<TaskEntry>) {
        val array = JSONArray()
        for (entry in entries) {
            array.put(JSONObject().apply {
                put("command", entry.command)
                put("result", entry.result)
                put("steps", entry.steps)
                put("tokens", entry.tokensUsed)
                put("timestamp", entry.timestamp)
                put("success", entry.success)
            })
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }
}
