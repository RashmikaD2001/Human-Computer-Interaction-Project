package com.example.hci3v1

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.CountDownTimer
import android.os.PowerManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.example.hci3v1.network.NetworkConfig  // Import network communication

class MyAccessibilityService : AccessibilityService() {
    private var overlayView: View? = null
    private lateinit var windowManager: WindowManager
    private lateinit var wakeLock: PowerManager.WakeLock
    private var currentTimer: CountDownTimer? = null
    private var isBlocking = false
    private lateinit var scoreViewModel: ScoreViewModel

    private val toggleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.hci3v1.TOGGLE_BLOCKING") {
                isBlocking = !isBlocking
                if (!isBlocking) {
                    removeOverlay()
                    sendStopToESP()
                } else {
                    // Force-check the current app and show overlay if needed
                    performGlobalAction(GLOBAL_ACTION_HOME) // Minimize app for instant detection
                    sendStartToESP()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(toggleReceiver, IntentFilter("com.example.hci3v1.TOGGLE_BLOCKING"), RECEIVER_NOT_EXPORTED)

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MyAccessibilityService::WakeLock"
        )
        wakeLock.acquire(10 * 60 * 1000L)

        scoreViewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(application).create(ScoreViewModel::class.java)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(toggleReceiver)
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
            removeOverlay()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isBlocking) return

        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            Log.d("MyAccessibilityService", "Detected package: $packageName")

            if (packageName in listOf(
                    "com.instagram.android",
                    "com.facebook.katana",
                    "com.whatsapp"
                )) {
                Log.d("MyAccessibilityService", "Blocking social media app: $packageName")
                showOverlay()
                sendOverlayToESP() // Notify ESP when overlay appears
            }
        }
    }

    private fun showOverlay() {
        if (overlayView != null) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val layoutParams = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.CENTER
        }

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_block, null)
        overlayView?.setOnTouchListener { _, _ -> true }

        try {
            windowManager.addView(overlayView, layoutParams)
            startCountdown()
            scoreViewModel.decreaseScore(20) // Deduct 20 points when overlay appears
        } catch (e: Exception) {
            e.printStackTrace()
            overlayView = null
        }
    }

    private fun removeOverlay() {
        currentTimer?.cancel()
        currentTimer = null

        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
    }

    private fun startCountdown() {
        currentTimer?.cancel()
        val timerText: TextView = overlayView?.findViewById(R.id.tv_timer) ?: return

        currentTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerText.text = "${millisUntilFinished / 1000} seconds remaining"
            }

            override fun onFinish() {
                removeOverlay()
            }
        }.start()
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.DEFAULT
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    // ESP8266 Communication Methods
    private fun sendStartToESP() {
        NetworkConfig.sendRequest("start") { success ->
            if (!success) Log.e("ESP8266", "Failed to notify ESP of session start")
        }
    }

    private fun sendOverlayToESP() {
        NetworkConfig.sendRequest("overlay") { success ->
            if (!success) Log.e("ESP8266", "Failed to notify ESP of overlay")
        }
    }

    private fun sendStopToESP() {
        val score = scoreViewModel.score.value ?: 0
        NetworkConfig.sendRequest("stop", "score=$score") { success ->
            if (!success) Log.e("ESP8266", "Failed to notify ESP of session stop")
        }
    }
}
