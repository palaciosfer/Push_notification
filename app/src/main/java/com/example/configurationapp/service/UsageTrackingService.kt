package com.example.configurationapp.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.configurationapp.repository.ConfigurationRepository
import kotlinx.coroutines.launch

/**
 * Servicio que rastrea el tiempo de uso de la aplicación.
 * Se ejecuta en segundo plano para monitorear cuándo la aplicación está activa.
 */
class UsageTrackingService : LifecycleService() {

    private lateinit var repository: ConfigurationRepository
    private var sessionStartTime: Long = 0L
    private var isTracking = false

    companion object {
        const val ACTION_START_TRACKING = "com.example.configurationapp.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.example.configurationapp.STOP_TRACKING"
        const val ACTION_PAUSE_TRACKING = "com.example.configurationapp.PAUSE_TRACKING"
        const val ACTION_RESUME_TRACKING = "com.example.configurationapp.RESUME_TRACKING"
    }

    override fun onCreate() {
        super.onCreate()
        repository = ConfigurationRepository(applicationContext)
        lifecycleScope.launch {
            repository.initialize()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START_TRACKING -> startTracking()
            ACTION_STOP_TRACKING -> stopTracking()
            ACTION_PAUSE_TRACKING -> pauseTracking()
            ACTION_RESUME_TRACKING -> resumeTracking()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    /**
     * Inicia el seguimiento del tiempo de uso
     */
    private fun startTracking() {
        if (!isTracking) {
            sessionStartTime = System.currentTimeMillis()
            isTracking = true

            // Actualizar tiempo de último acceso
            lifecycleScope.launch {
                repository.updateLastAccessTime()
            }
        }
    }

    /**
     * Detiene el seguimiento y guarda el tiempo de sesión
     */
    private fun stopTracking() {
        if (isTracking) {
            val sessionDuration = calculateSessionDuration()
            if (sessionDuration > 0) {
                lifecycleScope.launch {
                    repository.addUsageTime(sessionDuration)
                }
            }
            isTracking = false
            sessionStartTime = 0L
        }
    }

    /**
     * Pausa el seguimiento temporalmente (cuando la app va a segundo plano)
     */
    private fun pauseTracking() {
        if (isTracking) {
            val sessionDuration = calculateSessionDuration()
            if (sessionDuration > 0) {
                lifecycleScope.launch {
                    repository.addUsageTime(sessionDuration)
                }
            }
            // Reiniciar el tiempo de sesión para la próxima reanudación
            sessionStartTime = 0L
        }
    }

    /**
     * Reanuda el seguimiento (cuando la app vuelve a primer plano)
     */
    private fun resumeTracking() {
        if (isTracking) {
            sessionStartTime = System.currentTimeMillis()

            // Actualizar tiempo de último acceso
            lifecycleScope.launch {
                repository.updateLastAccessTime()
            }
        }
    }

    /**
     * Calcula la duración de la sesión actual en segundos
     */
    private fun calculateSessionDuration(): Long {
        return if (sessionStartTime > 0) {
            val currentTime = System.currentTimeMillis()
            val durationMillis = currentTime - sessionStartTime
            durationMillis / 1000 // Convertir a segundos
        } else {
            0L
        }
    }

    override fun onDestroy() {
        stopTracking()
        super.onDestroy()
    }
}

