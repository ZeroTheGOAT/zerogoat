package com.zerogoat.zero.screen

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Compresses and encodes screenshots for vision API calls.
 * Minimizes image size to reduce token consumption.
 */
object ScreenshotAnalyzer {

    /**
     * Convert a bitmap to a base64-encoded JPEG string for sending to vision APIs.
     * Uses aggressive compression (quality 50) to minimize tokens.
     */
    fun toBase64Jpeg(bitmap: Bitmap, quality: Int = 50): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val bytes = stream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Get the MIME type and base64 data for an API request.
     */
    fun toImagePayload(bitmap: Bitmap, quality: Int = 50): Pair<String, String> {
        return "image/jpeg" to toBase64Jpeg(bitmap, quality)
    }

    /**
     * Estimate approximate token usage for a vision API call with this image.
     * Based on OpenAI's vision pricing model.
     */
    fun estimateVisionTokens(bitmap: Bitmap): Int {
        val tiles = ((bitmap.width / 512) + 1) * ((bitmap.height / 512) + 1)
        return 85 + (tiles * 170) // Base + per-tile cost
    }
}
