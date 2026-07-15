package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.LauncherDatabase
import com.example.data.LauncherRepository
import com.example.ui.LauncherViewModel
import com.example.ui.LauncherViewModelFactory
import com.example.ui.screens.AppDrawerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import java.io.File
import androidx.compose.ui.platform.LocalContext

enum class LauncherScreen {
    HOME, DRAWER, SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable true Edge-to-Edge full screen layout
        enableEdgeToEdge()

        // Initialize Room Database & Repository
        val database = LauncherDatabase.getDatabase(this)
        val repository = LauncherRepository(database.launcherDao())
        
        // Instantiate ViewModel
        val viewModel = ViewModelProvider(
            this,
            LauncherViewModelFactory(repository)
        )[LauncherViewModel::class.java]

        // Load apps asynchronously on start
        viewModel.loadApps(this)

        setContent {
            val settingsMap by viewModel.settingsMap.collectAsState()
            
            // Theme Selector: "dark", "light", "auto"
            val themeSelector = settingsMap["theme_mode_selector"] ?: "dark"
            val darkTheme = when (themeSelector) {
                "light" -> false
                "auto" -> androidx.compose.foundation.isSystemInDarkTheme()
                else -> true
            }
            
            val themeMode = settingsMap["theme_mode"] ?: "oled"
            
            // Dynamic card colors
            val cardBg = if (darkTheme) {
                if (themeMode == "oled") Color.Black else Color(0xFF141414)
            } else {
                Color(0xFFFFFFFF)
            }
            val cardBorder = if (darkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
            val cardText = if (darkTheme) Color.White else Color.Black
            val cardSecondaryText = if (darkTheme) Color.Gray else Color(0xFF666666)
            
            val fontSetting = settingsMap["app_font"] ?: "mono"
            val appFont = com.example.ui.theme.getAppFontFamily(fontSetting)

            MyApplicationTheme(darkTheme = darkTheme, dynamicColor = false) {
                CompositionLocalProvider(
                    com.example.ui.theme.LocalAccentColor provides com.example.ui.theme.getAccentColor(settingsMap),
                    com.example.ui.theme.LocalAppFont provides appFont,
                    com.example.ui.theme.LocalCardBackgroundColor provides cardBg,
                    com.example.ui.theme.LocalCardBorderColor provides cardBorder,
                    com.example.ui.theme.LocalCardTextColor provides cardText,
                    com.example.ui.theme.LocalCardSecondaryTextColor provides cardSecondaryText
                ) {
                    // Read active theme configurations
                    val wallpaperMode = settingsMap["wallpaper_mode"] ?: "system"
                    val wallpaperDim = (settingsMap["wallpaper_dim"] ?: "0.2").toFloatOrNull() ?: 0.2f
                    val wallpaperBlur = (settingsMap["wallpaper_blur"] ?: "10.0").toFloatOrNull() ?: 10.0f
                    val wallpaperBrightness = (settingsMap["wallpaper_brightness"] ?: "1.0").toFloatOrNull() ?: 1.0f

                    // Native system window blur on Android 12+ for system wallpaper
                    val blurRadiusPx = (wallpaperBlur * 3).coerceIn(0f, 150f).toInt()
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        LaunchedEffect(wallpaperMode, wallpaperBlur) {
                            val window = this@MainActivity.window
                            if (wallpaperMode == "system" && blurRadiusPx > 0) {
                                window.setBackgroundBlurRadius(blurRadiusPx)
                            } else {
                                window.setBackgroundBlurRadius(0)
                            }
                        }
                    }

                    // Read hide status bar configuration
                    val hideStatusBarSetting = settingsMap["hide_status_bar"] == "true"

                    // Dynamically hide/show the status bar
                    LaunchedEffect(hideStatusBarSetting) {
                        val window = this@MainActivity.window
                        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                        if (hideStatusBarSetting) {
                            insetsController.hide(WindowInsetsCompat.Type.statusBars())
                            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        } else {
                            insetsController.show(WindowInsetsCompat.Type.statusBars())
                        }
                    }

                    var currentScreen by remember { mutableStateOf(LauncherScreen.HOME) }

                    // Handle Hardware Back Button
                    BackHandler(enabled = currentScreen != LauncherScreen.HOME) {
                        currentScreen = LauncherScreen.HOME
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // --- Wallpaper Background Layer ---
                            val context = LocalContext.current
                            if (wallpaperMode == "custom") {
                                val customWallpaperFile = File(context.filesDir, "custom_wallpaper.png")
                                if (customWallpaperFile.exists()) {
                                    AsyncImage(
                                        model = customWallpaperFile,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .blur(wallpaperBlur.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    val fallbackColor = if (darkTheme) {
                                        if (themeMode == "oled") Color(0xFF050505) else Color(0xFF121212)
                                    } else {
                                        Color(0xFFF5F5F5)
                                    }
                                    Box(modifier = Modifier.fillMaxSize().background(fallbackColor))
                                }
                            } else if (wallpaperMode == "oled" || !darkTheme) {
                                val oledBg = if (darkTheme) Color.Black else Color(0xFFF5F5F5)
                                Box(modifier = Modifier.fillMaxSize().background(oledBg))
                            } else {
                                // Default background color when system wallpaper isn't drawn behind window
                                // or if showing transparent
                            }

                            // --- Dim & Brightness Overlays ---
                            if (wallpaperMode == "system" || wallpaperMode == "custom") {
                                // Dim Overlay
                                if (wallpaperDim > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = wallpaperDim))
                                    )
                                }
                                // Brightness adjustments
                                if (wallpaperBrightness < 1.0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = (1.0f - wallpaperBrightness).coerceIn(0f, 1f)))
                                    )
                                } else if (wallpaperBrightness > 1.0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.White.copy(alpha = ((wallpaperBrightness - 1.0f) * 0.5f).coerceIn(0f, 0.5f)))
                                    )
                                }
                            }

                            // --- Core Application Layout ---
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .pointerInput(settingsMap) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            if (currentScreen == LauncherScreen.HOME && dragAmount.y < -15f) {
                                                // Swipe UP opens App Drawer
                                                currentScreen = LauncherScreen.DRAWER
                                            } else if (currentScreen == LauncherScreen.HOME && dragAmount.y > 15f) {
                                                // Swipe DOWN opens notifications or quick settings
                                                val swipeDownAction = settingsMap["swipe_down_action"] ?: "notifications"
                                                if (com.example.services.LauncherAccessibilityService.isRunning()) {
                                                    if (swipeDownAction == "notifications") {
                                                        com.example.services.LauncherAccessibilityService.expandNotifications()
                                                    } else {
                                                        com.example.services.LauncherAccessibilityService.expandQuickSettings()
                                                    }
                                                } else {
                                                    // Fallback standard expand panel via system statusbar manager reflection
                                                    try {
                                                        val statusBarService = context.getSystemService("statusbar")
                                                        val statusBarManager = Class.forName("android.app.StatusBarManager")
                                                        val expandMethod = statusBarService.javaClass.getMethod("expandNotificationsPanel")
                                                        expandMethod.invoke(statusBarService)
                                                    } catch (e: Exception) {
                                                        android.widget.Toast.makeText(context, "Enable accessibility for gesture support", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } else if (currentScreen == LauncherScreen.DRAWER && dragAmount.y > 15f) {
                                                // Swipe DOWN inside Drawer returns Home
                                                currentScreen = LauncherScreen.HOME
                                            }
                                        }
                                    }
                            ) {
                            AnimatedContent(
                                targetState = currentScreen,
                                transitionSpec = {
                                    if (targetState == LauncherScreen.DRAWER || initialState == LauncherScreen.DRAWER) {
                                        // Slide up / down for app drawer
                                        slideInVertically(
                                            initialOffsetY = { if (targetState == LauncherScreen.DRAWER) it else -it },
                                            animationSpec = tween(300)
                                        ) togetherWith slideOutVertically(
                                            targetOffsetY = { if (targetState == LauncherScreen.HOME) it else -it },
                                            animationSpec = tween(300)
                                        )
                                    } else {
                                        // Fade transition for settings
                                        fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(250))
                                    }
                                },
                                label = "ScreenTransition"
                            ) { screen ->
                                when (screen) {
                                    LauncherScreen.HOME -> {
                                        HomeScreen(
                                            viewModel = viewModel,
                                            onSwipeUp = { currentScreen = LauncherScreen.DRAWER },
                                            onOpenSettings = { currentScreen = LauncherScreen.SETTINGS }
                                        )
                                    }
                                    LauncherScreen.DRAWER -> {
                                        AppDrawerScreen(
                                            viewModel = viewModel,
                                            onBackToHome = { currentScreen = LauncherScreen.HOME }
                                        )
                                    }
                                    LauncherScreen.SETTINGS -> {
                                        SettingsScreen(
                                            viewModel = viewModel,
                                            onBack = { currentScreen = LauncherScreen.HOME }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}
