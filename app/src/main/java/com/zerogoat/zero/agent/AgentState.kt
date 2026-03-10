package com.zerogoat.zero.agent

/**
 * State machine for the agent lifecycle.
 */
sealed class AgentState {

    /** Agent is idle, waiting for a command */
    data object Idle : AgentState()

    /** Agent is planning the task steps */
    data class Planning(val taskDescription: String) : AgentState()

    /** Agent is executing an action */
    data class Acting(
        val step: Int,
        val maxSteps: Int,
        val currentThought: String,
        val currentAction: ActionType
    ) : AgentState()

    /** Agent is waiting for user confirmation (e.g., payment) */
    data class WaitingConfirmation(
        val message: String,
        val pendingAction: ActionType
    ) : AgentState()

    /** Agent paused by user */
    data class Paused(val step: Int, val reason: String) : AgentState()

    /** Task completed successfully */
    data class Completed(val summary: String, val totalSteps: Int, val tokensUsed: Int) : AgentState()

    /** Task failed */
    data class Failed(val reason: String, val step: Int, val tokensUsed: Int) : AgentState()
}
