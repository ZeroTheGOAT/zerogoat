package com.zerogoat.zero.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Extracts a compact text representation of the UI hierarchy.
 * This is the KEY token optimization — sending a text tree (~200 tokens)
 * instead of a screenshot (~1500+ tokens via vision) saves 85%+ on API costs.
 */
object UITreeExtractor {

    data class UINode(
        val index: String,
        val className: String,
        val text: String?,
        val contentDescription: String?,
        val hint: String?,
        val isClickable: Boolean,
        val isEditable: Boolean,
        val isScrollable: Boolean,
        val isCheckable: Boolean,
        val isChecked: Boolean,
        val isFocusable: Boolean,
        val isSelected: Boolean,
        val viewId: String?,
        val bounds: Rect,
        val children: List<UINode>,
        val nodeRef: AccessibilityNodeInfo
    )

    /**
     * Extract the UI tree as a compact text string for LLM consumption.
     * Filters out invisible, decorative, and off-screen nodes.
     * Indexes each interactive node for action reference.
     */
    fun extractCompactTree(root: AccessibilityNodeInfo?, screenWidth: Int = 1080, screenHeight: Int = 2400): String {
        root ?: return "[Empty Screen]"

        val sb = StringBuilder()
        val packageName = root.packageName?.toString() ?: "unknown"
        sb.appendLine("[Screen: $packageName]")

        var nodeIndex = 0
        val indexCounter = object { var value = 0 }

        fun traverse(node: AccessibilityNodeInfo, depth: Int, parentIndex: String) {
            // Skip invisible nodes
            if (!node.isVisibleToUser) return

            // Skip off-screen nodes
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            if (bounds.right <= 0 || bounds.bottom <= 0 ||
                bounds.left >= screenWidth || bounds.top >= screenHeight) return

            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            val hint = node.hintText?.toString()?.trim()
            val className = simplifyClassName(node.className?.toString())
            val isInteractive = node.isClickable || node.isLongClickable ||
                node.isEditable || node.isScrollable || node.isCheckable

            // Determine if this node has meaningful content
            val hasContent = !text.isNullOrEmpty() || !desc.isNullOrEmpty() || !hint.isNullOrEmpty()
            val hasInteraction = isInteractive

            if (hasContent || hasInteraction) {
                val idx = indexCounter.value.toString()
                indexCounter.value++

                val indent = "  ".repeat(depth)
                val prefix = "$indent[$idx]"

                sb.append(prefix)
                sb.append(" $className")

                // Content
                if (!text.isNullOrEmpty()) {
                    val truncated = if (text.length > 80) text.take(77) + "..." else text
                    sb.append(" \"$truncated\"")
                } else if (!desc.isNullOrEmpty()) {
                    val truncated = if (desc.length > 60) desc.take(57) + "..." else desc
                    sb.append(" ($truncated)")
                }

                // Properties
                val props = mutableListOf<String>()
                if (node.isClickable) props.add("clickable")
                if (node.isLongClickable) props.add("long-clickable")
                if (node.isEditable) props.add("editable")
                if (node.isScrollable) props.add("scrollable")
                if (node.isCheckable) {
                    props.add(if (node.isChecked) "checked" else "unchecked")
                }
                if (node.isSelected) props.add("selected")
                if (!hint.isNullOrEmpty()) props.add("hint:\"$hint\"")

                if (props.isNotEmpty()) {
                    sb.append(" {${props.joinToString(", ")}}")
                }

                sb.appendLine()
            }

            // Traverse children
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                traverse(child, depth + 1, parentIndex)
            }
        }

        traverse(root, 0, "")

        val result = sb.toString().trim()
        return if (result.lines().size <= 1) "[Empty Screen - no interactive elements]" else result
    }

    /**
     * Build a flat list of all interactive nodes indexed for action reference.
     * Returns a map of index → AccessibilityNodeInfo for the ActionExecutor.
     */
    fun buildNodeIndex(root: AccessibilityNodeInfo?): Map<Int, AccessibilityNodeInfo> {
        root ?: return emptyMap()
        val nodeMap = mutableMapOf<Int, AccessibilityNodeInfo>()
        var index = 0

        fun traverse(node: AccessibilityNodeInfo) {
            if (!node.isVisibleToUser) return

            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            val hint = node.hintText?.toString()?.trim()
            val hasContent = !text.isNullOrEmpty() || !desc.isNullOrEmpty() || !hint.isNullOrEmpty()
            val isInteractive = node.isClickable || node.isLongClickable ||
                node.isEditable || node.isScrollable || node.isCheckable

            if (hasContent || isInteractive) {
                nodeMap[index] = node
                index++
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                traverse(child)
            }
        }

        traverse(root)
        return nodeMap
    }

    /** Simplify Android class names for compact representation */
    private fun simplifyClassName(className: String?): String {
        if (className == null) return "View"
        return when {
            className.contains("EditText") -> "EditText"
            className.contains("Button") -> "Button"
            className.contains("TextView") -> "Text"
            className.contains("ImageView") -> "Image"
            className.contains("ImageButton") -> "ImageButton"
            className.contains("CheckBox") -> "CheckBox"
            className.contains("RadioButton") -> "Radio"
            className.contains("Switch") -> "Switch"
            className.contains("ToggleButton") -> "Toggle"
            className.contains("SeekBar") -> "Slider"
            className.contains("ProgressBar") -> "Progress"
            className.contains("Spinner") -> "Dropdown"
            className.contains("RecyclerView") -> "List"
            className.contains("ListView") -> "List"
            className.contains("ScrollView") -> "ScrollView"
            className.contains("ViewPager") -> "Pager"
            className.contains("TabLayout") -> "Tabs"
            className.contains("WebView") -> "WebView"
            className.contains("ViewGroup") -> "Group"
            className.contains("FrameLayout") -> "Frame"
            className.contains("LinearLayout") -> "Row"
            className.contains("RelativeLayout") -> "Container"
            className.contains("ConstraintLayout") -> "Container"
            else -> className.substringAfterLast(".")
        }
    }
}
