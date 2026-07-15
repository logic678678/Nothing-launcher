package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.staticCompositionLocalOf

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val LocalAccentColor = staticCompositionLocalOf { Color(0xFFFF1E1E) }

fun getAccentColor(settingsMap: Map<String, String>): Color {
    return when (settingsMap["accent_color"] ?: "red") {
        "red" -> Color(0xFFFF1E1E)       // Nothing Red (Striking bright red)
        "orange" -> Color(0xFFFF9500)    // Electric orange
        "yellow" -> Color(0xFFFFCC00)    // Dot gold
        "green" -> Color(0xFF10D05C)     // Emerald green
        "blue" -> Color(0xFF0088FF)      // Electric blue
        "pink" -> Color(0xFFFF2D55)      // Cyber pink
        "grey", "gray" -> Color(0xFF9E9E9E) // Minimalist dot-grey
        "white" -> Color(0xFFFFFFFF)     // Pure monochrome white
        else -> Color(0xFFFF1E1E)
    }
}
