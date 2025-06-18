package com.example.notificacionesapp

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

class ConfigurationManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("encrypted_config", Context.MODE_PRIVATE)
    private val encryptionManager = EncryptionManager()
    private val gson = Gson()

    fun saveConfiguration(config: UserConfiguration) {
        val json = gson.toJson(config)
        val encryptedJson = encryptionManager.encrypt(json)

        sharedPreferences.edit()
            .putString("user_config", encryptedJson)
            .apply()
    }

    fun loadConfiguration(): UserConfiguration {
        val encryptedJson = sharedPreferences.getString("user_config", null)

        return if (encryptedJson != null) {
            try {
                val json = encryptionManager.decrypt(encryptedJson)
                gson.fromJson(json, UserConfiguration::class.java)
            } catch (e: Exception) {
                // Si hay error al desencriptar, retorna configuración por defecto
                UserConfiguration()
            }
        } else {
            UserConfiguration()
        }
    }

    fun clearConfiguration() {
        sharedPreferences.edit().clear().apply()
    }
}