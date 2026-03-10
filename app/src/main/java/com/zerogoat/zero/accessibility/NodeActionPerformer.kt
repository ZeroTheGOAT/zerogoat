package com.zerogoat.zero.accessibility

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Translates indexed node actions from the LLM into actual
 * AccessibilityService calls. Maps node indices from UITreeExtractor
 * to real AccessibilityNodeInfo objects.
 */
class NodeActionPerformer(private val service: ZeroAccessibilityService) {

    companion object {
        private const val TAG = "NodeAction"
    }

    /** Click node at the given index in the current UI tree */
    fun clickNode(nodeIndex: Int): Boolean {
        val node = resolveNode(nodeIndex) ?: return false
        Log.d(TAG, "Clicking node [$nodeIndex]: ${node.text ?: node.contentDescription}")
        return service.performClick(node)
    }

    /** Long-click node at the given index */
    fun longClickNode(nodeIndex: Int): Boolean {
        val node = resolveNode(nodeIndex) ?: return false
        Log.d(TAG, "Long-clicking node [$nodeIndex]")
        return service.performLongClick(node)
    }

    /** Type text into the node at the given index */
    fun typeText(nodeIndex: Int, text: String): Boolean {
        val node = resolveNode(nodeIndex) ?: return false
        // Focus the field first
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        Log.d(TAG, "Typing into node [$nodeIndex]: '$text'")
        return service.performSetText(node, text)
    }

    /** Scroll a scrollable node or the general screen */
    fun scroll(direction: String, nodeIndex: Int? = null): Boolean {
        val forward = direction.lowercase() in listOf("down", "right", "forward")

        if (nodeIndex != null) {
            val node = resolveNode(nodeIndex) ?: return false
            if (node.isScrollable) {
                Log.d(TAG, "Scrolling node [$nodeIndex] ${if (forward) "forward" else "backward"}")
                return service.performScroll(node, forward)
            }
        }

        // Find the first scrollable node on screen
        val root = service.getUIRoot() ?: return false
        val scrollable = findFirstScrollable(root)
        if (scrollable != null) {
            Log.d(TAG, "Scrolling first scrollable ${if (forward) "forward" else "backward"}")
            return service.performScroll(scrollable, forward)
        }

        // Fallback: perform a swipe gesture
        val screenHeight = 2400f
        val screenWidth = 1080f
        val centerX = screenWidth / 2
        return if (forward) {
            service.performSwipe(centerX, screenHeight * 0.7f, centerX, screenHeight * 0.3f)
        } else {
            service.performSwipe(centerX, screenHeight * 0.3f, centerX, screenHeight * 0.7f)
        }
    }

    /** Resolve a node index to an actual AccessibilityNodeInfo */
    private fun resolveNode(index: Int): AccessibilityNodeInfo? {
        val root = service.getUIRoot()
        val nodeMap = UITreeExtractor.buildNodeIndex(root)
        val node = nodeMap[index]
        if (node == null) {
            Log.w(TAG, "Node [$index] not found in current UI tree (${nodeMap.size} nodes)")
        }
        return node
    }

    /** Find the first scrollable node in the tree */
    private fun findFirstScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findFirstScrollable(child)
            if (result != null) return result
        }
        return null
    }
}
