package com.zerogoat.zero.agent

import com.squareup.moshi.JsonClass

/**
 * All possible actions Zero can perform on the device.
 */
sealed class ActionType {

    /** Tap a UI node by its index in the tree */
    @JsonClass(generateAdapter = false)
    data class Click(val target: Int) : ActionType()

    /** Long-press a UI node */
    @JsonClass(generateAdapter = false)
    data class LongClick(val target: Int) : ActionType()

    /** Type text into a UI node (usually EditText) */
    @JsonClass(generateAdapter = false)
    data class TypeText(val target: Int, val text: String) : ActionType()

    /** Scroll the screen or a specific scrollable node */
    @JsonClass(generateAdapter = false)
    data class Scroll(val direction: String, val target: Int? = null) : ActionType()

    /** Swipe gesture with coordinates */
    @JsonClass(generateAdapter = false)
    data class Swipe(
        val startX: Int, val startY: Int,
        val endX: Int, val endY: Int,
        val durationMs: Long = 300
    ) : ActionType()

    /** Press back button */
    data object Back : ActionType()

    /** Press home button */
    data object Home : ActionType()

    /** Open recent apps */
    data object Recents : ActionType()

    /** Open notification shade */
    data object Notifications : ActionType()

    /** Launch an app by package name */
    @JsonClass(generateAdapter = false)
    data class LaunchApp(val packageName: String) : ActionType()

    /** Wait for the screen to change */
    @JsonClass(generateAdapter = false)
    data class Wait(val durationMs: Long = 1000) : ActionType()

    /** Task completed successfully */
    @JsonClass(generateAdapter = false)
    data class Done(val summary: String) : ActionType()

    /** Task failed */
    @JsonClass(generateAdapter = false)
    data class Fail(val reason: String) : ActionType()

    /** Request user confirmation (for payments, etc.) */
    @JsonClass(generateAdapter = false)
    data class Confirm(val message: String) : ActionType()
}

/**
 * The structured response Zero's LLM produces.
 */
data class AgentAction(
    val thought: String,
    val action: ActionType
)
