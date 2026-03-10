package com.zerogoat.zero.llm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Tracks token usage and estimated cost across tasks.
 */
class TokenTracker {

    private val _totalInputTokens = MutableStateFlow(0)
    val totalInputTokens: StateFlow<Int> = _totalInputTokens

    private val _totalOutputTokens = MutableStateFlow(0)
    val totalOutputTokens: StateFlow<Int> = _totalOutputTokens

    private val _taskTokens = MutableStateFlow(0)
    val taskTokens: StateFlow<Int> = _taskTokens

    private val _estimatedCost = MutableStateFlow(0.0)
    val estimatedCost: StateFlow<Double> = _estimatedCost

    /** Record token usage from an LLM response */
    fun record(response: LLMResponse, provider: String) {
        _totalInputTokens.value += response.inputTokens
        _totalOutputTokens.value += response.outputTokens
        _taskTokens.value += response.totalTokens

        // Estimate cost based on provider
        val cost = when (provider) {
            "Gemini" -> {
                // Gemini Flash: ~$0.075/1M input, ~$0.30/1M output
                (response.inputTokens * 0.000000075) + (response.outputTokens * 0.0000003)
            }
            "OpenAI" -> {
                // GPT-4o-mini: ~$0.15/1M input, ~$0.60/1M output
                (response.inputTokens * 0.00000015) + (response.outputTokens * 0.0000006)
            }
            "Anthropic" -> {
                // Claude 3.5 Haiku: ~$0.25/1M input, ~$1.25/1M output
                (response.inputTokens * 0.00000025) + (response.outputTokens * 0.00000125)
            }
            else -> 0.0
        }
        _estimatedCost.value += cost
    }

    /** Reset task-level counters (called when starting a new task) */
    fun resetTask() {
        _taskTokens.value = 0
    }

    /** Format cost as a human-readable string */
    fun formatCost(): String {
        val cost = _estimatedCost.value
        return when {
            cost < 0.01 -> "< $0.01"
            cost < 1.0 -> "$${String.format("%.3f", cost)}"
            else -> "$${String.format("%.2f", cost)}"
        }
    }

    /** Format task tokens */
    fun formatTaskTokens(): String {
        val tokens = _taskTokens.value
        return when {
            tokens < 1000 -> "$tokens tokens"
            tokens < 100_000 -> "${tokens / 1000}K tokens"
            else -> "${tokens / 1000}K tokens"
        }
    }
}
