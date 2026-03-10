package com.zerogoat.zero.agent

import android.accessibilityservice.AccessibilityService
import android.util.Log
import com.zerogoat.zero.accessibility.NodeActionPerformer
import com.zerogoat.zero.accessibility.ZeroAccessibilityService

/**
 * Executes individual actions via the AccessibilityService.
 * Used by the AgentLoop after parsing LLM responses.
 */
class ActionExecutor {

    companion object {
        private const val TAG = "ActionExecutor"
    }

    /** Execute an action and return success/failure */
    fun execute(action: ActionType): Boolean {
        val service = ZeroAccessibilityService.instance.value
        if (service == null) {
            Log.e(TAG, "Accessibility Service not available")
            return false
        }

        val performer = NodeActionPerformer(service)

        return try {
            when (action) {
                is ActionType.Click -> {
                    Log.d(TAG, "Click: target=${action.target}")
                    performer.clickNode(action.target)
                }
                is ActionType.LongClick -> {
                    Log.d(TAG, "LongClick: target=${action.target}")
                    performer.longClickNode(action.target)
                }
                is ActionType.TypeText -> {
                    Log.d(TAG, "Type: target=${action.target}, text='${action.text}'")
                    performer.typeText(action.target, action.text)
                }
                is ActionType.Scroll -> {
                    Log.d(TAG, "Scroll: direction=${action.direction}")
                    performer.scroll(action.direction, action.target)
                }
                is ActionType.Swipe -> {
                    Log.d(TAG, "Swipe: (${action.startX},${action.startY}) → (${action.endX},${action.endY})")
                    service.performSwipe(
                        action.startX.toFloat(), action.startY.toFloat(),
                        action.endX.toFloat(), action.endY.toFloat(),
                        action.durationMs
                    )
                }
                is ActionType.Back -> {
                    Log.d(TAG, "Back")
                    service.doGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                }
                is ActionType.Home -> {
                    Log.d(TAG, "Home")
                    service.doGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                }
                is ActionType.Recents -> {
                    Log.d(TAG, "Recents")
                    service.doGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
                }
                is ActionType.Notifications -> {
                    Log.d(TAG, "Notifications")
                    service.doGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                }
                is ActionType.LaunchApp -> {
                    Log.d(TAG, "Launch: ${action.packageName}")
                    service.launchApp(action.packageName)
                }
                is ActionType.Wait -> {
                    Log.d(TAG, "Wait: ${action.durationMs}ms")
                    Thread.sleep(action.durationMs)
                    true
                }
                is ActionType.Done -> {
                    Log.d(TAG, "Done: ${action.summary}")
                    true
                }
                is ActionType.Fail -> {
                    Log.d(TAG, "Fail: ${action.reason}")
                    false
                }
                is ActionType.Confirm -> {
                    Log.d(TAG, "Confirm: ${action.message}")
                    true // Confirmation is handled in AgentLoop
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Action execution failed: ${action::class.simpleName}", e)
            false
        }
    }
}
