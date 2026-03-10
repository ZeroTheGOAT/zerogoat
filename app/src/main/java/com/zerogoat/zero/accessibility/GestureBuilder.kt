package com.zerogoat.zero.accessibility

import android.accessibilityservice.GestureDescription
import android.graphics.Path

/**
 * Builds complex gesture descriptions for the Accessibility Service.
 * Supports multi-point gestures like pinch, drag, etc.
 */
object GestureBuilder {

    /** Create a simple tap gesture */
    fun tap(x: Float, y: Float, durationMs: Long = 100): GestureDescription {
        val path = Path().apply { moveTo(x, y) }
        return GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
    }

    /** Create a swipe/drag gesture */
    fun swipe(
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        durationMs: Long = 300
    ): GestureDescription {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        return GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
    }

    /** Create a long press gesture */
    fun longPress(x: Float, y: Float, durationMs: Long = 800): GestureDescription {
        val path = Path().apply { moveTo(x, y) }
        return GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
    }

    /** Create a pinch gesture (zoom in/out) */
    fun pinch(
        centerX: Float, centerY: Float,
        startDistance: Float, endDistance: Float,
        durationMs: Long = 500
    ): GestureDescription {
        val path1 = Path().apply {
            moveTo(centerX - startDistance, centerY)
            lineTo(centerX - endDistance, centerY)
        }
        val path2 = Path().apply {
            moveTo(centerX + startDistance, centerY)
            lineTo(centerX + endDistance, centerY)
        }
        return GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path1, 0, durationMs))
            .addStroke(GestureDescription.StrokeDescription(path2, 0, durationMs))
            .build()
    }

    /** Create a drag and drop gesture */
    fun dragAndDrop(
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        holdMs: Long = 500,
        moveMs: Long = 500
    ): GestureDescription {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(startX, startY) // Hold in place
            lineTo(endX, endY)     // Drag to target
        }
        return GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, holdMs + moveMs))
            .build()
    }

    /** Scroll up by swiping from bottom to top */
    fun scrollUp(screenWidth: Float = 1080f, screenHeight: Float = 2400f): GestureDescription {
        return swipe(
            screenWidth / 2, screenHeight * 0.7f,
            screenWidth / 2, screenHeight * 0.3f,
            400
        )
    }

    /** Scroll down by swiping from top to bottom */
    fun scrollDown(screenWidth: Float = 1080f, screenHeight: Float = 2400f): GestureDescription {
        return swipe(
            screenWidth / 2, screenHeight * 0.3f,
            screenWidth / 2, screenHeight * 0.7f,
            400
        )
    }
}
