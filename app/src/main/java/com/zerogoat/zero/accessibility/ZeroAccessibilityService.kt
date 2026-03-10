package com.zerogoat.zero.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Zero's Accessibility Service — the heart of OS-level control.
 * Provides full access to UI hierarchy and device actions without root.
 */
class ZeroAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ZeroA11y"

        private val _instance = MutableStateFlow<ZeroAccessibilityService?>(null)
        val instance: StateFlow<ZeroAccessibilityService?> = _instance

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning

        private val _currentPackage = MutableStateFlow<String?>(null)
        val currentPackage: StateFlow<String?> = _currentPackage
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _instance.value = this
        _isRunning.value = true
        Log.i(TAG, "Zero Accessibility Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                event.packageName?.toString()?.let { pkg ->
                    if (pkg != "com.zerogoat.zero") {
                        _currentPackage.value = pkg
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Zero Accessibility Service interrupted")
    }

    override fun onDestroy() {
        _instance.value = null
        _isRunning.value = false
        super.onDestroy()
    }

    // ========== UI Tree Access ==========

    /** Get the root node of the current active window */
    fun getUIRoot(): AccessibilityNodeInfo? {
        return try {
            rootInActiveWindow
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get root node", e)
            null
        }
    }

    /** Get all window roots (for multi-window scenarios) */
    fun getAllWindowRoots(): List<AccessibilityNodeInfo> {
        return try {
            windows.mapNotNull { it.root }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get window roots", e)
            emptyList()
        }
    }

    // ========== Actions ==========

    /** Click on a specific node */
    fun performClick(node: AccessibilityNodeInfo): Boolean {
        return if (node.isClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            // Try clicking the parent if the node itself isn't clickable
            findClickableParent(node)?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
        }
    }

    /** Long-click on a node */
    fun performLongClick(node: AccessibilityNodeInfo): Boolean {
        return if (node.isLongClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
        } else {
            findLongClickableParent(node)?.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK) ?: false
        }
    }

    /** Set text in an editable field */
    fun performSetText(node: AccessibilityNodeInfo, text: String): Boolean {
        val args = android.os.Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    /** Scroll a scrollable node */
    fun performScroll(node: AccessibilityNodeInfo, forward: Boolean): Boolean {
        val action = if (forward) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }
        return node.performAction(action)
    }

    /** Perform a global action: BACK, HOME, RECENTS, NOTIFICATIONS */
    fun doGlobalAction(actionId: Int): Boolean {
        return super.performGlobalAction(actionId)
    }

    fun pressBack(): Boolean = doGlobalAction(GLOBAL_ACTION_BACK)
    fun pressHome(): Boolean = doGlobalAction(GLOBAL_ACTION_HOME)
    fun openRecents(): Boolean = doGlobalAction(GLOBAL_ACTION_RECENTS)
    fun openNotifications(): Boolean = doGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    fun openQuickSettings(): Boolean = doGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)

    /** Launch any app by package name */
    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = applicationContext.packageManager
                .getLaunchIntentForPackage(packageName)
                ?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            if (intent != null) {
                applicationContext.startActivity(intent)
                true
            } else {
                Log.w(TAG, "No launch intent for package: $packageName")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch app: $packageName", e)
            false
        }
    }

    /** Perform a tap gesture at specific coordinates */
    fun performTap(x: Float, y: Float, callback: GestureResultCallback? = null): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        return dispatchGesture(gesture, callback, null)
    }

    /** Perform a swipe gesture */
    fun performSwipe(
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        durationMs: Long = 300,
        callback: GestureResultCallback? = null
    ): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        return dispatchGesture(gesture, callback, null)
    }

    // ========== Node Search Helpers ==========

    /** Find a node by text content */
    fun findNodeByText(text: String): AccessibilityNodeInfo? {
        val root = getUIRoot() ?: return null
        val nodes = root.findAccessibilityNodeInfosByText(text)
        return nodes.firstOrNull()
    }

    /** Find a node by view ID */
    fun findNodeById(viewId: String): AccessibilityNodeInfo? {
        val root = getUIRoot() ?: return null
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
        return nodes.firstOrNull()
    }

    // ========== Private Helpers ==========

    private fun findClickableParent(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current = node.parent
        var depth = 0
        while (current != null && depth < 5) {
            if (current.isClickable) return current
            current = current.parent
            depth++
        }
        return null
    }

    private fun findLongClickableParent(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current = node.parent
        var depth = 0
        while (current != null && depth < 5) {
            if (current.isLongClickable) return current
            current = current.parent
            depth++
        }
        return null
    }
}
