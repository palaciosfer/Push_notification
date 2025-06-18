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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.messaging.FirebaseMessaging
import java.util.*

// ViewModel para manejar el estado del tema
class ThemeViewModel : ViewModel() {
    var isDarkMode = false
        private set

    fun toggleDarkMode() {
        isDarkMode = !isDarkMode
    }

    fun setDarkMode(darkMode: Boolean) {
        isDarkMode = darkMode
    }
}

class MainActivity : AppCompatActivity() {
    private lateinit var configManager: ConfigurationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var appUsageTracker: AppUsageTracker
    private lateinit var themeViewModel: ThemeViewModel

    private lateinit var etUserName: EditText
    private lateinit var spinnerLanguage: Spinner
    private lateinit var seekBarVolume: SeekBar
    private lateinit var tvLastAccess: TextView
    private lateinit var tvLastLocation: TextView
    private lateinit var tvTotalUsage: TextView
    private lateinit var btnSave: Button
    private lateinit var btnToggleTheme: Button
    private lateinit var rootLayout: LinearLayout
    private lateinit var scrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar managers y ViewModel
        configManager = ConfigurationManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        appUsageTracker = AppUsageTracker(this)
        themeViewModel = ThemeViewModel()

        // Cargar tema guardado
        loadSavedTheme()

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

