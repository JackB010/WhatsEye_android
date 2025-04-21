package com.example.whatseye.access

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent


class MyAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Respond to accessibility events here
    }

    override fun onInterrupt() {
        // Handle interruption of the service
    }

    // Optional: Other methods to customize your service
}