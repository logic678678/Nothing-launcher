package com.example.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class LauncherAccessibilityService : AccessibilityService() {

    companion object {
        private var instance: LauncherAccessibilityService? = null

        fun isRunning(): Boolean {
            return instance != null
        }

        fun performLockScreen(): Boolean {
            return instance?.let { service ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                } else {
                    false
                }
            } ?: false
        }

        fun expandNotifications(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) ?: false
        }

        fun expandQuickSettings(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS) ?: false
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("AccessibilityService", "Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }
}
