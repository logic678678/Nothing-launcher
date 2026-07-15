package com.example.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.LauncherRepository
import com.example.data.PinnedAppEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import com.example.services.NothingMediaListenerService
import com.example.services.MediaState
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import java.util.Locale
import java.util.Calendar
import java.text.SimpleDateFormat
import android.provider.CalendarContract

data class CalendarEvent(
    val title: String,
    val startTime: String,
    val isToday: Boolean
)

data class WeatherState(
    val tempCelsius: Float = 21f,
    val condition: String = "CLEAR",
    val locationName: String = "LONDON",
    val isFetching: Boolean = false,
    val error: String? = null
)

data class AppModel(
    val packageName: String,
    val activityName: String,
    val label: String,
    val category: String
)

class LauncherViewModel(private val repository: LauncherRepository) : ViewModel() {

    // Weather widget state
    private val _weatherState = MutableStateFlow(WeatherState())
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    // Step tracker state
    private val _stepCount = MutableStateFlow(0)
    val stepCount: StateFlow<Int> = _stepCount.asStateFlow()

    // Calendar events state
    private val _calendarEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarEvent>> = _calendarEvents.asStateFlow()

    private var sensorManager: SensorManager? = null
    private var stepSensorListener: SensorEventListener? = null

    // Media state observed from the NotificationListener service
    val mediaState: StateFlow<MediaState> = NothingMediaListenerService.mediaState

    // UI Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // UI Tab Selection for drawer
    private val _selectedCategory = MutableStateFlow("ALL")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Apps raw list loaded from package manager
    private val _installedApps = MutableStateFlow<List<AppModel>>(emptyList())
    val installedApps: StateFlow<List<AppModel>> = _installedApps.asStateFlow()

    // Room Database State observations
    val pinnedApps: StateFlow<List<PinnedAppEntity>> = repository.pinnedApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hiddenApps: StateFlow<Set<String>> = repository.hiddenApps
        .map { list -> list.map { it.packageName }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val quickNote: StateFlow<String> = repository.quickNote
        .map { it?.content ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Launcher Settings Flow loaded as a Map for reactive UI toggles
    private val _settingsMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val settingsMap: StateFlow<Map<String, String>> = _settingsMap.asStateFlow()

    init {
        // Collect database settings and update map
        viewModelScope.launch {
            repository.allSettings.collect { settings ->
                _settingsMap.value = settings.associate { it.key to it.value }
            }
        }
    }

    // Load applications from package manager
    fun loadApps(context: Context) {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val list = queryInstalledApps(context)
                // Pre-warm the icon cache in background thread
                for (app in list) {
                    try {
                        AppIconProcessor.getAppIcon(context, app.packageName)
                    } catch (e: Exception) {
                        Log.e("LauncherViewModel", "Failed to pre-warm icon for ${app.packageName}", e)
                    }
                }
                list
            }
            _installedApps.value = apps
        }
    }

    private fun queryInstalledApps(context: Context): List<AppModel> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolvedActivities = pm.queryIntentActivities(intent, 0)
        val appList = mutableListOf<AppModel>()

        for (resolveInfo in resolvedActivities) {
            val packageName = resolveInfo.activityInfo.packageName
            val activityName = resolveInfo.activityInfo.name
            val label = resolveInfo.loadLabel(pm).toString()
            val flags = resolveInfo.activityInfo.applicationInfo.flags
            val systemCategory = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                resolveInfo.activityInfo.applicationInfo.category
            } else {
                -1
            }

            // Categorize app based on intent info or heuristics
            val category = heuristicallyCategorize(packageName, label, flags, systemCategory)

