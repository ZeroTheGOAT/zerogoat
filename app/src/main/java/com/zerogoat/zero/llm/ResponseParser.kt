package com.zerogoat.zero.llm

import android.util.Log
import com.zerogoat.zero.agent.ActionType
import com.zerogoat.zero.agent.AgentAction
import org.json.JSONObject

/**
 * Parses structured JSON responses from the LLM into AgentAction objects.
 * Handles malformed JSON gracefully with fallback extraction.
 */
object ResponseParser {

    private const val TAG = "ResponseParser"

    /** Parse LLM response text into an AgentAction */
    fun parse(responseText: String): AgentAction? {
        try {
            // Clean up response — handle markdown code blocks
            val cleaned = responseText
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val json = JSONObject(cleaned)
            val thought = json.optString("thought", "")
            val actionJson = json.optJSONObject("action") ?: json

            val action = parseAction(actionJson) ?: return null
            return AgentAction(thought = thought, action = action)

        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse response: ${e.message}")
            // Try fallback extraction if JSON parsing fails
            return tryFallbackParse(responseText)
        }
    }

    private fun parseAction(json: JSONObject): ActionType? {
        val actionName = json.optString("action", "")

        return when (actionName) {
            "click" -> {
                val target = json.optInt("target", -1)
                if (target >= 0) ActionType.Click(target) else null
            }
            "long_click" -> {
                val target = json.optInt("target", -1)
                if (target >= 0) ActionType.LongClick(target) else null
            }
            "type" -> {
                val target = json.optInt("target", -1)
                val text = json.optString("text", "")
                if (target >= 0 && text.isNotEmpty()) ActionType.TypeText(target, text) else null
            }
            "scroll" -> {
                val direction = json.optString("direction", "down")
                val target = if (json.has("target")) json.optInt("target") else null
                ActionType.Scroll(direction, target)
            }
            "swipe" -> {
                ActionType.Swipe(
                    startX = json.optInt("startX", 0),
                    startY = json.optInt("startY", 0),
                    endX = json.optInt("endX", 0),
                    endY = json.optInt("endY", 0),
                    durationMs = json.optLong("durationMs", 300)
                )
            }
            "back" -> ActionType.Back
            "home" -> ActionType.Home
            "recents" -> ActionType.Recents
            "notifications" -> ActionType.Notifications
            "launch" -> {
                val pkg = json.optString("package", "")
                if (pkg.isNotEmpty()) ActionType.LaunchApp(pkg) else null
            }
            "wait" -> {
                ActionType.Wait(json.optLong("durationMs", 1000))
            }
            "confirm" -> {
                val message = json.optString("message", "Proceed?")
                ActionType.Confirm(message)
            }
            "done" -> {
                val summary = json.optString("summary", "Task completed")
                ActionType.Done(summary)
            }
            "fail" -> {
                val reason = json.optString("reason", "Unknown error")
                ActionType.Fail(reason)
            }
            else -> {
                Log.w(TAG, "Unknown action: $actionName")
                null
            }
        }
    }

    /** Fallback: try to extract action from messy LLM output */
    private fun tryFallbackParse(text: String): AgentAction? {
        // Look for JSON-like patterns in the text
        val jsonPattern = Regex("\\{[^}]*\"action\"[^}]*\\}")
        val match = jsonPattern.find(text)
        if (match != null) {
            try {
                val json = JSONObject(match.value)
                val action = parseAction(json)
                if (action != null) {
                    return AgentAction(thought = "fallback parse", action = action)
                }
            } catch (_: Exception) {}
        }
        return null
    }
}
