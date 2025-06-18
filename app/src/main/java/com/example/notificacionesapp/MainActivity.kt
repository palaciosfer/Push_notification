package com.example.notificacionesapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.messaging.FirebaseMessaging
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var configManager: ConfigurationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var appUsageTracker: AppUsageTracker

    private lateinit var etUserName: EditText
    private lateinit var switchDarkMode: Switch
    private lateinit var spinnerLanguage: Spinner
    private lateinit var seekBarVolume: SeekBar
    private lateinit var tvLastAccess: TextView
    private lateinit var tvLastLocation: TextView
    private lateinit var tvTotalUsage: TextView
    private lateinit var btnSave: Button
    private lateinit var rootLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar managers primero
        configManager = ConfigurationManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        appUsageTracker = AppUsageTracker(this)

        // Aplicar el tema guardado antes de crear el layout
        applyInitialTheme()

        // Crear layout programáticamente
        createLayout()

        // Cargar configuraciones guardadas
        loadSavedConfigurations()

        // Configurar listeners
        setupListeners()

        // Firebase token
        getFirebaseToken()

        // Solicitar permisos de ubicación
        requestLocationPermission()

        // Actualizar última fecha de acceso
        updateLastAccess()

        // Iniciar seguimiento de uso
        appUsageTracker.startTracking()
    }

    private fun applyInitialTheme() {
        val config = configManager.loadConfiguration()
        if (config.darkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun createLayout() {
        val scrollView = ScrollView(this)

        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Título de la aplicación
        val tvTitle = TextView(this).apply {
            text = "Configuración de Notificaciones"
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
            setTextColor(getThemeColor())
        }
        rootLayout.addView(tvTitle)

        // Campo de nombre de usuario
        val tvUserNameLabel = TextView(this).apply {
            text = "Nombre de usuario:"
            textSize = 16f
            setPadding(0, 16, 0, 8)
            setTextColor(getThemeColor())
        }
        rootLayout.addView(tvUserNameLabel)

        etUserName = EditText(this).apply {
            hint = "Ingresa tu nombre"
            setPadding(16, 16, 16, 16)
            setBackgroundColor(getEditTextBackgroundColor())
            setTextColor(getThemeColor())
            setHintTextColor(getHintColor())
        }
        rootLayout.addView(etUserName)

        // Switch modo oscuro
        val switchLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 24, 0, 16)
        }

        val tvDarkModeLabel = TextView(this).apply {
            text = "Modo oscuro"
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setTextColor(getThemeColor())
        }
        switchLayout.addView(tvDarkModeLabel)

        switchDarkMode = Switch(this)
        switchLayout.addView(switchDarkMode)
        rootLayout.addView(switchLayout)

        // Selector de idioma
        val tvLanguageLabel = TextView(this).apply {
            text = "Idioma:"
            textSize = 16f
            setPadding(0, 16, 0, 8)
            setTextColor(getThemeColor())
        }
        rootLayout.addView(tvLanguageLabel)

        spinnerLanguage = Spinner(this).apply {
            setPadding(16, 16, 16, 16)
        }

        val languages = arrayOf("Español", "English", "Français", "Deutsch")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = adapter
        rootLayout.addView(spinnerLanguage)

        // Control de volumen
        val tvVolumeLabel = TextView(this).apply {
            text = "Volumen de notificaciones:"
            textSize = 16f
            setPadding(0, 24, 0, 8)
            setTextColor(getThemeColor())
        }
        rootLayout.addView(tvVolumeLabel)

        seekBarVolume = SeekBar(this).apply {
            max = 100
            progress = 50
            setPadding(0, 8, 0, 16)
        }
        rootLayout.addView(seekBarVolume)

        // Información de último acceso
        tvLastAccess = TextView(this).apply {
            text = "Último acceso: --"
            textSize = 14f
            setPadding(0, 16, 0, 8)
            setTextColor(getSecondaryTextColor())
        }
        rootLayout.addView(tvLastAccess)

        // Información de última ubicación
        tvLastLocation = TextView(this).apply {
            text = "Última ubicación: --"
            textSize = 14f
            setPadding(0, 8, 0, 8)
            setTextColor(getSecondaryTextColor())
        }
        rootLayout.addView(tvLastLocation)

        // Información de tiempo total de uso
        tvTotalUsage = TextView(this).apply {
            text = "Tiempo total: --"
            textSize = 14f
            setPadding(0, 8, 0, 24)
            setTextColor(getSecondaryTextColor())
        }
        rootLayout.addView(tvTotalUsage)

        // Botón guardar
        btnSave = Button(this).apply {
            text = "GUARDAR CONFIGURACIÓN"
            textSize = 16f
            setBackgroundColor(Color.parseColor("#2196F3"))
            setTextColor(Color.WHITE)
            setPadding(24, 16, 24, 16)
        }
        rootLayout.addView(btnSave)

        // Aplicar color de fondo al layout principal
        rootLayout.setBackgroundColor(getBackgroundColor())

        scrollView.addView(rootLayout)
        setContentView(scrollView)
    }

    private fun setupListeners() {
        btnSave.setOnClickListener {
            saveConfigurations()
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            // Guardar configuración primero
            val currentConfig = configManager.loadConfiguration()
            configManager.saveConfiguration(
                currentConfig.copy(darkModeEnabled = isChecked)
            )

            // Cambiar tema y recrear actividad
            changeTheme(isChecked)

            Toast.makeText(this,
                if (isChecked) "Modo oscuro activado" else "Modo claro activado",
                Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSavedConfigurations() {
        val config = configManager.loadConfiguration()

        etUserName.setText(config.userName)
        switchDarkMode.isChecked = config.darkModeEnabled

        val languages = arrayOf("Español", "English", "Français", "Deutsch")
        val languageIndex = languages.indexOf(config.preferredLanguage)
        if (languageIndex >= 0) {
            spinnerLanguage.setSelection(languageIndex)
        }

        seekBarVolume.progress = config.notificationVolume
        tvLastAccess.text = "Último acceso: ${config.lastAccessDate}"
        tvLastLocation.text = "Última ubicación: ${config.lastLocation}"
        tvTotalUsage.text = "Tiempo total: ${formatUsageTime(config.totalUsageTime)}"
    }

    private fun saveConfigurations() {
        val config = UserConfiguration(
            userName = etUserName.text.toString(),
            darkModeEnabled = switchDarkMode.isChecked,
            preferredLanguage = spinnerLanguage.selectedItem.toString(),
            notificationVolume = seekBarVolume.progress,
            lastAccessDate = Date().toString(),
            lastLocation = tvLastLocation.text.toString(),
            totalUsageTime = appUsageTracker.getTotalUsageTime()
        )

        configManager.saveConfiguration(config)
        Toast.makeText(this, "Configuraciones guardadas", Toast.LENGTH_SHORT).show()

        // Obtener ubicación actual
        getCurrentLocation()
    }

    private fun changeTheme(darkMode: Boolean) {
        if (darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        // Recrear la actividad para aplicar el tema inmediatamente
        recreate()
    }

    // Funciones auxiliares para obtener colores según el tema
    private fun isDarkMode(): Boolean {
        val nightModeFlags = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    private fun getThemeColor(): Int {
        return if (isDarkMode()) Color.WHITE else Color.BLACK
    }

    private fun getSecondaryTextColor(): Int {
        return if (isDarkMode()) Color.parseColor("#CCCCCC") else Color.parseColor("#666666")
    }

    private fun getBackgroundColor(): Int {
        return if (isDarkMode()) Color.parseColor("#121212") else Color.WHITE
    }

    private fun getEditTextBackgroundColor(): Int {
        // Usar un color personalizado según el tema
        val nightModeFlags = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            Color.parseColor("#333333") // Gris oscuro para modo oscuro
        } else {
            Color.parseColor("#F5F5F5") // Gris claro para modo claro
        }
    }

    private fun getHintColor(): Int {
        return if (isDarkMode()) Color.parseColor("#888888") else Color.parseColor("#999999")
    }

    private fun updateLastAccess() {
        val currentDate = Date().toString()
        tvLastAccess.text = "Último acceso: $currentDate"
    }

    private fun formatUsageTime(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return "${hours}h ${minutes % 60}m ${seconds % 60}s"
    }

    private fun getFirebaseToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("TOKEN", "Error al obtener el token", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("TOKEN", "Token del dispositivo: $token")
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val locationText = "Lat: ${it.latitude}, Lng: ${it.longitude}"
                    tvLastLocation.text = "Última ubicación: $locationText"
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appUsageTracker.stopTracking()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}