            appList.add(AppModel(packageName, activityName, label, category))
        }

        return appList.sortedBy { it.label.lowercase() }
    }

    private fun heuristicallyCategorize(
        packageName: String,
        label: String,
        flags: Int,
        systemCategory: Int
    ): String {
        val lowerPkg = packageName.lowercase()
        val lowerLabel = label.lowercase()

        // 1. Respect system categorization where available
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            when (systemCategory) {
                ApplicationInfo.CATEGORY_GAME -> return "GAMES"
                ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_IMAGE -> return "MEDIA"
                ApplicationInfo.CATEGORY_SOCIAL -> return "SOCIAL"
                ApplicationInfo.CATEGORY_MAPS, ApplicationInfo.CATEGORY_NEWS, ApplicationInfo.CATEGORY_PRODUCTIVITY -> return "UTILITY"
            }
        }

        // 2. Custom Heuristic mappings (highly precise offline matchers)
        return when {
            // Games
            lowerPkg.contains("game") || lowerPkg.contains("play") && !lowerPkg.contains("store") || lowerLabel.contains("game") -> "GAMES"
            
            // Social / Chatting
            lowerPkg.contains("chat") || lowerPkg.contains("messenger") || lowerPkg.contains("telegram") ||
                    lowerPkg.contains("whatsapp") || lowerPkg.contains("social") || lowerPkg.contains("facebook") ||
                    lowerPkg.contains("twitter") || lowerPkg.contains("instagram") || lowerPkg.contains("discord") ||
                    lowerPkg.contains("viber") || lowerPkg.contains("snapchat") || lowerPkg.contains("skype") -> "SOCIAL"
            
            // Media
            lowerPkg.contains("music") || lowerPkg.contains("player") || lowerPkg.contains("video") ||
                    lowerPkg.contains("youtube") || lowerPkg.contains("spotify") || lowerPkg.contains("media") ||
                    lowerPkg.contains("gallery") || lowerPkg.contains("photo") || lowerPkg.contains("camera") ||
                    lowerPkg.contains("recorder") || lowerPkg.contains("sound") || lowerPkg.contains("tv") -> "MEDIA"
            
            // System Core apps
            (flags and ApplicationInfo.FLAG_SYSTEM) != 0 || lowerPkg.contains("android") ||
                    lowerPkg.contains("providers") || lowerPkg.contains("systemui") || lowerPkg.contains("launcher") ||
                    lowerPkg.contains("settings") || lowerLabel.contains("setting") || lowerLabel.contains("phone") ||
                    lowerLabel.contains("contacts") || lowerLabel.contains("files") || lowerLabel.contains("download") -> "SYSTEM"
            
            // Default to UTILITY
            else -> "UTILITY"
        }
    }

    // --- Search & Tab Filters ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    // Get filtered list for App Drawer
    val filteredApps: StateFlow<List<AppModel>> = combine(
        _installedApps,
        _searchQuery,
        _selectedCategory,
        hiddenApps
    ) { apps, query, category, hidden ->
        apps.filter { app ->
            // Filter out hidden apps in app drawer
            !hidden.contains(app.packageName) &&
            // Matches category tab
            (category == "ALL" || app.category == category) &&
            // Matches search query
            (query.isBlank() || app.label.contains(query, ignoreCase = true) || app.packageName.contains(query, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Core Launcher Functions ---
    fun launchApp(context: Context, app: AppModel) {
        try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                Log.e("LauncherViewModel", "Intent is null for app: ${app.packageName}")
            }
        } catch (e: Exception) {
            Log.e("LauncherViewModel", "Failed to launch ${app.packageName}", e)
        }
    }

    fun pinApp(app: AppModel) {
        viewModelScope.launch {
            val index = pinnedApps.value.size
            repository.pinApp(app.packageName, app.activityName, app.label, index)
        }
    }

    fun unpinApp(packageName: String) {
        viewModelScope.launch {
            repository.unpinApp(packageName)
        }
    }

    fun hideApp(packageName: String) {
        viewModelScope.launch {
            repository.hideApp(packageName)
        }
    }

    fun unhideApp(packageName: String) {
        viewModelScope.launch {
            repository.unhideApp(packageName)
        }
    }

    fun saveQuickNote(content: String) {
        viewModelScope.launch {
            repository.saveQuickNote(content)
        }
    }

    fun saveSetting(key: String, value: String) {
        viewModelScope.launch {
            repository.saveSetting(key, value)
        }
    }

    fun getSetting(key: String, defaultValue: String): String {
        return settingsMap.value[key] ?: defaultValue
    }

    // Step sensor listener registration
    fun startStepListening(context: Context) {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (sensorManager == null) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        }

        val stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val stepDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        if (stepCounterSensor != null) {
            var initialSteps = -1f
            stepSensorListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                        val steps = event.values[0]
                        if (initialSteps < 0) {
                            initialSteps = steps - _stepCount.value
                        } else {
                            _stepCount.value = (steps - initialSteps).toInt()
                        }
                    } else if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                        _stepCount.value += 1
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager?.registerListener(stepSensorListener, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)
            if (stepDetectorSensor != null) {
                sensorManager?.registerListener(stepSensorListener, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI)
            }
        } else {
            // Accelerometer fallback for emulators & non-step-sensor devices
            val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (accelSensor != null) {
                var isPeak = false
                var lastStepTime = 0L
                stepSensorListener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]
                        val magnitude = kotlin.math.sqrt(x*x + y*y + z*z)
                        
                        val currentTime = System.currentTimeMillis()
                        // Increased threshold to 13.5f and added 350ms cooldown to reduce shaking sensitivity
                        if (magnitude > 13.5f && !isPeak) {
                            if (currentTime - lastStepTime > 350L) {
                                isPeak = true
                                _stepCount.value += 1
                                lastStepTime = currentTime
                            }
                        } else if (magnitude < 9.5f) {
                            isPeak = false
                        }
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }
                sensorManager?.registerListener(stepSensorListener, accelSensor, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    fun stopStepListening() {
        stepSensorListener?.let {
            sensorManager?.unregisterListener(it)
        }
        stepSensorListener = null
    }

    // Query real system calendar events
    fun fetchCalendarEvents(context: Context) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            _calendarEvents.value = emptyList()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val eventsList = mutableListOf<CalendarEvent>()
            try {
                val uri = CalendarContract.Events.CONTENT_URI
                val projection = arrayOf(
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.DTEND,
                    CalendarContract.Events.ALL_DAY
                )

                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val startMillis = cal.timeInMillis

                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val endMillis = cal.timeInMillis

                val selection = "(${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTSTART} <= ?) AND (${CalendarContract.Events.DELETED} != 1)"
                val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString())
                val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

                val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
                cursor?.use { c ->
                    val titleIdx = c.getColumnIndex(CalendarContract.Events.TITLE)
                    val startIdx = c.getColumnIndex(CalendarContract.Events.DTSTART)
                    val allDayIdx = c.getColumnIndex(CalendarContract.Events.ALL_DAY)

                    while (c.moveToNext()) {
                        val title = if (titleIdx != -1) c.getString(titleIdx) ?: "No Title" else "No Title"
                        val dtstart = if (startIdx != -1) c.getLong(startIdx) else 0L
                        val allDay = if (allDayIdx != -1) c.getInt(allDayIdx) == 1 else false

                        val timeStr = if (allDay) {
                            "ALL DAY"
                        } else {
                            val eventCal = Calendar.getInstance().apply { timeInMillis = dtstart }
                            SimpleDateFormat("h:mm a", Locale.US).format(eventCal.time)
                        }

                        eventsList.add(CalendarEvent(title, timeStr, true))
                    }
                }
            } catch (e: Exception) {
                Log.e("LauncherViewModel", "Failed to query calendar", e)
            }

            // Fallback to simple default holidays if no events are listed
            if (eventsList.isEmpty()) {
                val today = Calendar.getInstance()
                val month = today.get(Calendar.MONTH) // 0-indexed
                val day = today.get(Calendar.DAY_OF_MONTH)
                val holiday = getHolidayForDate(month, day)
                if (holiday != null) {
                    eventsList.add(CalendarEvent(holiday, "ALL DAY", true))
                } else {
                    eventsList.add(CalendarEvent("NO EVENTS TODAY", "FREE", true))
                }
            }

            _calendarEvents.value = eventsList
        }
    }

    private fun getHolidayForDate(month: Int, day: Int): String? {
        return when {
            month == Calendar.JANUARY && day == 1 -> "NEW YEAR'S DAY"
            month == Calendar.FEBRUARY && day == 14 -> "VALENTINE'S DAY"
            month == Calendar.MARCH && day == 17 -> "ST. PATRICK'S DAY"
            month == Calendar.JULY && day == 4 -> "INDEPENDENCE DAY"
            month == Calendar.OCTOBER && day == 31 -> "HALLOWEEN"
            month == Calendar.NOVEMBER && day == 24 -> "THANKSGIVING DAY"
            month == Calendar.DECEMBER && day == 24 -> "CHRISTMAS EVE"
            month == Calendar.DECEMBER && day == 25 -> "CHRISTMAS DAY"
            month == Calendar.DECEMBER && day == 31 -> "NEW YEAR'S EVE"
            else -> null
        }
    }

    fun fetchWeatherForLocation(context: Context, latitude: Double, longitude: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _weatherState.value = _weatherState.value.copy(isFetching = true)
                val url = URL("https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current_weather=true")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                
                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    
                    val json = JSONObject(response.toString())
                    val currentWeather = json.getJSONObject("current_weather")
                    val temp = currentWeather.getDouble("temperature").toFloat()
                    val weatherCode = currentWeather.getInt("weathercode")
                    
                    val condition = when (weatherCode) {
                        0 -> "CLEAR"
                        1, 2, 3 -> "OVERCAST"
                        45, 48 -> "FOGGY"
                        51, 53, 55, 61, 63, 65, 80, 81, 82 -> "RAINY"
                        71, 73, 75, 77, 85, 86 -> "SNOWY"
                        95, 96, 99 -> "STORMY"
                        else -> "OVERCAST"
                    }
                    
                    var locName = "CURRENT LOCATION"
                    try {
                        val geocoder = android.location.Geocoder(context, Locale.getDefault())
                        if (android.os.Build.VERSION.SDK_INT >= 33) {
                            geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                                if (addresses.isNotEmpty()) {
                                    val address = addresses[0]
                                    val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: "CURRENT LOCATION"
                                    _weatherState.value = WeatherState(
                                        tempCelsius = temp,
                                        condition = condition,
                                        locationName = city.uppercase(),
                                        isFetching = false,
                                        error = null
                                    )
                                }
                            }
                            return@launch
                        } else {
                            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                            if (!addresses.isNullOrEmpty()) {
                                val address = addresses[0]
                                locName = address.locality ?: address.subAdminArea ?: address.adminArea ?: "CURRENT LOCATION"
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("LauncherViewModel", "Geocoder failed", e)
                    }
                    
                    _weatherState.value = WeatherState(
                        tempCelsius = temp,
                        condition = condition,
                        locationName = locName.uppercase(),
                        isFetching = false,
                        error = null
                    )
                } else {
                    _weatherState.value = _weatherState.value.copy(isFetching = false, error = "HTTP ${conn.responseCode}")
                }
            } catch (e: Exception) {
                Log.e("LauncherViewModel", "Weather fetch failed", e)
                _weatherState.value = _weatherState.value.copy(isFetching = false, error = e.localizedMessage)
            }
        }
    }

    fun updateLocationAndWeather(context: Context) {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            _weatherState.value = _weatherState.value.copy(error = "No Location Permission")
            return
        }

        viewModelScope.launch {
            try {
                _weatherState.value = _weatherState.value.copy(isFetching = true)
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
                val isNetworkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
                
                var location: android.location.Location? = null
                if (isNetworkEnabled) {
                    location = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                }
                if (location == null && isGpsEnabled) {
                    location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                }
                
                if (location != null) {
                    fetchWeatherForLocation(context, location.latitude, location.longitude)
                } else {
                    var updated = false
                    val provider = if (isNetworkEnabled) android.location.LocationManager.NETWORK_PROVIDER else android.location.LocationManager.GPS_PROVIDER
                    
                    try {
                        locationManager.requestSingleUpdate(provider, object : android.location.LocationListener {
                            override fun onLocationChanged(loc: android.location.Location) {
                                if (!updated) {
                                    updated = true
                                    fetchWeatherForLocation(context, loc.latitude, loc.longitude)
                                }
                            }
                            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                            override fun onProviderEnabled(provider: String) {}
                            override fun onProviderDisabled(provider: String) {}
                        }, android.os.Looper.getMainLooper())
                    } catch (e: Exception) {
                        Log.e("LauncherViewModel", "requestSingleUpdate failed", e)
                    }

                    // 1.5 seconds timeout fallback to IP Geolocation
                    viewModelScope.launch(Dispatchers.IO) {
                        kotlinx.coroutines.delay(1500)
                        if (!updated) {
                            updated = true
                            Log.d("LauncherViewModel", "GPS/Network location timed out. Falling back to IP Geolocation.")
                            fetchLocationViaIp(context)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LauncherViewModel", "Location retrieval failed, falling back to IP", e)
                fetchLocationViaIp(context)
            }
        }
    }

    private fun fetchLocationViaIp(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://ipapi.co/json/")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    
                    val json = JSONObject(response.toString())
                    val lat = json.optDouble("latitude", 0.0)
                    val lon = json.optDouble("longitude", 0.0)
                    val city = json.optString("city", "CURRENT LOCATION")
                    
                    if (lat != 0.0 || lon != 0.0) {
                        val weatherUrl = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true")
                        val weatherConn = weatherUrl.openConnection() as HttpURLConnection
                        weatherConn.requestMethod = "GET"
                        weatherConn.connectTimeout = 4000
                        weatherConn.readTimeout = 4000
                        if (weatherConn.responseCode == 200) {
                            val wReader = BufferedReader(InputStreamReader(weatherConn.inputStream))
                            val wResponse = StringBuilder()
                            var wLine: String?
                            while (wReader.readLine().also { wLine = it } != null) {
                                wResponse.append(wLine)
                            }
                            wReader.close()
                            
                            val wJson = JSONObject(wResponse.toString())
                            val currentWeather = wJson.getJSONObject("current_weather")
                            val temp = currentWeather.getDouble("temperature").toFloat()
                            val weatherCode = currentWeather.getInt("weathercode")
                            
                            val condition = when (weatherCode) {
                                0 -> "CLEAR"
                                1, 2, 3 -> "OVERCAST"
                                45, 48 -> "FOGGY"
                                51, 53, 55, 61, 63, 65, 80, 81, 82 -> "RAINY"
                                71, 73, 75, 77, 85, 86 -> "SNOWY"
                                95, 96, 99 -> "STORMY"
                                else -> "OVERCAST"
                            }
                            
                            _weatherState.value = WeatherState(
                                tempCelsius = temp,
                                condition = condition,
                                locationName = city.uppercase(),
                                isFetching = false,
                                error = null
                            )
                            return@launch
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LauncherViewModel", "IP Geolocation failed", e)
            }
            
            // Final fallback to New York
            _weatherState.value = WeatherState(
                tempCelsius = 22f,
                condition = "CLEAR",
                locationName = "NEW YORK",
                isFetching = false,
                error = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopStepListening()
    }
}

// ViewModel Factory
class LauncherViewModelFactory(private val repository: LauncherRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LauncherViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LauncherViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
