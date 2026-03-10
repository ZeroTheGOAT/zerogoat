package com.zerogoat.zero.llm

/**
 * Builds token-optimized prompts for the LLM.
 * Uses compact system prompt (~400 tokens) and structured JSON output.
 */
object PromptBuilder {

    /** Compact system prompt for the agent — kept small for KV-cache efficiency */
    val SYSTEM_PROMPT = """
You are Zero, an Android AI agent inside the ZeroGoat app. You control the device via actions.
You see a text representation of the current screen. Each interactive element has an index [N].

RULES:
- Respond ONLY with valid JSON: {"thought":"brief reasoning","action":{...}}
- Be precise: click exact elements, type exact text
- If task is done, use "done" action
- If stuck, try scrolling or going back
- NEVER make purchases without "confirm" action first
- Keep thoughts under 20 words

ACTIONS:
{"action":"click","target":N} — tap element [N]
{"action":"long_click","target":N} — long-press [N]
{"action":"type","target":N,"text":"..."} — type into [N]
{"action":"scroll","direction":"down|up|left|right"} — scroll
{"action":"scroll","direction":"down","target":N} — scroll element [N]
{"action":"swipe","startX":N,"startY":N,"endX":N,"endY":N} — swipe gesture
{"action":"back"} — press back
{"action":"home"} — go home
{"action":"recents"} — recent apps
{"action":"notifications"} — open notifications
{"action":"launch","package":"com.example.app"} — open app
{"action":"wait","durationMs":1000} — wait
{"action":"confirm","message":"..."} — ask user before proceeding
{"action":"done","summary":"..."} — task complete
{"action":"fail","reason":"..."} — cannot complete
""".trimIndent()

    /** Build the user message containing the task + current UI state */
    fun buildUserMessage(
        task: String,
        uiTree: String,
        stepNumber: Int,
        maxSteps: Int,
        previousActions: List<String> = emptyList()
    ): String {
        val sb = StringBuilder()
        sb.appendLine("TASK: $task")
        sb.appendLine("STEP: $stepNumber/$maxSteps")

        if (previousActions.isNotEmpty()) {
            sb.appendLine("PREVIOUS:")
            previousActions.takeLast(3).forEach { sb.appendLine("  - $it") }
        }

        sb.appendLine()
        sb.appendLine("CURRENT SCREEN:")
        sb.appendLine(uiTree)

        return sb.toString()
    }

    /** Build a simpler prompt for task planning (breaking down complex tasks) */
    fun buildPlanningPrompt(task: String): String {
        return """
Break this task into numbered steps (max 8). Be specific about which app to use.
Task: $task
Respond as JSON: {"steps":["step 1","step 2",...]}
""".trimIndent()
    }

    /** Build prompt for identifying which app to launch */
    fun buildAppIdentificationPrompt(task: String, installedApps: List<String>): String {
        return """
Which app should be opened for this task? Pick from installed apps list.
Task: $task
Apps: ${installedApps.joinToString(", ")}
Respond as JSON: {"package":"com.example.app","reason":"brief reason"}
""".trimIndent()
    }
}
