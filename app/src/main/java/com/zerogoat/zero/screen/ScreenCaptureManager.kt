package com.zerogoat.zero.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.DisplayMetrics
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Screen capture via MediaProjection API (no root needed).
 * Used as a FALLBACK when UI tree is insufficient (WebViews, games, canvases).
 */
class ScreenCaptureManager(private val context: Context) {

    companion object {
        private const val TAG = "ScreenCapture"
        const val REQUEST_CODE = 1001
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    /** Initialize with the result from MediaProjection permission prompt */
    fun initialize(resultCode: Int, data: Intent) {
        val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = manager.getMediaProjection(resultCode, data)
        Log.i(TAG, "MediaProjection initialized")
    }

    /** Check if screen capture is available */
    val isAvailable: Boolean get() = mediaProjection != null

    /**
     * Capture current screen as a compressed JPEG bitmap.
     * Resolution is capped at 720p to minimize vision API token cost.
     */
    suspend fun captureScreen(maxWidth: Int = 720): Bitmap? = suspendCancellableCoroutine { cont ->
        val projection = mediaProjection
        if (projection == null) {
            Log.w(TAG, "MediaProjection not initialized")
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        try {
            val metrics = context.resources.displayMetrics
            val scale = maxWidth.toFloat() / metrics.widthPixels
            val width = maxWidth
            val height = (metrics.heightPixels * scale).toInt()
            val density = metrics.densityDpi

            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            imageReader = reader

            val display = projection.createVirtualDisplay(
                "ZeroCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface, null, null
            )
            virtualDisplay = display

            reader.setOnImageAvailableListener({ imgReader ->
                val image = imgReader.acquireLatestImage()
                if (image != null) {
                    try {
                        val planes = image.planes
                        val buffer = planes[0].buffer
                        val pixelStride = planes[0].pixelStride
                        val rowStride = planes[0].rowStride
                        val rowPadding = rowStride - pixelStride * width

                        val bitmap = Bitmap.createBitmap(
                            width + rowPadding / pixelStride, height,
                            Bitmap.Config.ARGB_8888
                        )
                        bitmap.copyPixelsFromBuffer(buffer)

                        // Crop to actual width
                        val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                        if (cropped != bitmap) bitmap.recycle()

                        cont.resume(cropped)
                    } finally {
                        image.close()
                        display.release()
                        reader.close()
                    }
                }
            }, null)
        } catch (e: Exception) {
            Log.e(TAG, "Screen capture failed", e)
            cont.resume(null)
        }
    }

    /** Release all resources */
    fun release() {
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        mediaProjection = null
        Log.i(TAG, "ScreenCaptureManager released")
    }
}
