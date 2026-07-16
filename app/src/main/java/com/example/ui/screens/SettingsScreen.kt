package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppModel
import com.example.ui.LauncherViewModel
import com.example.ui.NothingAppIcon
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalAppFont
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsMap by viewModel.settingsMap.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val hiddenAppsSet by viewModel.hiddenApps.collectAsState()

    val accentColor = LocalAccentColor.current
    val appFont = LocalAppFont.current

    var searchQuery by remember { mutableStateOf("") }
    fun matches(vararg keywords: String): Boolean {
        if (searchQuery.isBlank()) return true
        return keywords.any { keyword ->
            keyword.contains(searchQuery, ignoreCase = true)
        }
    }

    // Key preferences
    val isMonochrome = settingsMap["monochrome_icons"] ?: "true" == "true"
    val themeMode = settingsMap["theme_mode"] ?: "oled" // "oled" or "grey"

    // Hidden Apps models
    val hiddenAppsModels = remember(installedApps, hiddenAppsSet) {
        installedApps.filter { hiddenAppsSet.contains(it.packageName) }
    }

    var isHiddenAppsExpanded by remember { mutableStateOf(false) }
    val backgroundColor = if (themeMode == "oled") Color(0xFF050505) else Color(0xFF121212)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "LAUNCHER SETTINGS",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontFamily = appFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = backgroundColor
                )
            )
        },
        containerColor = backgroundColor,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // --- Brand Header ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.5.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "N",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontFamily = appFont,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "NOTHING OS STYLED",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = appFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "v1.0.0 Stable (Bloat-Free)",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = appFont
                    )
                }
            }

            // --- Settings Search Bar ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(20.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search Settings",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "SEARCH SETTINGS...",
                                color = Color.White.copy(alpha = 0.25f),
                                fontSize = 12.sp,
                                fontFamily = appFont,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = appFont,
                                letterSpacing = 0.5.sp
                            ),
                            cursorBrush = SolidColor(Color(0xFFFF3B30)),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear Search",
                            tint = Color.White,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            // --- Set as Default Launcher Card ---
            if (matches("default", "launcher", "system", "integration", "home")) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .clickable {
                                    try {
                                        val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        try {
                                            val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        } catch (ex: Exception) {
                                            val intent = Intent(Intent.ACTION_MAIN).apply {
                                                addCategory(Intent.CATEGORY_HOME)
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                        }
                                    }
                                }
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                    text = "SYSTEM INTEGRATION",
                                    color = accentColor,
                                    fontSize = 10.sp,
                                    fontFamily = appFont,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                            }
                            Text(
                                text = "Set as Default Launcher",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontFamily = appFont,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Tap here to open system settings and configure Nothing Launcher as your permanent default home screen.",
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                fontFamily = appFont,
                                lineHeight = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // --- Section: Visual Theme Settings ---
            if (matches("monochrome", "icons", "hide", "status bar", "theme", "accent", "color", "red", "orange", "green", "blue", "yellow", "purple", "pink", "grey", "white", "background", "aesthetic", "oled", "black", "grey", "dark grey", "visual", "identity", "gesture", "navigation")) {
                item {
                    Text(
                        text = "GESTURE NAVIGATION",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = appFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Toggle Gesture Navigation
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Gesture Navigation",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Enable swipe left for settings and swipe right for Utility Hub",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = appFont,
                                        lineHeight = 14.sp
                                    )
                                }
                                val gestureNavEnabled = (settingsMap["gesture_navigation_enabled"] ?: "true") == "true"
                                Switch(
                                    checked = gestureNavEnabled,
                                    onCheckedChange = { viewModel.saveSetting("gesture_navigation_enabled", it.toString()) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = Color.White,
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "VISUAL IDENTITY",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = appFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Toggle Monochrome Icons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Monochrome Icons",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Converts all apps to high contrast circular glyphs",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = appFont,
                                        lineHeight = 14.sp
                                    )
                                }
                                Switch(
                                    checked = isMonochrome,
                                    onCheckedChange = {
                                        viewModel.saveSetting("monochrome_icons", it.toString())
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = Color.White,
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            }
                            // Toggle App Drawer Style (Grid vs List)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "App Drawer Style",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Switch between grid view and alphabetical list view",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = appFont,
                                        lineHeight = 14.sp
                                    )
                                }
                                val isListView = (settingsMap["drawer_list_view"] ?: "false") == "true"
                                Switch(
                                    checked = isListView,
                                    onCheckedChange = { viewModel.saveSetting("drawer_list_view", it.toString()) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = Color.White,
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )

                            // Toggle Hide Status Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Hide Status Bar",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Hides the system status bar for a clean immersive look",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = appFont,
                                        lineHeight = 14.sp
                                    )
                                }
                                val hideStatusBar = settingsMap["hide_status_bar"] == "true"
                                Switch(
                                    checked = hideStatusBar,
                                    onCheckedChange = {
                                        viewModel.saveSetting("hide_status_bar", it.toString())
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = Color.White,
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )

                            // Toggle Vibration on Scroll
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Vibrate on Scroll",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Toggle haptic vibrations when fast-scrolling through letters in the app drawer",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = appFont,
                                        lineHeight = 14.sp
                                    )
                                }
                                val vibrateOnScroll = (settingsMap["vibrate_on_scroll"] ?: "true") == "true"
                                Switch(
                                    checked = vibrateOnScroll,
                                    onCheckedChange = {
                                        viewModel.saveSetting("vibrate_on_scroll", it.toString())
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = Color.White,
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )

                            // Toggle Black vs Dark Grey Theme
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Background Aesthetic",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontFamily = appFont,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (themeMode == "oled") Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                            .border(
                                                1.dp,
                                                if (themeMode == "oled") accentColor else Color.White.copy(alpha = 0.1f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { viewModel.saveSetting("theme_mode", "oled") }
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "OLED BLACK",
                                            color = if (themeMode == "oled") Color.White else Color.Gray,
                                            fontSize = 11.sp,
                                            fontFamily = appFont,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (themeMode == "grey") Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                            .border(
                                                1.dp,
                                                if (themeMode == "grey") accentColor else Color.White.copy(alpha = 0.1f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { viewModel.saveSetting("theme_mode", "grey") }
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "DARK GREY",
                                            color = if (themeMode == "grey") Color.White else Color.Gray,
                                            fontSize = 11.sp,
                                            fontFamily = appFont,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )

                            // Theme Mode Selector: "dark", "light", "auto"
                            val themeSelector = settingsMap["theme_mode_selector"] ?: "dark"
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Theme Mode",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontFamily = appFont,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("dark" to "DARK", "light" to "LIGHT", "auto" to "SYSTEM").forEach { (value, label) ->
                                        val isSelected = themeSelector == value
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) accentColor else Color.White.copy(alpha = 0.1f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable { viewModel.saveSetting("theme_mode_selector", value) }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSelected) Color.White else Color.Gray,
                                                fontSize = 10.sp,
                                                fontFamily = appFont,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }



                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )

                            // Accent Color Customization
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Accent Theme Color",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontFamily = appFont,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Choose a striking signature accent color for Nothing OS elements",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontFamily = appFont,
                                    lineHeight = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                val activeAccent = settingsMap["accent_color"] ?: "red"
                                val accentColorsList = listOf(
                                    "red" to Color(0xFFFF1E1E),
                                    "orange" to Color(0xFFFF6B00),
                                    "yellow" to Color(0xFFFFC700),
                                    "green" to Color(0xFF00E575),
                                    "blue" to Color(0xFF00B2FF),
                                    "pink" to Color(0xFFFF5B99),
                                    "grey" to Color(0xFF8E8E93),
                                    "white" to Color(0xFFFFFFFF)
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    accentColorsList.chunked(4).forEach { rowList ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            rowList.forEach { (name, colorValue) ->
                                                val isSelected = activeAccent == name
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(CircleShape)
                                                        .background(colorValue)
                                                        .border(
                                                            if (isSelected) 3.dp else 1.dp,
                                                            if (isSelected) Color.White else Color.White.copy(alpha = 0.1f),
                                                            CircleShape
                                                        )
                                                        .clickable {
                                                            viewModel.saveSetting("accent_color", name)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isSelected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .clip(CircleShape)
                                                                .background(if (colorValue == Color.White) Color.Black else Color.White)
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

            // --- Section: Custom Icon Packs & Adaptive Styling ---
            if (matches("icon", "pack", "adaptive", "styling", "monochrome", "shape", "color")) {
                item {
                    Text(
                        text = "ICON PACKS & ADAPTIVE STYLING",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = appFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Icon Pack Type Selector
                            val iconPackType = settingsMap["icon_pack_type"] ?: "default"
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Icon Pack Type",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontFamily = appFont,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Select between standard monochrome, custom adaptive colored plates, or third-party packages",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontFamily = appFont,
                                    lineHeight = 14.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("default" to "DEFAULT", "adaptive" to "ADAPTIVE", "custom" to "CUSTOM").forEach { (value, label) ->
                                        val isSelected = iconPackType == value
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) accentColor else Color.White.copy(alpha = 0.1f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable { viewModel.saveSetting("icon_pack_type", value) }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSelected) Color.White else Color.Gray,
                                                fontSize = 9.sp,
                                                fontFamily = appFont,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // If Adaptive is selected, show adaptive background and foreground customization
                            if (iconPackType == "adaptive") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color.White.copy(alpha = 0.08f))
                                )

                                val adaptiveBg = settingsMap["adaptive_icon_bg"] ?: "white"
                                val adaptiveFg = settingsMap["adaptive_icon_fg"] ?: "black"

                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "Adaptive Color Plate Settings",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold
                                    )

                                    // Background color select
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "Background Shape Color",
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            fontFamily = appFont
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(
                                                "none" to "NONE",
                                                "white" to "WHITE",
                                                "black" to "BLACK",
                                                "accent" to "ACCENT",
                                                "custom_grey" to "GREY"
                                            ).forEach { (value, label) ->
                                                val isSelected = adaptiveBg == value
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                                        .border(
                                                            1.dp,
                                                            if (isSelected) accentColor else Color.White.copy(alpha = 0.05f),
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable { viewModel.saveSetting("adaptive_icon_bg", value) }
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = label,
                                                        color = if (isSelected) Color.White else Color.Gray,
                                                        fontSize = 9.sp,
                                                        fontFamily = appFont
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Foreground color select
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "Logo Silhouette Tint Color",
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            fontFamily = appFont
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf(
                                                "black" to "BLACK",
                                                "white" to "WHITE",
                                                "accent" to "ACCENT"
                                            ).forEach { (value, label) ->
                                                val isSelected = adaptiveFg == value
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                                        .border(
                                                            1.dp,
                                                            if (isSelected) accentColor else Color.White.copy(alpha = 0.05f),
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable { viewModel.saveSetting("adaptive_icon_fg", value) }
                                                        .padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = label,
                                                        color = if (isSelected) Color.White else Color.Gray,
                                                        fontSize = 9.sp,
                                                        fontFamily = appFont
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // If Custom Icon Pack is selected, show installed icon packs
                            if (iconPackType == "custom") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color.White.copy(alpha = 0.08f))
                                )

                                val activeCustomPack = settingsMap["active_custom_icon_pack"] ?: ""
                                val installedPacks = remember {
                                    com.example.ui.AppIconProcessor.getInstalledIconPacks(context)
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Select Custom Icon Pack",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold
                                    )

                                    if (installedPacks.isEmpty()) {
                                        Text(
                                            text = "No compatible custom icon packs detected on this device. Install icon packs (e.g., 'Whicons', 'CandyCons') from the Play Store to choose them here.",
                                            color = Color.Gray,
                                            fontSize = 10.sp,
                                            fontFamily = appFont,
                                            lineHeight = 14.sp,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            installedPacks.forEach { pack ->
                                                val isSelected = activeCustomPack == pack.packageName
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                                                        .border(
                                                            1.dp,
                                                            if (isSelected) accentColor else Color.White.copy(alpha = 0.05f),
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable {
                                                            viewModel.saveSetting("active_custom_icon_pack", pack.packageName)
                                                        }
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = pack.name,
                                                            color = Color.White,
                                                            fontSize = 12.sp,
                                                            fontFamily = appFont,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = pack.packageName,
                                                            color = Color.Gray,
                                                            fontSize = 9.sp,
                                                            fontFamily = appFont
                                                        )
                                                    }
                                                    RadioButton(
                                                        selected = isSelected,
                                                        onClick = {
                                                            viewModel.saveSetting("active_custom_icon_pack", pack.packageName)
                                                        },
                                                        colors = RadioButtonDefaults.colors(
                                                            selectedColor = accentColor,
                                                            unselectedColor = Color.Gray
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // --- Section: Gesture Controls & Accessibility ---
            if (matches("gesture", "accessibility", "notifications", "control center", "lock", "double tap", "swipe")) {
                item {
                    Text(
                        text = "GESTURES & ACCESSIBILITY",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = appFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Accessibility service status row
                            val isServiceRunning = com.example.services.LauncherAccessibilityService.isRunning()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Accessibility Gestures",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isServiceRunning) "SERVICE IS RUNNING" else "TAP TO SETUP GESTURE ACCESS",
                                        color = if (isServiceRunning) accentColor else Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Could not open Accessibility settings", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isServiceRunning) Color.White.copy(alpha = 0.1f) else accentColor,
                                        contentColor = if (isServiceRunning) Color.White else Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (isServiceRunning) "SETUP" else "ENABLE",
                                        fontSize = 11.sp,
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )

                            // Swipe Down Preference Row
                            val swipeDownAction = settingsMap["swipe_down_action"] ?: "notifications"
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Swipe Down on Home Screen",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontFamily = appFont,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    listOf("notifications" to "NOTIFICATIONS", "control_center" to "CONTROL CENTER").forEach { (value, label) ->
                                        val isSelected = swipeDownAction == value
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) accentColor else Color.White.copy(alpha = 0.1f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable { viewModel.saveSetting("swipe_down_action", value) }
                                                .padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSelected) Color.White else Color.Gray,
                                                fontSize = 10.sp,
                                                fontFamily = appFont,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )

                            // Double Tap to Lock Toggle Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Double Tap to Lock Screen",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Locks screen when double tapping empty area of home screen",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = appFont,
                                        lineHeight = 14.sp
                                    )
                                }
                                val doubleTapLock = settingsMap["double_tap_lock"] == "true"
                                Switch(
                                    checked = doubleTapLock,
                                    onCheckedChange = {
                                        viewModel.saveSetting("double_tap_lock", it.toString())
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = Color.White,
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // --- Section: Wallpaper Styling ---
            if (matches("wallpaper", "oled", "black", "system", "custom", "background", "blur", "dim", "brightness", "source", "mode")) {
                item {
                    Text(
                        text = "WALLPAPER STYLING",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = appFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val context = LocalContext.current
                    val wallpaperMode = settingsMap["wallpaper_mode"] ?: "system"
                    val wallpaperDim = (settingsMap["wallpaper_dim"] ?: "0.2").toFloatOrNull() ?: 0.2f
                    val wallpaperBlur = (settingsMap["wallpaper_blur"] ?: "10.0").toFloatOrNull() ?: 10.0f
                    val wallpaperBrightness = (settingsMap["wallpaper_brightness"] ?: "1.0").toFloatOrNull() ?: 1.0f

                    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        if (uri != null) {
                            try {
                                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                    val file = File(context.filesDir, "custom_wallpaper.png")
                                    FileOutputStream(file).use { outputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                                    viewModel.saveSetting("wallpaper_mode", "custom")
                                    android.widget.Toast.makeText(context, "Wallpaper set successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("SettingsScreen", "Failed to save wallpaper", e)
                                android.widget.Toast.makeText(context, "Failed to save wallpaper image.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Use System Wallpaper",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Render your system wallpaper behind the launcher with blur and dim controls",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = appFont,
                                        lineHeight = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Switch(
                                    checked = wallpaperMode == "system",
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            viewModel.saveSetting("wallpaper_mode", "system")
                                        } else {
                                            viewModel.saveSetting("wallpaper_mode", "oled")
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = Color.White,
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )

                            Text(
                                text = "Wallpaper Source Mode",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = appFont,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            // Three mode selectors
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "system" to "SYSTEM",
                                    "oled" to "OLED BLACK",
                                    "custom" to "CUSTOM"
                                ).forEach { (mode, display) ->
                                    val isSelected = wallpaperMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                            .border(
                                                1.dp,
                                                if (isSelected) accentColor else Color.White.copy(alpha = 0.1f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                viewModel.saveSetting("wallpaper_mode", mode)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = display,
                                            color = if (isSelected) Color.White else Color.Gray,
                                            fontSize = 11.sp,
                                            fontFamily = appFont,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Select custom image button if mode is custom
                            if (wallpaperMode == "custom") {
                                Button(
                                    onClick = { wallpaperPickerLauncher.launch("image/*") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accentColor.copy(alpha = 0.1f),
                                        contentColor = accentColor
                                    ),
                                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "SELECT WALLPAPER IMAGE",
                                        fontFamily = appFont,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )

                            // Blur adjustments
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Wallpaper Blur",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${wallpaperBlur.toInt()} dp",
                                        color = accentColor,
                                        fontSize = 12.sp,
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Slider(
                                    value = wallpaperBlur,
                                    onValueChange = { viewModel.saveSetting("wallpaper_blur", it.toString()) },
                                    valueRange = 0f..25f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = accentColor,
                                        activeTrackColor = accentColor,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            }

                            // Dim adjustments
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Wallpaper Dim Level",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${(wallpaperDim * 100).toInt()}%",
                                        color = accentColor,
                                        fontSize = 12.sp,
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Slider(
                                    value = wallpaperDim,
                                    onValueChange = { viewModel.saveSetting("wallpaper_dim", it.toString()) },
                                    valueRange = 0f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = accentColor,
                                        activeTrackColor = accentColor,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            }

                            // Brightness adjustments
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Wallpaper Brightness",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${(wallpaperBrightness * 100).toInt()}%",
                                        color = accentColor,
                                        fontSize = 12.sp,
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Slider(
                                    value = wallpaperBrightness,
                                    onValueChange = { viewModel.saveSetting("wallpaper_brightness", it.toString()) },
                                    valueRange = 0.5f..1.5f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = accentColor,
                                        activeTrackColor = accentColor,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // --- Section: Widget Manager ---
            if (matches("widget", "clock", "weather", "quick", "toggles", "notes", "stats", "performance", "calendar", "music", "fitness", "steps", "track", "player", "manager")) {
                item {
                    Text(
                        text = "WIDGET MANAGER",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = appFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Toggle Active Widgets",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = appFont,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            Text(
                                text = "Enable or disable custom-styled Nothing OS widgets to customize your home screen workspace.",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontFamily = appFont,
                                lineHeight = 14.sp
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )

                            val widgetsList = listOf(
                                Triple("clock", "Clock Widget", "Displays dynamic analogue & digital clock styles"),
                                Triple("weather", "Weather Widget", "Displays real-time atmospheric conditions & custom cities"),
                                Triple("quick_toggles", "Quick Toggles", "Displays custom dynamic toggle controls"),
                                Triple("notes", "Quick Notes", "A minimal notepad that persists scribbled thoughts instantly"),
                                Triple("stats", "Performance Stats", "Monitors active RAM, CPU load, & device temperature"),
                                Triple("calendar", "Calendar Widget", "A clean monochrome overview of today's date & schedule"),
                                Triple("music", "Music Player", "Control music media playback instantly with track titles"),
                                Triple("fitness", "Fitness Tracker", "Counts real-time step goals and active thresholds")
                            )

                            widgetsList.forEachIndexed { index, (key, label, desc) ->
                                val isVisible = (settingsMap["widget_${key}_visible"] ?: (if (key in listOf("calendar", "music", "fitness")) "false" else "true")) == "true"

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = label,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontFamily = appFont,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = desc,
                                            color = Color.Gray,
                                            fontSize = 10.sp,
                                            fontFamily = appFont,
                                            lineHeight = 13.sp
                                        )
                                    }
                                    Switch(
                                        checked = isVisible,
                                        onCheckedChange = { checked ->
                                            viewModel.saveSetting("widget_${key}_visible", checked.toString())
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = Color.White,
                                            uncheckedThumbColor = Color.Gray,
                                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                        )
                                    )
                                }

                                if (index < widgetsList.lastIndex) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .height(1.dp)
                                            .background(Color.White.copy(alpha = 0.04f))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Section: Hidden Apps Management ---
            if (matches("privacy", "control", "hidden", "applications", "apps", "unhide")) {
                item {
                    Text(
                        text = "PRIVACY CONTROL",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = appFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(
                            modifier = Modifier
                                .clickable { isHiddenAppsExpanded = !isHiddenAppsExpanded }
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.VisibilityOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Hidden Applications",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${hiddenAppsModels.size} APPS",
                                    color = accentColor,
                                    fontSize = 11.sp,
                                    fontFamily = appFont,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            AnimatedVisibility(visible = isHiddenAppsExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (hiddenAppsModels.isEmpty()) {
                                        Text(
                                            text = "No hidden apps. You can long-press apps in the drawer or home screen to hide them.",
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            fontFamily = appFont,
                                            lineHeight = 16.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        hiddenAppsModels.forEach { app ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    NothingAppIcon(
                                                        packageName = app.packageName,
                                                        label = app.label,
                                                        isMonochrome = true,
                                                        context = context,
                                                        size = 36.dp,
                                                        settingsMap = settingsMap
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = app.label,
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontFamily = appFont,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Text(
                                                    text = "UNHIDE",
                                                    color = accentColor,
                                                    fontSize = 11.sp,
                                                    fontFamily = appFont,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .clickable { viewModel.unhideApp(app.packageName) }
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
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

            // --- Section: Performance & Battery Efficiency Dashboard ---
            if (matches("power", "efficiency", "battery", "ram", "memory", "footprint", "performance", "optimization", "offline", "database", "telemetry")) {
                item {
                    Text(
                        text = "POWER & EFFICIENCY",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = appFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.BatteryChargingFull, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Ultra Low Footprint Mode",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontFamily = appFont,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "Nothing Launcher is meticulously crafted to maximize battery endurance and minimize RAM footprint using core OS capabilities:",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                fontFamily = appFont,
                                lineHeight = 16.sp
                            )

                            val statsTips = listOf(
                                "OLED Pixel optimization: Using deep monochrome theme.",
                                "Strict Package caching: Prevents costly continuous binder queries.",
                                "Zero background execution: No active sync polling or trackers.",
                                "Fully Offline Local Database: Zero telemetry API connections."
                            )

                            statsTips.forEach { tip ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = "•", color = accentColor, fontSize = 12.sp, fontFamily = appFont)
                                    Text(
                                        text = tip,
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = appFont,
                                        lineHeight = 14.sp
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
