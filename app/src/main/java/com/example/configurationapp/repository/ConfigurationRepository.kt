package com.example.configurationapp.repository

import android.content.Context
import com.example.configurationapp.data.ConfigurationDataSource
import com.example.configurationapp.model.UserConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repositorio que actúa como una única fuente de verdad para las configuraciones del usuario.
 * Gestiona la comunicación entre la capa de datos y la lógica de negocio.
 */
class ConfigurationRepository(context: Context) {

    private val dataSource = ConfigurationDataSource(context)

    // StateFlow para observar cambios en la configuración
    private val _configuration =
        MutableStateFlow(UserConfiguration.Companion.getDefaultConfiguration())
    val configuration: StateFlow<UserConfiguration> = _configuration.asStateFlow()

    /**
     * Inicializa el repositorio cargando la configuración existente
     */
    suspend fun initialize() {
        val storedConfig = if (dataSource.hasStoredConfiguration()) {
            dataSource.loadConfiguration()
        } else {
            UserConfiguration.Companion.getDefaultConfiguration()
        }
        _configuration.value = storedConfig
    }

    /**
     * Guarda una nueva configuración completa
     */
    suspend fun saveConfiguration(configuration: UserConfiguration) {
        dataSource.saveConfiguration(configuration)
        _configuration.value = configuration
    }

    /**
     * Actualiza el nombre del usuario
     */
    suspend fun updateUserName(userName: String) {
        val updatedConfig = _configuration.value.copy(userName = userName)
        saveConfiguration(updatedConfig)
    }

    /**
     * Actualiza la preferencia de tema oscuro
     */
    suspend fun updateDarkTheme(enabled: Boolean) {
        val updatedConfig = _configuration.value.copy(isDarkThemeEnabled = enabled)
        saveConfiguration(updatedConfig)
    }

    /**
     * Actualiza el idioma preferido
     */
    suspend fun updatePreferredLanguage(languageCode: String) {
        val updatedConfig = _configuration.value.copy(preferredLanguage = languageCode)
        saveConfiguration(updatedConfig)
    }

    /**
     * Actualiza el volumen de notificaciones
     */
    suspend fun updateNotificationVolume(volume: Int) {
        val clampedVolume = volume.coerceIn(0, 100)
        val updatedConfig = _configuration.value.copy(notificationVolume = clampedVolume)
        saveConfiguration(updatedConfig)
    }

    /**
     * Actualiza la fecha y hora de último acceso
     */
    suspend fun updateLastAccessTime() {
        dataSource.updateLastAccessTime()
        val updatedConfig = _configuration.value.copy(lastAccessDateTime = System.currentTimeMillis())
        _configuration.value = updatedConfig
    }

    /**
     * Actualiza la ubicación del usuario
     */
    suspend fun updateLocation(latitude: Double, longitude: Double) {
        dataSource.updateLocation(latitude, longitude)
        val updatedConfig = _configuration.value.copy(
            lastLocationLatitude = latitude,
            lastLocationLongitude = longitude
        )
        _configuration.value = updatedConfig
    }

    /**
     * Añade tiempo de uso de una sesión al total acumulado
     */
    suspend fun addUsageTime(sessionDurationSeconds: Long) {
        if (sessionDurationSeconds > 0) {
            dataSource.addUsageTime(sessionDurationSeconds)
            val updatedConfig = _configuration.value.copy(
                totalUsageTimeSeconds = _configuration.value.totalUsageTimeSeconds + sessionDurationSeconds
            )
            _configuration.value = updatedConfig
        }
    }

    /**
     * Obtiene la configuración actual
     */
    fun getCurrentConfiguration(): UserConfiguration {
        return _configuration.value
    }

    /**
     * Verifica si hay configuraciones guardadas
     */
    suspend fun hasStoredConfiguration(): Boolean {
        return dataSource.hasStoredConfiguration()
    }

    /**
     * Limpia todas las configuraciones y restablece a valores por defecto
     */
    suspend fun resetToDefaults() {
        dataSource.clearAllConfiguration()
        _configuration.value = UserConfiguration.Companion.getDefaultConfiguration()
    }

    /**
     * Exporta la configuración actual como mapa (útil para backup o debug)
     */
    fun exportConfiguration(): Map<String, Any?> {
        return _configuration.value.toMap()
    }

    /**
     * Importa configuración desde un mapa (útil para restore o migración)
     */
    suspend fun importConfiguration(configMap: Map<String, Any?>) {
        val importedConfig = UserConfiguration.Companion.fromMap(configMap)
        saveConfiguration(importedConfig)
    }
}