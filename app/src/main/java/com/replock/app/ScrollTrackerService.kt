package com.replock.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import com.replock.app.data.TimeQuotaRepository

class ScrollTrackerService : AccessibilityService() {

    private lateinit var quotaRepo: TimeQuotaRepository
    private var isInTrackedApp = false
    private var lastTickTime = 0L
    private var overlayView: FrameLayout? = null
    private val handler = Handler(Looper.getMainLooper())

    private val trackedPackages = setOf(
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.aweme",
        "com.instagram.android"
    )

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (isInTrackedApp) {
                val now = System.currentTimeMillis()
                if (lastTickTime > 0) {
                    val elapsedSeconds = ((now - lastTickTime) / 1000).toInt()
                    if (elapsedSeconds > 0) {
                        val remaining = quotaRepo.consumeSeconds(elapsedSeconds)
                        if (remaining <= 0) {
                            blockCurrentApp()
                        }
                    }
                }
                lastTickTime = now
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        quotaRepo = TimeQuotaRepository(this)

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        val currentlyInTracked = packageName in trackedPackages

        if (currentlyInTracked && !isInTrackedApp) {
            // Entered tracked app
            isInTrackedApp = true
            lastTickTime = System.currentTimeMillis()
            handler.removeCallbacks(tickRunnable)
            handler.post(tickRunnable)
        } else if (!currentlyInTracked && isInTrackedApp) {
            // Left tracked app
            isInTrackedApp = false
            handler.removeCallbacks(tickRunnable)
            lastTickTime = 0L
        }
    }

    private fun blockCurrentApp() {
        isInTrackedApp = false
        handler.removeCallbacks(tickRunnable)
        showBlockOverlay()
        handler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_HOME)
            Toast.makeText(
                this,
                "Time limit reached. Open RepLock and do push-ups to earn more time.",
                Toast.LENGTH_LONG
            ).show()
        }, 1000)
    }

    private fun showBlockOverlay() {
        if (overlayView != null) return

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        overlayView = FrameLayout(this).apply {
            setBackgroundColor(0xE6000000.toInt())
            val tv = TextView(this@ScrollTrackerService).apply {
                text = "Time limit reached\n\nOpen RepLock and perform push-ups\nto earn more minutes."
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(64, 64, 64, 64)
            }
            addView(tv)
        }

        try {
            windowManager.addView(overlayView, params)
            handler.postDelayed({ removeOverlay() }, 2800)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                val wm = getSystemService(WINDOW_SERVICE) as WindowManager
                wm.removeView(it)
            } catch (_: Exception) { }
            overlayView = null
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        handler.removeCallbacks(tickRunnable)
        removeOverlay()
        super.onDestroy()
    }
}
