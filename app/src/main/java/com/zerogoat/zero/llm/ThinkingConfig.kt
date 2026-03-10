package com.zerogoat.zero.llm

/**
 * Thinking levels — controls reasoning depth.
 * Inspired by OpenClaw's thinkingLevel / setChatThinkingLevel.
 */
enum class ThinkingLevel(
    val label: String,
    val description: String,
    val temperatureMultiplier: Float,
    val maxTokensMultiplier: Int,
    val systemPromptSuffix: String
) {
    QUICK(
        label = "⚡ Quick",
        description = "Fast, concise answers. Minimal reasoning.",
        temperatureMultiplier = 0.1f,
        maxTokensMultiplier = 1,
        systemPromptSuffix = "Be extremely concise. One action per step. No explanation needed."
    ),
    BALANCED(
        label = "🧠 Balanced",
        description = "Good balance of speed and thoroughness.",
        temperatureMultiplier = 0.2f,
        maxTokensMultiplier = 1,
        systemPromptSuffix = "Think briefly, then act. Keep thoughts under 20 words."
    ),
    DEEP(
        label = "💭 Deep",
        description = "Thorough reasoning. Chain-of-thought for complex tasks.",
        temperatureMultiplier = 0.4f,
        maxTokensMultiplier = 2,
        systemPromptSuffix = "Think step-by-step. Consider edge cases. Explain your reasoning in the thought field."
    );

    companion object {
        fun fromString(value: String): ThinkingLevel {
            return when (value.lowercase()) {
                "quick", "low", "fast" -> QUICK
                "balanced", "medium", "default" -> BALANCED
                "deep", "high", "thorough" -> DEEP
                else -> BALANCED
            }
        }
    }
}