    private fun createLayout() {
        scrollView = ScrollView(this)

        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Aplicar tema inicial
        applyTheme()

        // Título de la aplicación
        val tvTitle = TextView(this).apply {
            text = getString(R.string.configuration_title)
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        rootLayout.addView(tvTitle)

        // Botón para cambiar tema
        btnToggleTheme = Button(this).apply {
            text = if (themeViewModel.isDarkMode) getString(R.string.light_mode) else getString(R.string.dark_mode)
            textSize = 14f
            setPadding(16, 12, 16, 12)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                setMargins(0, 0, 0, 24)
            }
            layoutParams = params
        }
        rootLayout.addView(btnToggleTheme)

        // Campo de nombre de usuario
        val tvUserNameLabel = TextView(this).apply {
            text = getString(R.string.username_label)
            textSize = 16f
            setPadding(0, 16, 0, 8)
        }
        rootLayout.addView(tvUserNameLabel)

        etUserName = EditText(this).apply {
            hint = getString(R.string.username_hint)
            setPadding(16, 16, 16, 16)
        }
        rootLayout.addView(etUserName)

        // Selector de idioma
        val tvLanguageLabel = TextView(this).apply {
            text = getString(R.string.language_label)
            textSize = 16f
            setTextColor(Color.parseColor("#000000"))
            setPadding(0, 16, 0, 8)

        }
        rootLayout.addView(tvLanguageLabel)

        spinnerLanguage = Spinner(this).apply {
            setPadding(16, 16, 16, 16)


        }

        val languages = arrayOf(
            getString(R.string.spanish),
            getString(R.string.english),
            getString(R.string.french),
            getString(R.string.german)

        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = adapter
        rootLayout.addView(spinnerLanguage)

        // Control de volumen
        val tvVolumeLabel = TextView(this).apply {
            text = getString(R.string.volume_label)
            textSize = 16f
            setPadding(0, 24, 0, 8)
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
            text = "${getString(R.string.last_access)} ${getString(R.string.no_data)}"
            textSize = 14f
            setPadding(0, 16, 0, 8)
        }
        rootLayout.addView(tvLastAccess)

        // Información de última ubicación
        tvLastLocation = TextView(this).apply {
            text = "${getString(R.string.last_location)} ${getString(R.string.no_data)}"
            textSize = 14f
            setPadding(0, 8, 0, 8)
        }
        rootLayout.addView(tvLastLocation)

        // Información de tiempo total de uso
        tvTotalUsage = TextView(this).apply {
            text = "${getString(R.string.total_usage)} ${getString(R.string.no_data)}"
            textSize = 14f
            setPadding(0, 8, 0, 24)
        }
        rootLayout.addView(tvTotalUsage)

        // Botón guardar
        btnSave = Button(this).apply {
            text = getString(R.string.save_button)
            textSize = 16f
            setPadding(24, 16, 24, 16)
        }
        rootLayout.addView(btnSave)

        scrollView.addView(rootLayout)
        setContentView(scrollView)
    }

    private fun applyTheme() {
        val isDark = themeViewModel.isDarkMode

        // Colores para modo oscuro y claro
        val backgroundColor = if (isDark) Color.parseColor("#121212") else Color.WHITE
        val textColor = if (isDark) Color.WHITE else Color.BLACK
        val secondaryTextColor = if (isDark) Color.parseColor("#BBBBBB") else Color.parseColor("#666666")
        val inputBackgroundColor = if (isDark) Color.parseColor("#1E1E1E") else Color.parseColor("#F5F5F5")
        val buttonBackgroundColor = if (isDark) Color.parseColor("#BB86FC") else Color.parseColor("#2196F3")
        val toggleButtonColor = if (isDark) Color.parseColor("#03DAC6") else Color.parseColor("#FF9800")

        // Aplicar colores al layout principal
        rootLayout.setBackgroundColor(backgroundColor)
        scrollView.setBackgroundColor(backgroundColor)

        // Aplicar colores a todos los TextViews
        for (i in 0 until rootLayout.childCount) {
            val child = rootLayout.getChildAt(i)
            when (child) {
                is TextView -> {
                    if (child == tvLastAccess || child == tvLastLocation || child == tvTotalUsage) {
                        child.setTextColor(secondaryTextColor)
                    } else {
                        child.setTextColor(textColor)
                    }
                }
                is EditText -> {
                    child.setTextColor(textColor)
                    child.setBackgroundColor(inputBackgroundColor)
                    child.setHintTextColor(secondaryTextColor)
                }
                is Button -> {
                    when (child) {
                        btnToggleTheme -> {
                            child.setBackgroundColor(toggleButtonColor)
                            child.setTextColor(Color.WHITE)
                        }
                        btnSave -> {
                            child.setBackgroundColor(buttonBackgroundColor)
                            child.setTextColor(Color.WHITE)
                        }
                    }
                }
            }
        }
    }

    private var isUserSelection = false

    private fun setupListeners() {
        btnSave.setOnClickListener {
            saveConfigurations()
        }

        btnToggleTheme.setOnClickListener {
            themeViewModel.toggleDarkMode()
            btnToggleTheme.text = if (themeViewModel.isDarkMode) getString(R.string.light_mode) else getString(R.string.dark_mode)
            applyTheme()
            saveThemePreference()
        }

        // Listener para cambio de idioma - solo cuando el usuario lo selecciona
        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                // Solo cambiar idioma si es una selección del usuario, no la carga inicial
                if (isUserSelection) {
                    val selectedLanguage = when (position) {
                        0 -> "es" // Español
                        1 -> "en" // English
                        2 -> "fr" // Français
                        3 -> "de" // Deutsch
                        else -> "es"
                    }
                    changeLanguage(selectedLanguage)
                }
                // Después de la primera carga, habilitar las selecciones del usuario
                isUserSelection = true
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun changeLanguage(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        // Recrear la actividad para aplicar los cambios
        recreate()
    }

    private fun loadSavedConfigurations() {
        val config = configManager.loadConfiguration()

        etUserName.setText(config.userName)

        // Deshabilitar temporalmente la selección del usuario
        isUserSelection = false

        val languages = arrayOf("Español", "English", "Français", "Deutsch")
        val languageIndex = languages.indexOf(config.preferredLanguage)
        if (languageIndex >= 0) {
            spinnerLanguage.setSelection(languageIndex)
        }

        seekBarVolume.progress = config.notificationVolume
        tvLastAccess.text = "${getString(R.string.last_access)} ${config.lastAccessDate}"
        tvLastLocation.text = "${getString(R.string.last_location)} ${config.lastLocation}"
        tvTotalUsage.text = "${getString(R.string.total_usage)} ${formatUsageTime(config.totalUsageTime)}"
    }

    private fun saveConfigurations() {
        val config = UserConfiguration(
            userName = etUserName.text.toString(),
            darkModeEnabled = themeViewModel.isDarkMode,
            preferredLanguage = spinnerLanguage.selectedItem.toString(),
            notificationVolume = seekBarVolume.progress,
            lastAccessDate = Date().toString(),
            lastLocation = tvLastLocation.text.toString(),
            totalUsageTime = appUsageTracker.getTotalUsageTime()
        )

        configManager.saveConfiguration(config)
        Toast.makeText(this, getString(R.string.configurations_saved), Toast.LENGTH_SHORT).show()

        // Obtener ubicación actual
        getCurrentLocation()
    }

    private fun loadSavedTheme() {
        val config = configManager.loadConfiguration()
        themeViewModel.setDarkMode(config.darkModeEnabled)
    }

    private fun saveThemePreference() {
        // Guardar solo la preferencia del tema sin afectar otras configuraciones
        val currentConfig = configManager.loadConfiguration()
        val updatedConfig = currentConfig.copy(darkModeEnabled = themeViewModel.isDarkMode)
        configManager.saveConfiguration(updatedConfig)
    }

    private fun updateLastAccess() {
        val currentDate = Date().toString()
        tvLastAccess.text = "${getString(R.string.last_access)} $currentDate"
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
                    tvLastLocation.text = "${getString(R.string.last_location)} $locationText"
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