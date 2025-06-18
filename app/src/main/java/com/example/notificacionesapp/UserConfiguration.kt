package com.example.notificacionesapp

data class UserConfiguration(
    val userName: String = "",
    val darkModeEnabled: Boolean = false,
    val preferredLanguage: String = "Español",
    val notificationVolume: Int = 50,
    val lastAccessDate: String = "",
    val lastLocation: String = "",
    val totalUsageTime: Long = 0L
)