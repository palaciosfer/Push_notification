package com.example.configurationapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.configurationapp.model.UserConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fuente de datos que maneja el almacenamiento encriptado de las configuraciones del usuario.
 * Utiliza EncryptedSharedPreferences para garantizar la seguridad de los datos.
 */
class ConfigurationDataSource(private val context: Context) {

    companion object {
        private const val PREFS_FILE_NAME = "user_configuration_prefs"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_DARK_THEME = "dark_theme_enabled"
        private const val KEY_PREFERRED_LANGUAGE = "preferred_language"
        private const val KEY_NOTIFICATION_VOLUME = "notification_volume"
        private const val KEY_LAST_ACCESS_TIME = "last_access_time"
        private const val KEY_LAST_LOCATION_LAT = "last_location_latitude"
        private const val KEY_LAST_LOCATION_LNG = "last_location_longitude"
        private const val KEY_TOTAL_USAGE_TIME = "total_usage_time_seconds"
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedSharedPreferences: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Guarda la configuración del usuario de forma encriptada
     */
    suspend fun saveConfiguration(configuration: UserConfiguration) {
        withContext(Dispatchers.IO) {
            with(encryptedSharedPreferences.edit()) {
                putString(KEY_USER_NAME, configuration.userName)
                putBoolean(KEY_DARK_THEME, configuration.isDarkThemeEnabled)
                putString(KEY_PREFERRED_LANGUAGE, configuration.preferredLanguage)
                putInt(KEY_NOTIFICATION_VOLUME, configuration.notificationVolume)
                putLong(KEY_LAST_ACCESS_TIME, configuration.lastAccessDateTime)

                // Guardar ubicación solo si está disponible
                configuration.lastLocationLatitude?.let { lat ->
                    putFloat(KEY_LAST_LOCATION_LAT, lat.toFloat())
                }
                configuration.lastLocationLongitude?.let { lng ->
                    putFloat(KEY_LAST_LOCATION_LNG, lng.toFloat())
                }

                putLong(KEY_TOTAL_USAGE_TIME, configuration.totalUsageTimeSeconds)
                apply()
            }
        }
    }

    /**
     * Carga la configuración del usuario desde el almacenamiento encriptado
     */
    suspend fun loadConfiguration(): UserConfiguration {
        return withContext(Dispatchers.IO) {
            val prefs = encryptedSharedPreferences

            val userName = prefs.getString(KEY_USER_NAME, "") ?: ""
            val isDarkThemeEnabled = prefs.getBoolean(KEY_DARK_THEME, false)
            val preferredLanguage = prefs.getString(KEY_PREFERRED_LANGUAGE, "es") ?: "es"
            val notificationVolume = prefs.getInt(KEY_NOTIFICATION_VOLUME, 50)
            val lastAccessDateTime = prefs.getLong(KEY_LAST_ACCESS_TIME, System.currentTimeMillis())

            // Cargar ubicación si existe
            val lastLocationLatitude = if (prefs.contains(KEY_LAST_LOCATION_LAT)) {
                prefs.getFloat(KEY_LAST_LOCATION_LAT, 0f).toDouble()
            } else null

            val lastLocationLongitude = if (prefs.contains(KEY_LAST_LOCATION_LNG)) {
                prefs.getFloat(KEY_LAST_LOCATION_LNG, 0f).toDouble()
            } else null

            val totalUsageTimeSeconds = prefs.getLong(KEY_TOTAL_USAGE_TIME, 0L)

            UserConfiguration(
                userName = userName,
                isDarkThemeEnabled = isDarkThemeEnabled,
                preferredLanguage = preferredLanguage,
                notificationVolume = notificationVolume,
                lastAccessDateTime = lastAccessDateTime,
                lastLocationLatitude = lastLocationLatitude,
                lastLocationLongitude = lastLocationLongitude,
                totalUsageTimeSeconds = totalUsageTimeSeconds
            )
        }
    }

    /**
     * Actualiza solo la fecha y hora de último acceso
     */
    suspend fun updateLastAccessTime() {
        withContext(Dispatchers.IO) {
            encryptedSharedPreferences.edit()
                .putLong(KEY_LAST_ACCESS_TIME, System.currentTimeMillis())
                .apply()
        }
    }

    /**
     * Actualiza solo la ubicación
     */
    suspend fun updateLocation(latitude: Double, longitude: Double) {
        withContext(Dispatchers.IO) {
            encryptedSharedPreferences.edit()
                .putFloat(KEY_LAST_LOCATION_LAT, latitude.toFloat())
                .putFloat(KEY_LAST_LOCATION_LNG, longitude.toFloat())
                .apply()
        }
    }

    /**
     * Actualiza solo el tiempo total de uso
     */
    suspend fun updateTotalUsageTime(totalSeconds: Long) {
        withContext(Dispatchers.IO) {
            encryptedSharedPreferences.edit()
                .putLong(KEY_TOTAL_USAGE_TIME, totalSeconds)
                .apply()
        }
    }

    /**
     * Incrementa el tiempo total de uso con la duración de una sesión
     */
    suspend fun addUsageTime(sessionDurationSeconds: Long) {
        withContext(Dispatchers.IO) {
            val currentTotal = encryptedSharedPreferences.getLong(KEY_TOTAL_USAGE_TIME, 0L)
            val newTotal = currentTotal + sessionDurationSeconds
            encryptedSharedPreferences.edit()
                .putLong(KEY_TOTAL_USAGE_TIME, newTotal)
                .apply()
        }
    }

    /**
     * Verifica si existen configuraciones guardadas
     */
    suspend fun hasStoredConfiguration(): Boolean {
        return withContext(Dispatchers.IO) {
            encryptedSharedPreferences.contains(KEY_USER_NAME)
        }
    }

    /**
     * Limpia todas las configuraciones (útil para reset o logout)
     */
    suspend fun clearAllConfiguration() {
        withContext(Dispatchers.IO) {
            encryptedSharedPreferences.edit().clear().apply()
        }
    }
}