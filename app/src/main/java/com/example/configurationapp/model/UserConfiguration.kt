package com.example.configurationapp.model

import java.util.Date

/**
 * Modelo de datos que representa todas las configuraciones del usuario
 * que se almacenarán de forma encriptada en la aplicación.
 */
data class UserConfiguration(
    val userName: String = "",
    val isDarkThemeEnabled: Boolean = false,
    val preferredLanguage: String = "es", // Código de idioma ISO 639-1
    val notificationVolume: Int = 50, // Volumen de 0 a 100
    val lastAccessDateTime: Long = System.currentTimeMillis(), // Timestamp en milisegundos
    val lastLocationLatitude: Double? = null, // Latitud de la última ubicación
    val lastLocationLongitude: Double? = null, // Longitud de la última ubicación
    val totalUsageTimeSeconds: Long = 0L // Tiempo total de uso en segundos
) {
    /**
     * Convierte la configuración a un mapa de pares clave-valor para almacenamiento
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userName" to userName,
            "isDarkThemeEnabled" to isDarkThemeEnabled,
            "preferredLanguage" to preferredLanguage,
            "notificationVolume" to notificationVolume,
            "lastAccessDateTime" to lastAccessDateTime,
            "lastLocationLatitude" to lastLocationLatitude,
            "lastLocationLongitude" to lastLocationLongitude,
            "totalUsageTimeSeconds" to totalUsageTimeSeconds
        )
    }

    companion object {
        /**
         * Crea una instancia de UserConfiguration desde un mapa de datos
         */
        fun fromMap(map: Map<String, Any?>): UserConfiguration {
            return UserConfiguration(
                userName = map["userName"] as? String ?: "",
                isDarkThemeEnabled = map["isDarkThemeEnabled"] as? Boolean ?: false,
                preferredLanguage = map["preferredLanguage"] as? String ?: "es",
                notificationVolume = map["notificationVolume"] as? Int ?: 50,
                lastAccessDateTime = map["lastAccessDateTime"] as? Long ?: System.currentTimeMillis(),
                lastLocationLatitude = map["lastLocationLatitude"] as? Double,
                lastLocationLongitude = map["lastLocationLongitude"] as? Double,
                totalUsageTimeSeconds = map["totalUsageTimeSeconds"] as? Long ?: 0L
            )
        }

        /**
         * Configuración por defecto para nuevos usuarios
         */
        fun getDefaultConfiguration(): UserConfiguration {
            return UserConfiguration()
        }
    }

    /**
     * Obtiene la fecha de último acceso como objeto Date
     */
    fun getLastAccessDate(): Date {
        return Date(lastAccessDateTime)
    }

    /**
     * Verifica si tiene ubicación válida
     */
    fun hasValidLocation(): Boolean {
        return lastLocationLatitude != null && lastLocationLongitude != null
    }

    /**
     * Obtiene el tiempo total de uso formateado en horas, minutos y segundos
     */
    fun getFormattedUsageTime(): String {
        val hours = totalUsageTimeSeconds / 3600
        val minutes = (totalUsageTimeSeconds % 3600) / 60
        val seconds = totalUsageTimeSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}