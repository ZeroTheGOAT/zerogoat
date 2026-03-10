package com.zerogoat.zero.agent

import com.zerogoat.zero.llm.LLMClient
import com.zerogoat.zero.llm.PromptBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Breaks complex user commands into high-level steps before execution.
 * Used for multi-step tasks like "order biryani on Swiggy".
 */
class TaskPlanner(private val llmClient: LLMClient) {

    /** Break a task into steps using the LLM */
    suspend fun planTask(task: String): List<String> = withContext(Dispatchers.Default) {
        try {
            val prompt = PromptBuilder.buildPlanningPrompt(task)
            val response = llmClient.chat(
                systemPrompt = "You are a task planning assistant. Break tasks into clear numbered steps.",
                userMessage = prompt
            )

            if (!response.isSuccess) return@withContext listOf(task)

            val json = JSONObject(response.text.trim()
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim())
            val steps = json.getJSONArray("steps")
            (0 until steps.length()).map { steps.getString(it) }
        } catch (e: Exception) {
            // If planning fails, treat the whole task as one step
            listOf(task)
        }
    }
}
