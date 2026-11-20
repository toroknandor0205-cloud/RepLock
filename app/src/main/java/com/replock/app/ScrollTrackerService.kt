package com.replock.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import com.replock.app.data.VideoQuotaRepository

class ScrollTrackerService : AccessibilityService() {

    private lateinit var quotaRepo: VideoQuotaRepository
    private var lastVideoTime = 0L
    private val minIntervalMs = 1400L          // minimum time between two counted videos
    private var lastPackage: String? = null
    private var overlayView: FrameLayout? = null
    private val handler = Handler(Looper.getMainLooper())

    private val trackedPackages = setOf(
        "com.zhiliaoapp.musically",   // TikTok
        "com.ss.android.ugc.aweme",   // TikTok (alternative)
        "com.instagram.android"      // Instagram
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
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 80
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName !in trackedPackages) return

        val now = System.currentTimeMillis()

        // Only count when enough time has passed since last video
        if (now - lastVideoTime < minIntervalMs) return

        val shouldCount = when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> true
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Extra check for Instagram Reels / TikTok feed changes
                isLikelyNewVideo(event)
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                // Strong signal of swiping to next video
                true
            }
            else -> false
        }

        if (shouldCount) {
            lastVideoTime = now
            lastPackage = packageName

            val remaining = quotaRepo.consumeVideo()
            if (remaining <= 0) {
                blockCurrentApp()
            }
        }
    }

    private fun isLikelyNewVideo(event: AccessibilityEvent): Boolean {
        // Heuristic: large content change usually means new video loaded
        val node = event.source ?: return false
        return try {
            val childCount = node.childCount
            childCount > 3   // simple but effective filter
        } catch (e: Exception) {
            false
        } finally {
            node.recycle()
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
        }, 1100)
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
        removeOverlay()
        super.onDestroy()
    }
}
