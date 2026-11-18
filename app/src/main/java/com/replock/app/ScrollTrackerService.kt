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
import com.replock.app.data.VideoQuotaRepository

class ScrollTrackerService : AccessibilityService() {

    private lateinit var quotaRepo: VideoQuotaRepository
    private var lastEventTime = 0L
    private val debounceMs = 800L
    private var overlayView: FrameLayout? = null
    private val handler = Handler(Looper.getMainLooper())

    private val trackedPackages = setOf(
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.aweme",
        "com.instagram.android"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        quotaRepo = VideoQuotaRepository(this)

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_SCROLLED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName !in trackedPackages) return

        val now = System.currentTimeMillis()
        if (now - lastEventTime < debounceMs) return
        lastEventTime = now

        val isNewVideoCandidate =
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                    event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED

        if (isNewVideoCandidate) {
            val remaining = quotaRepo.consumeVideo()
            if (remaining <= 0) {
                blockCurrentApp()
            }
        }
    }

    private fun blockCurrentApp() {
        showBlockOverlay()
        handler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_HOME)
            Toast.makeText(
                this,
                "Daily limit reached. Open RepLock and do push-ups to unlock.",
                Toast.LENGTH_LONG
            ).show()
        }, 1200)
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
                text = "Daily limit reached\n\nOpen RepLock and perform push-ups to continue."
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(64, 64, 64, 64)
            }
            addView(tv)
        }

        try {
            windowManager.addView(overlayView, params)
            handler.postDelayed({ removeOverlay() }, 3000)
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
        removeOverlay()
        super.onDestroy()
    }
}
