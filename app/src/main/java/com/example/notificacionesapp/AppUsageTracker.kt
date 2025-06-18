package com.example.notificacionesapp

import android.content.Context
import android.content.SharedPreferences

class AppUsageTracker(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("usage_tracker", Context.MODE_PRIVATE)
    private val encryptionManager = EncryptionManager()
    private var startTime: Long = 0
    private var isTracking = false

    fun startTracking() {
        if (!isTracking) {
            startTime = System.currentTimeMillis()
            isTracking = true
        }
    }

    fun stopTracking() {
        if (isTracking) {
            val currentTime = System.currentTimeMillis()
            val sessionTime = currentTime - startTime
            val totalTime = getTotalUsageTime() + sessionTime
            saveTotalUsageTime(totalTime)
            isTracking = false
        }
    }

    fun getTotalUsageTime(): Long {
        val encryptedTime = sharedPreferences.getString("total_usage_time", null)
        return if (encryptedTime != null) {
            try {
                encryptionManager.decrypt(encryptedTime).toLong()
            } catch (e: Exception) {
                0L
            }
        } else {
            0L
        }
    }

    private fun saveTotalUsageTime(totalTime: Long) {
        val encryptedTime = encryptionManager.encrypt(totalTime.toString())
        sharedPreferences.edit()
            .putString("total_usage_time", encryptedTime)
            .apply()
    }
}