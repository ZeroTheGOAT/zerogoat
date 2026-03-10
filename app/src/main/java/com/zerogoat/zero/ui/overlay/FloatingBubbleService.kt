package com.zerogoat.zero.ui.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat

/**
 * Floating bubble service — shows Zero's status as an always-on-top widget.
 * Like Facebook chat heads, but for your AI agent.
 */
class FloatingBubbleService : Service() {

    companion object {
        private const val CHANNEL_ID = "zero_bubble"
        private const val NOTIFICATION_ID = 42
    }

    private var windowManager: WindowManager? = null
    private var bubbleView: FrameLayout? = null
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showBubble()
        return START_STICKY
    }

    private fun showBubble() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        bubbleView = FrameLayout(this).apply {
            // The actual Compose/View content would be inflated here
            // For now, a simple colored circle
            setBackgroundColor(0xFF00E5FF.toInt())
        }

        val params = WindowManager.LayoutParams(
            120, 120,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        // Make it draggable
        bubbleView?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(bubbleView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (Math.abs(dx) < 10 && Math.abs(dy) < 10) {
                        // It was a tap, not a drag — open the app and trigger voice
                        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        launchIntent?.putExtra("start_voice", true)
                        if (launchIntent != null) startActivity(launchIntent)
                    }
                    true
                }
                else -> false
            }
        }

        windowManager?.addView(bubbleView, params)
    }

    override fun onDestroy() {
        bubbleView?.let { windowManager?.removeView(it) }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Zero Agent",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Zero AI Agent is running in the background"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Zero is active")
            .setContentText("Your AI agent is ready")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}
