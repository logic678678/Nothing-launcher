package com.example.ui.screens

import android.content.Context
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PinnedAppEntity
import com.example.ui.AppModel
import com.example.ui.LauncherViewModel
import com.example.ui.NothingAppIcon
import com.example.ui.components.*
import com.example.ui.theme.LocalAccentColor
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onSwipeUp: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pinnedApps by viewModel.pinnedApps.collectAsState()
    val quickNote by viewModel.quickNote.collectAsState()
    val settingsMap by viewModel.settingsMap.collectAsState()

    val weatherState by viewModel.weatherState.collectAsState()
    val stepCount by viewModel.stepCount.collectAsState()
    val mediaState by viewModel.mediaState.collectAsState()
    val calendarEvents by viewModel.calendarEvents.collectAsState()

    var hasMediaPermission by remember { mutableStateOf(false) }
    var hasLocationPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var hasStepsPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACTIVITY_RECOGNITION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCalendarPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CALENDAR
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val requestLocationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        hasLocationPermission = fineGranted || coarseGranted
        if (hasLocationPermission) {
            viewModel.updateLocationAndWeather(context)
        }
    }

    val requestStepsPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStepsPermission = isGranted
        if (isGranted) {
            viewModel.startStepListening(context)
        }
    }

    val requestCalendarPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCalendarPermission = isGranted
        if (isGranted) {
            viewModel.fetchCalendarEvents(context)
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasMediaPermission = com.example.services.NothingMediaListenerService.isPermissionGranted(context)
                hasLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                hasStepsPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACTIVITY_RECOGNITION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                hasCalendarPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_CALENDAR
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                if (hasStepsPermission) {
                    viewModel.startStepListening(context)
                }
                if (hasLocationPermission) {
                    viewModel.updateLocationAndWeather(context)
                }
                if (hasCalendarPermission) {
                    viewModel.fetchCalendarEvents(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val requestMediaPermission = {
        val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            requestLocationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            viewModel.updateLocationAndWeather(context)
        }
        if (!hasStepsPermission) {
            requestStepsPermissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            viewModel.startStepListening(context)
        }
        if (!hasCalendarPermission) {
            requestCalendarPermissionLauncher.launch(android.Manifest.permission.READ_CALENDAR)
        } else {
            viewModel.fetchCalendarEvents(context)
        }
    }

    val accentColor = LocalAccentColor.current
    val isMonochrome = (settingsMap["monochrome_icons"] ?: "true") == "true"

    var selectedAppForMenu by remember { mutableStateOf<PinnedAppEntity?>(null) }
    var isMenuVisible by remember { mutableStateOf(false) }
    var widgetToConfigure by remember { mutableStateOf<String?>(null) }

    // Dynamic Visibility and Size states for all 9 widgets
    val clockVisible = (settingsMap["widget_clock_visible"] ?: "true") == "true"
    val clockSize = settingsMap["widget_clock_size"] ?: "medium"

    val weatherVisible = (settingsMap["widget_weather_visible"] ?: "true") == "true"
    val weatherSize = settingsMap["widget_weather_size"] ?: "medium"

    val quickTogglesVisible = (settingsMap["widget_quick_toggles_visible"] ?: "true") == "true"
    val quickTogglesSize = settingsMap["widget_quick_toggles_size"] ?: "medium"

    val notesVisible = (settingsMap["widget_notes_visible"] ?: "true") == "true"
    val notesSize = settingsMap["widget_notes_size"] ?: "medium"

    val todoVisible = (settingsMap["widget_todo_visible"] ?: "true") == "true"
    val todoSize = settingsMap["widget_todo_size"] ?: "medium"

    val statsVisible = (settingsMap["widget_stats_visible"] ?: "true") == "true"
    val statsSize = settingsMap["widget_stats_size"] ?: "medium"

    val calendarVisible = (settingsMap["widget_calendar_visible"] ?: "false") == "true"
    val calendarSize = settingsMap["widget_calendar_size"] ?: "medium"

    val musicVisible = (settingsMap["widget_music_visible"] ?: "false") == "true"
    val musicSize = settingsMap["widget_music_size"] ?: "medium"

    val fitnessVisible = (settingsMap["widget_fitness_visible"] ?: "false") == "true"
    val fitnessSize = settingsMap["widget_fitness_size"] ?: "medium"

    val todoItems by viewModel.todoItems.collectAsState()

    // Custom layout blocks reordering
    val defaultBlocks = listOf("clock", "weather_quicktoggles", "notes", "todo", "stats", "calendar", "music", "fitness")
    val blocks = remember(settingsMap["widgets_order"]) {
        val orderStr = settingsMap["widgets_order"] ?: ""
        if (orderStr.isBlank()) {
            defaultBlocks
        } else {
            orderStr.split(",").filter { it in defaultBlocks }
        }
    }

    val homeScrollState = rememberScrollState()
    var touchStartY by remember { mutableStateOf(0f) }
    var touchStartX by remember { mutableStateOf(0f) }
    var hasTriggeredSwipe by remember { mutableStateOf(false) }

    val nestedScrollConnection = remember(settingsMap) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 15f) {
                    val swipeDownAction = settingsMap["swipe_down_action"] ?: "notifications"
                    if (com.example.services.LauncherAccessibilityService.isRunning()) {
                        if (swipeDownAction == "notifications") {
                            com.example.services.LauncherAccessibilityService.expandNotifications()
                        } else {
                            com.example.services.LauncherAccessibilityService.expandQuickSettings()
                        }
                    } else {
                        try {
                            val statusBarService = context.getSystemService("statusbar")
                            val expandMethod = statusBarService.javaClass.getMethod("expandNotificationsPanel")
                            expandMethod.invoke(statusBarService)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Enable accessibility for gesture support", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    return Offset(0f, available.y)
                }

                if (available.y < -15f) {
                    onSwipeUp()
                    return Offset(0f, available.y)
                }

                return Offset.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(settingsMap, homeScrollState) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull()
                        if (change != null) {
                            if (change.pressed) {
                                val isDown = change.previousPressed.not()
                                if (isDown) {
                                    touchStartY = change.position.y
                                    touchStartX = change.position.x
                                    hasTriggeredSwipe = false
                                } else if (!hasTriggeredSwipe) {
                                    val deltaY = change.position.y - touchStartY
                                    val deltaX = change.position.x - touchStartX
                                    
                                    // Swipe down threshold: > 100 pixels/dots and mostly vertical
                                    if (deltaY > 100f && kotlin.math.abs(deltaY) > kotlin.math.abs(deltaX) * 1.5f) {
                                        if (homeScrollState.value == 0) {
                                            hasTriggeredSwipe = true
                                            val swipeDownAction = settingsMap["swipe_down_action"] ?: "notifications"
                                            if (com.example.services.LauncherAccessibilityService.isRunning()) {
                                                if (swipeDownAction == "notifications") {
                                                    com.example.services.LauncherAccessibilityService.expandNotifications()
                                                } else {
                                                    com.example.services.LauncherAccessibilityService.expandQuickSettings()
                                                }
                                            } else {
                                                try {
                                                    val statusBarService = context.getSystemService("statusbar")
                                                    val expandMethod = statusBarService.javaClass.getMethod("expandNotificationsPanel")
                                                    expandMethod.invoke(statusBarService)
                                                } catch (e: Exception) {
                                                    android.widget.Toast.makeText(context, "Enable accessibility for gesture support", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Swipe up threshold: < -100 pixels/dots to open drawer
                                    if (deltaY < -100f && kotlin.math.abs(deltaY) > kotlin.math.abs(deltaX) * 1.5f) {
                                        if (homeScrollState.value >= homeScrollState.maxValue) {
                                            hasTriggeredSwipe = true
                                            onSwipeUp()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .pointerInput(settingsMap) {
                detectTapGestures(
                    onDoubleTap = {
                        val doubleTapToLock = settingsMap["double_tap_lock"] ?: "false"
                        if (doubleTapToLock == "true") {
                            if (com.example.services.LauncherAccessibilityService.isRunning()) {
                                com.example.services.LauncherAccessibilityService.performLockScreen()
                            } else {
                                android.widget.Toast.makeText(context, "Please enable Accessibility in Settings to support double-tap to lock screen", android.widget.Toast.LENGTH_LONG).show()
                            }
                        } else {
                            onOpenSettings()
                        }
                    },
                    onTap = { /* Dismiss app actions */ }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .nestedScroll(nestedScrollConnection)
                .verticalScroll(homeScrollState)
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            // Brand & Settings Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Text(
                        text = "NOTHING OS",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Launcher Settings",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dynamic Widget Blocks Rendering based on custom widgets_order list
            blocks.forEach { block ->
                val padKey = if (block == "weather_quicktoggles") "weather" else block
                val customPadding = (settingsMap["widget_${padKey}_custom_padding"] ?: "12").toIntOrNull() ?: 12
                val heightScale = (settingsMap["widget_${padKey}_height_scale"] ?: "1.0").toFloatOrNull() ?: 1.0f

                when (block) {
                    "clock" -> {
                        if (clockVisible) {
                            NothingClockWidget(
                                size = clockSize,
                                heightScale = heightScale,
                                onLongClick = { widgetToConfigure = "clock" },
                                modifier = Modifier.padding(vertical = customPadding.dp)
                            )
                        }
                    }
                    "weather_quicktoggles" -> {
                        val weatherPad = (settingsMap["widget_weather_custom_padding"] ?: "12").toIntOrNull() ?: 12
                        val weatherScale = (settingsMap["widget_weather_height_scale"] ?: "1.0").toFloatOrNull() ?: 1.0f
                        val togglePad = (settingsMap["widget_quick_toggles_custom_padding"] ?: "12").toIntOrNull() ?: 12
                        val toggleScale = (settingsMap["widget_quick_toggles_height_scale"] ?: "1.0").toFloatOrNull() ?: 1.0f

                        if (weatherVisible && quickTogglesVisible && weatherSize != "large" && quickTogglesSize != "large") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = ((weatherPad + togglePad) / 2).dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                NothingWeatherWidget(
                                    size = weatherSize,
                                    heightScale = weatherScale,
                                    weatherState = weatherState,
                                    hasPermission = hasLocationPermission,
                                    onRequestPermission = {
                                        requestLocationPermissionLauncher.launch(
                                            arrayOf(
                                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                                             )
                                        )
                                    },
                                    onRefresh = { viewModel.updateLocationAndWeather(context) },
                                    onLongClick = { widgetToConfigure = "weather" },
                                    modifier = Modifier.weight(1f)
                                )
                                NothingQuickTogglesWidget(
                                    size = quickTogglesSize,
                                    heightScale = toggleScale,
                                    onLongClick = { widgetToConfigure = "quick_toggles" },
                                    modifier = Modifier.weight(1.1f)
                                )
                            }
                        } else {
                            if (weatherVisible) {
                                NothingWeatherWidget(
                                    size = weatherSize,
                                    heightScale = weatherScale,
                                    weatherState = weatherState,
                                    hasPermission = hasLocationPermission,
                                    onRequestPermission = {
                                        requestLocationPermissionLauncher.launch(
                                            arrayOf(
                                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                                             )
                                        )
                                    },
                                    onRefresh = { viewModel.updateLocationAndWeather(context) },
                                    onLongClick = { widgetToConfigure = "weather" },
                                    modifier = Modifier.padding(vertical = weatherPad.dp)
                                )
                            }
                            if (quickTogglesVisible) {
                                NothingQuickTogglesWidget(
                                    size = quickTogglesSize,
                                    heightScale = toggleScale,
                                    onLongClick = { widgetToConfigure = "quick_toggles" },
                                    modifier = Modifier.padding(vertical = togglePad.dp)
                                )
                            }
                        }
                    }
                    "notes" -> {
                        if (notesVisible) {
                            NothingNotesWidget(
                                size = notesSize,
                                heightScale = heightScale,
                                savedNote = quickNote,
                                onSaveNote = { viewModel.saveQuickNote(it) },
                                onLongClick = { widgetToConfigure = "notes" },
                                modifier = Modifier.padding(vertical = customPadding.dp)
                            )
                        }
                    }
                    "todo" -> {
                        if (todoVisible) {
                            NothingTodoWidget(
                                size = todoSize,
                                heightScale = heightScale,
                                todoItems = todoItems,
                                onAddTodo = { viewModel.addTodoItem(it) },
                                onToggleTodo = { viewModel.toggleTodoItem(it) },
                                onDeleteTodo = { viewModel.deleteTodoItem(it) },
                                onLongClick = { widgetToConfigure = "todo" },
                                modifier = Modifier.padding(vertical = customPadding.dp)
                            )
                        }
                    }
                    "stats" -> {
                        if (statsVisible) {
                            NothingStatsWidget(
                                size = statsSize,
                                heightScale = heightScale,
                                onLongClick = { widgetToConfigure = "stats" },
                                modifier = Modifier.padding(vertical = customPadding.dp)
                            )
                        }
                    }
                    "calendar" -> {
                        if (calendarVisible) {
                            NothingCalendarWidget(
                                size = calendarSize,
                                heightScale = heightScale,
                                hasPermission = hasCalendarPermission,
                                onRequestPermission = { requestCalendarPermissionLauncher.launch(android.Manifest.permission.READ_CALENDAR) },
                                events = calendarEvents,
                                onLongClick = { widgetToConfigure = "calendar" },
                                modifier = Modifier.padding(vertical = customPadding.dp)
                            )
                        }
                    }
                    "music" -> {
                        if (musicVisible) {
                            NothingMusicWidget(
                                size = musicSize,
                                heightScale = heightScale,
                                mediaState = mediaState,
                                hasPermission = hasMediaPermission,
                                onRequestPermission = { requestMediaPermission() },
                                onTogglePlay = { com.example.services.NothingMediaListenerService.togglePlay() },
                                onNext = { com.example.services.NothingMediaListenerService.next() },
                                onPrevious = { com.example.services.NothingMediaListenerService.previous() },
                                onLongClick = { widgetToConfigure = "music" },
                                modifier = Modifier.padding(vertical = customPadding.dp)
                            )
                        }
                    }
                    "fitness" -> {
                        if (fitnessVisible) {
                            NothingFitnessWidget(
                                size = fitnessSize,
                                heightScale = heightScale,
                                stepCount = stepCount,
                                hasPermission = hasStepsPermission,
                                onRequestPermission = {
                                    requestStepsPermissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
                                },
                                onLongClick = { widgetToConfigure = "fitness" },
                                modifier = Modifier.padding(vertical = customPadding.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Pinned Apps Grid Heading
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PINNED APPLICATIONS",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                )
            }

            if (pinnedApps.isEmpty()) {
                // Onboarding card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NO PINNED APPS",
                            color = accentColor,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Swipe UP to open the App Drawer, then long-press any application and select 'Pin to Home' to pin it here.",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                // Render custom pinned apps using standard Row-wrapped items since we're nested inside verticalScroll.
                // We'll chunk the list into chunks of 4.
                val appChunks = pinnedApps.chunked(4)
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    for (chunk in appChunks) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (index in 0 until 4) {
                                if (index < chunk.size) {
                                    val pinnedApp = chunk[index]
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(76.dp)
                                            .combinedClickable(
                                                onClick = {
                                                    viewModel.launchApp(
                                                        context,
                                                        AppModel(
                                                            pinnedApp.packageName,
                                                            pinnedApp.activityName,
                                                            pinnedApp.label,
                                                            "PINNED"
                                                        )
                                                    )
                                                },
                                                onLongClick = {
                                                    selectedAppForMenu = pinnedApp
                                                    isMenuVisible = true
                                                }
                                            )
                                    ) {
                                        NothingAppIcon(
                                            packageName = pinnedApp.packageName,
                                            label = pinnedApp.label,
                                            isMonochrome = isMonochrome,
                                            context = context,
                                            size = 54.dp,
                                            settingsMap = settingsMap
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = pinnedApp.label,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(76.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        // --- Bottom Hint Slider ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clickable { onSwipeUp() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp, 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "SWIPE UP FOR DRAWER",
                color = Color.Gray,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        // --- Custom App Action Sheet / Modal ---
        if (isMenuVisible && selectedAppForMenu != null) {
            AlertDialog(
                onDismissRequest = { isMenuVisible = false },
                title = {
                    Text(
                        text = selectedAppForMenu!!.label.uppercase(),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.unpinApp(selectedAppForMenu!!.packageName)
                                isMenuVisible = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.PushPin, contentDescription = null, tint = accentColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("UNPIN FROM HOMESCREEN", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.hideApp(selectedAppForMenu!!.packageName)
                                viewModel.unpinApp(selectedAppForMenu!!.packageName)
                                isMenuVisible = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("HIDE APPLICATION", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { isMenuVisible = false }) {
                        Text("CANCEL", color = Color.Gray, fontFamily = FontFamily.Monospace)
                    }
                },
                containerColor = Color(0xFF121212),
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 8.dp
            )
        }

        // --- Custom Widget Action Menu ---
        if (widgetToConfigure != null) {
            val widgetKey = widgetToConfigure!!
            val widgetNameFormatted = widgetKey.replace("_", " ").uppercase()
            val currentSize = settingsMap["widget_${widgetKey}_size"] ?: "medium"

            AlertDialog(
                onDismissRequest = { widgetToConfigure = null },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Text(
                            text = "$widgetNameFormatted OPTIONS",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Resizing Options Heading
                        Text(
                            text = "CHOOSE DIMENSIONS",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        // Resize Pill buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("small", "medium", "large").forEach { sizeOption ->
                                val isSelected = currentSize == sizeOption
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (isSelected) accentColor else Color.White.copy(alpha = 0.08f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            viewModel.saveSetting("widget_${widgetKey}_size", sizeOption)
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = sizeOption.uppercase(),
                                        color = if (isSelected) Color.White else Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Manual height and padding sliders
                        val customHeightScale = (settingsMap["widget_${widgetKey}_height_scale"] ?: "1.0").toFloatOrNull() ?: 1.0f
                        Text(
                            text = "MANUAL HEIGHT MULTIPLIER: ${String.format(Locale.US, "%.1fx", customHeightScale)}",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Slider(
                            value = customHeightScale,
                            onValueChange = {
                                viewModel.saveSetting("widget_${widgetKey}_height_scale", String.format(Locale.US, "%.1f", it))
                            },
                            valueRange = 0.6f..1.8f,
                            colors = SliderDefaults.colors(
                                activeTrackColor = accentColor,
                                thumbColor = accentColor,
                                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )

                        val customPadding = (settingsMap["widget_${widgetKey}_custom_padding"] ?: "12").toIntOrNull() ?: 12
                        Text(
                            text = "MANUAL VERTICAL PADDING: ${customPadding} DP",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Slider(
                            value = customPadding.toFloat(),
                            onValueChange = {
                                viewModel.saveSetting("widget_${widgetKey}_custom_padding", it.toInt().toString())
                            },
                            valueRange = 0f..24f,
                            colors = SliderDefaults.colors(
                                activeTrackColor = accentColor,
                                thumbColor = accentColor,
                                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Rearrange Section
                        Text(
                            text = "ARRANGE LAYOUT",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val normalizedKey = if (widgetKey == "weather" || widgetKey == "quick_toggles") "weather_quicktoggles" else widgetKey
                            val index = blocks.indexOf(normalizedKey)
                            val canMoveUp = index > 0
                            val canMoveDown = index != -1 && index < blocks.size - 1

                            Button(
                                onClick = {
                                    if (canMoveUp) {
                                        val currentOrder = blocks.toMutableList()
                                        val temp = currentOrder[index]
                                        currentOrder[index] = currentOrder[index - 1]
                                        currentOrder[index - 1] = temp
                                        viewModel.saveSetting("widgets_order", currentOrder.joinToString(","))
                                    }
                                },
                                enabled = canMoveUp,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.05f),
                                    disabledContainerColor = Color.White.copy(alpha = 0.02f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowUpward,
                                    contentDescription = "Move Up",
                                    tint = if (canMoveUp) accentColor else Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("MOVE UP", color = if (canMoveUp) Color.White else Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                            }

                            Button(
                                onClick = {
                                    if (canMoveDown) {
                                        val currentOrder = blocks.toMutableList()
                                        val temp = currentOrder[index]
                                        currentOrder[index] = currentOrder[index + 1]
                                        currentOrder[index + 1] = temp
                                        viewModel.saveSetting("widgets_order", currentOrder.joinToString(","))
                                    }
                                },
                                enabled = canMoveDown,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.05f),
                                    disabledContainerColor = Color.White.copy(alpha = 0.02f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDownward,
                                    contentDescription = "Move Down",
                                    tint = if (canMoveDown) accentColor else Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("MOVE DOWN", color = if (canMoveDown) Color.White else Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Remove Button (Styled in striking red-accent)
                        Button(
                            onClick = {
                                viewModel.saveSetting("widget_${widgetKey}_visible", "false")
                                widgetToConfigure = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "REMOVE WIDGET",
                                color = accentColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { widgetToConfigure = null }) {
                        Text("DONE", color = Color.Gray, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color(0xFF141414),
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 8.dp
            )
        }
    }
}
