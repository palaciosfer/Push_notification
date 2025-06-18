package com.example.configurationapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.configurationapp.model.UserConfiguration
import com.example.configurationapp.service.UsageTrackingService
import com.example.configurationapp.viewmodel.ConfigurationViewModel
import com.example.notificacionesapp.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Actividad principal que muestra la interfaz de configuración del usuario.
 * Permite al usuario modificar sus preferencias y visualizar estadísticas de uso.
 */
class ConfigurationActivity : AppCompatActivity() {

    private val viewModel: ConfigurationViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Elementos de la UI
    private lateinit var editTextUserName: EditText
    private lateinit var switchDarkTheme: Switch
    private lateinit var spinnerLanguage: Spinner
    private lateinit var seekBarVolume: SeekBar
    private lateinit var textViewVolumeValue: TextView
    private lateinit var textViewLastAccess: TextView
    private lateinit var textViewLocation: TextView
    private lateinit var textViewUsageTime: TextView
    private lateinit var buttonSave: Button
    private lateinit var buttonReset: Button
    private lateinit var buttonUpdateLocation: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var textViewStatus: TextView

    // Launcher para solicitar permisos de ubicación
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            updateCurrentLocation()
        } else {
            showMessage("Permisos de ubicación denegados", isError = true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuration)

        initializeViews()
        setupLocationClient()
        setupObservers()
        setupListeners()
        startUsageTracking()

        // Registrar acceso a la aplicación
        viewModel.recordAccessTime()
    }

    /**
     * Inicializa las vistas de la UI
     */
    private fun initializeViews() {
        editTextUserName = findViewById(R.id.editTextUserName)
        switchDarkTheme = findViewById(R.id.switchDarkTheme)
        spinnerLanguage = findViewById(R.id.spinnerLanguage)
        seekBarVolume = findViewById(R.id.seekBarVolume)
        textViewVolumeValue = findViewById(R.id.textViewVolumeValue)
        textViewLastAccess = findViewById(R.id.textViewLastAccess)
        textViewLocation = findViewById(R.id.textViewLocation)
        textViewUsageTime = findViewById(R.id.textViewUsageTime)
        buttonSave = findViewById(R.id.buttonSave)
        buttonReset = findViewById(R.id.buttonReset)
        buttonUpdateLocation = findViewById(R.id.buttonUpdateLocation)
        progressBar = findViewById(R.id.progressBar)
        textViewStatus = findViewById(R.id.textViewStatus)

        setupLanguageSpinner()
        setupVolumeSeekBar()
    }

    /**
     * Configura el cliente de ubicación
     */
    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    /**
     * Configura los observadores para el ViewModel
     */
    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.configuration.collect { config ->
                updateUIWithConfiguration(config)
            }
        }

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                progressBar.visibility = if (state.isLoading)
                    View.VISIBLE else View.GONE

                state.successMessage?.let { message ->
                    showMessage(message, isError = false)
                    viewModel.clearMessages()
                }

                state.errorMessage?.let { message ->
                    showMessage(message, isError = true)
                    viewModel.clearMessages()
                }
            }
        }
    }

    /**
     * Configura los listeners de los elementos de la UI
     */
    private fun setupListeners() {
        buttonSave.setOnClickListener {
            saveCurrentConfiguration()
        }

        buttonReset.setOnClickListener {
            showResetConfirmationDialog()
        }

        buttonUpdateLocation.setOnClickListener {
            requestLocationUpdate()
        }

        switchDarkTheme.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateDarkTheme(isChecked)
        }

        seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textViewVolumeValue.text = "$progress%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let { viewModel.updateNotificationVolume(it.progress) }
            }
        })
    }

    /**
     * Configura el spinner de idiomas
     */
    private fun setupLanguageSpinner() {
        val languages = arrayOf("Español", "English", "Français", "Deutsch")
        val languageCodes = arrayOf("es", "en", "fr", "de")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = adapter

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.updatePreferredLanguage(languageCodes[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    /**
     * Configura la barra de volumen
     */
    private fun setupVolumeSeekBar() {
        seekBarVolume.max = 100
    }

    /**
     * Actualiza la UI con la configuración actual
     */
    private fun updateUIWithConfiguration(config: UserConfiguration) {
        editTextUserName.setText(config.userName)
        switchDarkTheme.isChecked = config.isDarkThemeEnabled
        seekBarVolume.progress = config.notificationVolume
        textViewVolumeValue.text = "${config.notificationVolume}%"

        // Actualizar spinner de idioma
        val languageCodes = arrayOf("es", "en", "fr", "de")
        val languageIndex = languageCodes.indexOf(config.preferredLanguage)
        if (languageIndex >= 0) {
            spinnerLanguage.setSelection(languageIndex)
        }

        // Actualizar información de último acceso
        val lastAccessDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            .format(config.getLastAccessDate())
        textViewLastAccess.text = "Último acceso: $lastAccessDate"

        // Actualizar información de ubicación
        if (config.hasValidLocation()) {
            textViewLocation.text = "Ubicación: ${config.lastLocationLatitude}, ${config.lastLocationLongitude}"
        } else {
            textViewLocation.text = "Ubicación: No disponible"
        }

        // Actualizar tiempo de uso
        textViewUsageTime.text = "Tiempo total de uso: ${config.getFormattedUsageTime()}"
    }

    /**
     * Guarda la configuración actual
     */
    private fun saveCurrentConfiguration() {
        val userName = editTextUserName.text.toString().trim()
        if (userName.isNotEmpty()) {
            viewModel.updateUserName(userName)
        } else {
            showMessage("El nombre de usuario no puede estar vacío", isError = true)
        }
    }

    /**
     * Solicita actualización de ubicación
     */
    private fun requestLocationUpdate() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                updateCurrentLocation()
            }
            else -> {
                locationPermissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        }
    }

    /**
     * Obtiene y actualiza la ubicación actual
     */
    private fun updateCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    viewModel.updateLocation(it.latitude, it.longitude)
                } ?: run {
                    showMessage("No se pudo obtener la ubicación actual", isError = true)
                }
            }.addOnFailureListener { exception ->
                showMessage("Error al obtener ubicación: ${exception.message}", isError = true)
            }
        }
    }

    /**
     * Muestra diálogo de confirmación para reset
     */
    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Restablecer Configuración")
            .setMessage("¿Estás seguro de que quieres restablecer todas las configuraciones a sus valores por defecto?")
            .setPositiveButton("Sí") { _, _ ->
                viewModel.resetToDefaults()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Muestra un mensaje al usuario
     */
    private fun showMessage(message: String, isError: Boolean) {
        textViewStatus.text = message
        textViewStatus.setTextColor(
            ContextCompat.getColor(this, if (isError) android.R.color.holo_red_dark else android.R.color.holo_green_dark)
        )

        // Limpiar el mensaje después de 3 segundos
        textViewStatus.postDelayed({
            textViewStatus.text = ""
        }, 3000)
    }

    /**
     * Inicia el servicio de seguimiento de uso
     */
    private fun startUsageTracking() {
        val intent = Intent(this, UsageTrackingService::class.java).apply {
            action = UsageTrackingService.Companion.ACTION_START_TRACKING
        }
        startService(intent)
    }

    override fun onResume() {
        super.onResume()
        // Reanudar seguimiento de uso
        val intent = Intent(this, UsageTrackingService::class.java).apply {
            action= UsageTrackingService.Companion.ACTION_RESUME_TRACKING
        }
        startService(intent)

        // Actualizar tiempo de acceso
        viewModel.recordAccessTime()
    }

    override fun onPause() {
        super.onPause()
        // Pausar seguimiento de uso
        val intent = Intent(this, UsageTrackingService::class.java).apply {
            action = UsageTrackingService.Companion.ACTION_PAUSE_TRACKING
        }
        startService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Detener seguimiento de uso
        val intent = Intent(this, UsageTrackingService::class.java).apply {
            action = UsageTrackingService.Companion.ACTION_STOP_TRACKING
        }
        startService(intent)
    }
}