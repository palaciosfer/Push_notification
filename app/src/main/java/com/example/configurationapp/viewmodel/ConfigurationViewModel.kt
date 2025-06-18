package com.example.configurationapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.configurationapp.model.UserConfiguration
import com.example.configurationapp.repository.ConfigurationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel que maneja la lógica de negocio para las configuraciones del usuario.
 * Actúa como intermediario entre la UI y el repositorio de datos.
 */
class ConfigurationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ConfigurationRepository(application.applicationContext)

    // Estados para la UI
    private val _uiState = MutableStateFlow(ConfigurationUiState())
    val uiState: StateFlow<ConfigurationUiState> = _uiState.asStateFlow()

    // Configuración actual
    val configuration: StateFlow<UserConfiguration> = repository.configuration

    init {
        initializeConfiguration()
    }

    /**
     * Inicializa la configuración cargando datos existentes
     */
    private fun initializeConfiguration() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                repository.initialize()
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error al cargar configuraciones: ${e.message}"
                )
            }
        }
    }

    /**
     * Actualiza el nombre del usuario
     */
    fun updateUserName(userName: String) {
        viewModelScope.launch {
            try {
                repository.updateUserName(userName.trim())
                showSuccessMessage("Nombre actualizado correctamente")
            } catch (e: Exception) {
                showErrorMessage("Error al actualizar nombre: ${e.message}")
            }
        }
    }

    /**
     * Actualiza la preferencia de tema oscuro
     */
    fun updateDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.updateDarkTheme(enabled)
                showSuccessMessage("Tema ${if (enabled) "oscuro" else "claro"} activado")
            } catch (e: Exception) {
                showErrorMessage("Error al cambiar tema: ${e.message}")
            }
        }
    }

    /**
     * Actualiza el idioma preferido
     */
    fun updatePreferredLanguage(languageCode: String) {
        viewModelScope.launch {
            try {
                repository.updatePreferredLanguage(languageCode)
                showSuccessMessage("Idioma actualizado correctamente")
            } catch (e: Exception) {
                showErrorMessage("Error al actualizar idioma: ${e.message}")
            }
        }
    }

    /**
     * Actualiza el volumen de notificaciones
     */
    fun updateNotificationVolume(volume: Int) {
        viewModelScope.launch {
            try {
                repository.updateNotificationVolume(volume)
                showSuccessMessage("Volumen actualizado: $volume%")
            } catch (e: Exception) {
                showErrorMessage("Error al actualizar volumen: ${e.message}")
            }
        }
    }

    /**
     * Actualiza la ubicación del usuario
     */
    fun updateLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                repository.updateLocation(latitude, longitude)
                showSuccessMessage("Ubicación actualizada")
            } catch (e: Exception) {
                showErrorMessage("Error al actualizar ubicación: ${e.message}")
            }
        }
    }

    /**
     * Registra el tiempo de acceso actual
     */
    fun recordAccessTime() {
        viewModelScope.launch {
            try {
                repository.updateLastAccessTime()
            } catch (e: Exception) {
                // Log silencioso para no interrumpir la experiencia del usuario
                android.util.Log.e("ConfigurationViewModel", "Error updating access time", e)
            }
        }
    }

    /**
     * Añade tiempo de uso de una sesión
     */
    fun addUsageTime(sessionDurationSeconds: Long) {
        viewModelScope.launch {
            try {
                repository.addUsageTime(sessionDurationSeconds)
            } catch (e: Exception) {
                android.util.Log.e("ConfigurationViewModel", "Error adding usage time", e)
            }
        }
    }

    /**
     * Restablece todas las configuraciones a valores por defecto
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                repository.resetToDefaults()
                _uiState.value = _uiState.value.copy(isLoading = false)
                showSuccessMessage("Configuraciones restablecidas")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                showErrorMessage("Error al restablecer: ${e.message}")
            }
        }
    }

    /**
     * Exporta la configuración actual
     */
    fun exportConfiguration(): Map<String, Any?> {
        return repository.exportConfiguration()
    }

    /**
     * Importa configuración desde un mapa
     */
    fun importConfiguration(configMap: Map<String, Any?>) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                repository.importConfiguration(configMap)
                _uiState.value = _uiState.value.copy(isLoading = false)
                showSuccessMessage("Configuración importada correctamente")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                showErrorMessage("Error al importar: ${e.message}")
            }
        }
    }

    /**
     * Limpia los mensajes de estado
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            errorMessage = null
        )
    }

    private fun showSuccessMessage(message: String) {
        _uiState.value = _uiState.value.copy(
            successMessage = message,
            errorMessage = null
        )
    }

    private fun showErrorMessage(message: String) {
        _uiState.value = _uiState.value.copy(
            errorMessage = message,
            successMessage = null
        )
    }
}

/**
 * Estado de la UI para la pantalla de configuraciones
 */
data class ConfigurationUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

