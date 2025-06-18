package com.example.notificacionesapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper

class AppUsageTracker(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("usage_tracker", Context.MODE_PRIVATE)
    private val encryptionManager = EncryptionManager()
    private var startTime: Long = 0
    private var isTracking = false
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    // Callback para actualizar la UI
    var onUsageTimeUpdate: ((Long) -> Unit)? = null

    fun startTracking() {
        if (!isTracking) {
            startTime = System.currentTimeMillis()
            isTracking = true
            startPeriodicUpdate()
        }
    }

    fun stopTracking() {
        if (isTracking) {
            stopPeriodicUpdate()
            val currentTime = System.currentTimeMillis()
            val sessionTime = currentTime - startTime
            val totalTime = getTotalUsageTime() + sessionTime
            saveTotalUsageTime(totalTime)
            isTracking = false
        }
    }

    fun pauseTracking() {
        if (isTracking) {
            val currentTime = System.currentTimeMillis()
            val sessionTime = currentTime - startTime
            val totalTime = getTotalUsageTime() + sessionTime
            saveTotalUsageTime(totalTime)
            stopPeriodicUpdate()
        }
    }

    fun resumeTracking() {
        if (isTracking) {
            startTime = System.currentTimeMillis()
            startPeriodicUpdate()
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

    fun getCurrentSessionTime(): Long {
        return if (isTracking) {
            System.currentTimeMillis() - startTime
        } else {
            0L
        }
    }

    fun getTotalUsageTimeIncludingCurrentSession(): Long {
        return getTotalUsageTime() + getCurrentSessionTime()
    }

    private fun saveTotalUsageTime(totalTime: Long) {
        val encryptedTime = encryptionManager.encrypt(totalTime.toString())
        sharedPreferences.edit()
            .putString("total_usage_time", encryptedTime)
            .apply()
    }

    private fun startPeriodicUpdate() {
        updateRunnable = object : Runnable {
            override fun run() {
                if (isTracking) {
                    val totalTimeWithSession = getTotalUsageTimeIncludingCurrentSession()
                    onUsageTimeUpdate?.invoke(totalTimeWithSession)
                    handler.postDelayed(this, 1000) // Actualizar cada segundo
                }
            }
        }
        handler.post(updateRunnable!!)
    }

    private fun stopPeriodicUpdate() {
        updateRunnable?.let { handler.removeCallbacks(it) }
        updateRunnable = null
    }
}