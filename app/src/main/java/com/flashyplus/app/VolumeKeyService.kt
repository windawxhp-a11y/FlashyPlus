package com.flashyplus.app

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class VolumeKeyService : AccessibilityService() {
    private var volumeUpDownAt = 0L
    private var handledLongPress = false
    private val holdThresholdMs = 500L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_UP) return false

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    volumeUpDownAt = event.eventTime
                    handledLongPress = false
                }
                if (!handledLongPress && event.eventTime - volumeUpDownAt >= holdThresholdMs) {
                    handledLongPress = toggleFlash()
                }
                // Consume volume-up while pressed so it does not also change media volume.
                return true
            }
            KeyEvent.ACTION_UP -> {
                val held = event.eventTime - volumeUpDownAt
                if (!handledLongPress && held >= holdThresholdMs) {
                    handledLongPress = toggleFlash()
                }
                volumeUpDownAt = 0L
                return true
            }
        }
        return true
    }

    private fun toggleFlash(): Boolean {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "افتح Flashy+ وامنحه إذن الكاميرا أولاً.", Toast.LENGTH_LONG).show()
            return false
        }
        return try {
            val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = manager.cameraIdList.firstOrNull {
                manager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return false
            val prefs = getSharedPreferences("flashy", Context.MODE_PRIVATE)
            val currentlyOn = prefs.getBoolean("flash_on", false)
            manager.setTorchMode(id, !currentlyOn)
            prefs.edit().putBoolean("flash_on", !currentlyOn).apply()
            true
        } catch (_: Exception) {
            false
        }
    }
}
