package com.example.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalCardBackgroundColor
import com.example.ui.theme.LocalCardBorderColor
import com.example.ui.theme.LocalCardTextColor
import com.example.ui.theme.LocalCardSecondaryTextColor
import com.example.services.MediaState
import com.example.services.NothingMediaListenerService
import com.example.ui.WeatherState

// --- DOT MATRIX DIGITAL RENDERER ---
// A custom 5x7 dot matrix representation for characters
private val DotMatrixFont = mapOf(
    '0' to listOf(
        " ### ",
        "#   #",
        "#  ##",
        "# # #",
        "##  #",
        "#   #",
        " ### "
    ),
    '1' to listOf(
        "  #  ",
        " ##  ",
        "  #  ",
        "  #  ",
        "  #  ",
        "  #  ",
        " ### "
    ),
    '2' to listOf(
        " ### ",
        "#   #",
        "    #",
        "  ## ",
        " #   ",
        "#    ",
        "#####"
    ),
    '3' to listOf(
        "#####",
        "    #",
        "   # ",
        "  ## ",
        "    #",
        "#   #",
        " ### "
    ),
    '4' to listOf(
        "   # ",
        "  ## ",
        " # # ",
        "#  # ",
        "#####",
        "   # ",
        "   # "
    ),
    '5' to listOf(
        "#####",
        "#    ",
        "#### ",
        "    #",
        "    #",
        "#   #",
        " ### "
    ),
    '6' to listOf(
        "  ## ",
        " #   ",
        "#    ",
        "#### ",
        "#   #",
        "#   #",
        " ### "
    ),
    '7' to listOf(
        "#####",
        "    #",
        "   # ",
        "  #  ",
        " #   ",
        " #   ",
        " #   "
    ),
    '8' to listOf(
        " ### ",
        "#   #",
        "#   #",
        " ### ",
        "#   #",
        "#   #",
        " ### "
    ),
    '9' to listOf(
        " ### ",
        "#   #",
        "#   #",
        " ####",
        "    #",
        "   # ",
        " ##  "
    ),
    ':' to listOf(
        "     ",
        "  #  ",
        "     ",
        "     ",
        "     ",
        "  #  ",
        "     "
    ),
    ' ' to listOf(
        "     ",
        "     ",
        "     ",
        "     ",
        "     ",
        "     ",
        "     "
    )
)

@Composable
fun DotMatrixCharacter(
    char: Char,
    dotColor: Color,
    inactiveDotColor: Color,
    modifier: Modifier = Modifier,
    dotSize: Float = 4f,
    spacing: Float = 2f
) {
    val matrix = DotMatrixFont[char] ?: DotMatrixFont[' ']!!

    Canvas(modifier = modifier.size((5 * (dotSize * 2 + spacing)).dp, (7 * (dotSize * 2 + spacing)).dp)) {
        val totalSpacingX = spacing
        val totalSpacingY = spacing
        val dRadius = dotSize

        for (row in 0 until 7) {
            val line = matrix[row]
            for (col in 0 until 5) {
                val isFilled = col < line.length && line[col] == '#'
                val cx = col * (dRadius * 2 + totalSpacingX) + dRadius + 4f
                val cy = row * (dRadius * 2 + totalSpacingY) + dRadius + 4f

                drawCircle(
                    color = if (isFilled) dotColor else inactiveDotColor,
                    radius = dRadius,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

@Composable
fun DotMatrixText(
    text: String,
    dotColor: Color = Color.White,
    inactiveDotColor: Color = Color.White.copy(alpha = 0.05f),
    modifier: Modifier = Modifier,
    dotSize: Float = 3f,
    spacing: Float = 1.5f
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (char in text) {
            DotMatrixCharacter(
                char = char,
                dotColor = dotColor,
                inactiveDotColor = inactiveDotColor,
                dotSize = dotSize,
                spacing = spacing
            )
        }
    }
}

// --- WIDGET 1: NOTHING CLOCK WIDGET ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NothingClockWidget(
    size: String = "medium",
    heightScale: Float = 1.0f,
    modifier: Modifier = Modifier,
    onWidgetClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null
) {
    var clockStyle by remember { mutableStateOf(0) } // 0 = Dot Matrix, 1 = Bento Bold, 2 = Analog
    var is24Hour by remember { mutableStateOf(false) }
    var showSecondsHand by remember { mutableStateOf(true) }
    var showClockCustomizer by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance()
            delay(1000)
        }
    }

    val timeString = remember(currentTime, is24Hour) {
        val sdf = SimpleDateFormat(if (is24Hour) "HH:mm" else "hh:mm", Locale.getDefault())
        sdf.format(currentTime.time)
    }

    val amPmString = remember(currentTime) {
        val sdf = SimpleDateFormat("a", Locale.getDefault())
        sdf.format(currentTime.time).uppercase()
    }

    val dateString = remember(currentTime) {
        val sdf = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
        sdf.format(currentTime.time).uppercase()
    }

    val defaultHeight = if (size == "small") 110.dp else 165.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(defaultHeight * heightScale)
            .combinedClickable(
                onClick = { onWidgetClick() },
                onLongClick = {
                    clockStyle = (clockStyle + 1) % 3
                    onLongClick?.invoke() ?: run { showClockCustomizer = true }
                }
            ),
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.LocalCardBackgroundColor.current),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.5.dp, com.example.ui.theme.LocalCardBorderColor.current)
    ) {
        Column(
            modifier = Modifier
                .padding(if (size == "small") 12.dp else 24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (clockStyle) {
                0 -> {
                    // Digital Dot Matrix
                    DotMatrixText(
                        text = timeString,
                        dotColor = com.example.ui.theme.LocalCardTextColor.current,
                        inactiveDotColor = com.example.ui.theme.LocalCardTextColor.current.copy(alpha = 0.03f),
                        dotSize = if (size == "small") 3.2f else 5f,
                        spacing = if (size == "small") 1.2f else 2f
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = dateString,
                            color = com.example.ui.theme.LocalCardSecondaryTextColor.current,
                            fontSize = 10.sp,
                            fontFamily = com.example.ui.theme.LocalAppFont.current,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        if (!is24Hour) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = amPmString,
                                color = if (showSecondsHand) Color(0xFFFF3B30) else com.example.ui.theme.LocalCardTextColor.current,
                                fontSize = 11.sp,
                                fontFamily = com.example.ui.theme.LocalAppFont.current,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                1 -> {
                    // Clean Bento Digital (09:41 style, with red colon)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = dateString,
                            color = com.example.ui.theme.LocalCardTextColor.current.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            fontFamily = com.example.ui.theme.LocalAppFont.current,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = timeString.substringBefore(":"),
                                color = com.example.ui.theme.LocalCardTextColor.current,
                                fontSize = if (size == "small") 36.sp else 64.sp,
                                fontFamily = com.example.ui.theme.LocalAppFont.current,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-2).sp
                            )
                            Text(
                                text = ":",
                                color = if (showSecondsHand) Color(0xFFFF3B30) else com.example.ui.theme.LocalCardTextColor.current,
                                fontSize = if (size == "small") 36.sp else 64.sp,
                                fontFamily = com.example.ui.theme.LocalAppFont.current,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                            Text(
                                text = timeString.substringAfter(":"),
                                color = com.example.ui.theme.LocalCardTextColor.current,
                                fontSize = if (size == "small") 36.sp else 64.sp,
                                fontFamily = com.example.ui.theme.LocalAppFont.current,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-2).sp
                            )
                            if (!is24Hour) {
                                Text(
                                    text = " $amPmString",
                                    color = com.example.ui.theme.LocalCardTextColor.current.copy(alpha = 0.5f),
                                    fontSize = 14.sp,
                                    fontFamily = com.example.ui.theme.LocalAppFont.current,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "NOTHING OS STYLED",
                            color = com.example.ui.theme.LocalCardTextColor.current.copy(alpha = 0.4f),
                            fontSize = 9.sp,
                            fontFamily = com.example.ui.theme.LocalAppFont.current,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                }
                2 -> {
                    // Analog Clock
                    val cardTextColor = com.example.ui.theme.LocalCardTextColor.current
                    val cardBgColor = com.example.ui.theme.LocalCardBackgroundColor.current
                    Box(
                        modifier = Modifier.size(if (size == "small") 90.dp else 120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(if (size == "small") 80.dp else 110.dp)) {
                            val radius = this.size.minDimension / 2
                            val center = Offset(this.size.width / 2, this.size.height / 2)

                            // Border circle
                            drawCircle(
                                color = cardTextColor.copy(alpha = 0.1f),
                                radius = radius,
                                style = Stroke(width = 1.dp.toPx())
                            )

                            // 12, 3, 6, 9 Dot markings
                            for (angle in listOf(0, 90, 180, 270)) {
                                val rad = Math.toRadians(angle.toDouble())
                                val dotRadius = 3.dp.toPx()
                                val cx = center.x + (radius - 12.dp.toPx()) * cos(rad).toFloat()
                                val cy = center.y + (radius - 12.dp.toPx()) * sin(rad).toFloat()
                                drawCircle(
                                    color = if (angle == 270 && showSecondsHand) Color(0xFFFF3B30) else cardTextColor,
                                    radius = dotRadius,
                                    center = Offset(cx, cy)
                                )
                            }

                            // Hands
                            val hour = currentTime.get(Calendar.HOUR)
                            val minute = currentTime.get(Calendar.MINUTE)
                            val second = currentTime.get(Calendar.SECOND)

                            // Hour hand
                            val hourAngle = Math.toRadians((hour * 30 + minute * 0.5 - 90))
                            val hourLength = radius * 0.5f
                            drawLine(
                                color = cardTextColor,
                                start = center,
                                end = Offset(
                                    center.x + hourLength * cos(hourAngle).toFloat(),
                                    center.y + hourLength * sin(hourAngle).toFloat()
                                ),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )

                            // Minute hand
                            val minuteAngle = Math.toRadians((minute * 6 - 90).toDouble())
                            val minuteLength = radius * 0.75f
                            drawLine(
                                color = cardTextColor,
                                start = center,
                                end = Offset(
                                    center.x + minuteLength * cos(minuteAngle).toFloat(),
                                    center.y + minuteLength * sin(minuteAngle).toFloat()
                                ),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )

                            // Second hand (Nothing Red)
                            val secondAngle = Math.toRadians((second * 6 - 90).toDouble())
                            val secondLength = radius * 0.85f
                            if (showSecondsHand) {
                                drawLine(
                                    color = Color(0xFFFF3B30),
                                    start = center,
                                    end = Offset(
                                        center.x + secondLength * cos(secondAngle).toFloat(),
                                        center.y + secondLength * sin(secondAngle).toFloat()
                                    ),
                                    strokeWidth = 1.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }

                            // Center pin
                            drawCircle(
                                color = cardBgColor,
                                radius = 3.dp.toPx()
                            )
                            drawCircle(
                                color = if (showSecondsHand) Color(0xFFFF3B30) else cardTextColor,
                                radius = 1.5.dp.toPx()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = dateString,
                        color = cardTextColor,
                        fontSize = 12.sp,
                        fontFamily = com.example.ui.theme.LocalAppFont.current,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }

    // Custom Options Dialog
    if (showClockCustomizer) {
        AlertDialog(
            onDismissRequest = { showClockCustomizer = false },
            title = {
                Text(
                    text = "CLOCK CONFIGURATION",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "CLOCK FACE",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("DOT MATRIX", "BENTO BOLD", "ANALOG").forEachIndexed { index, name ->
                            val isSelected = clockStyle == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFFFF3B30) else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .background(
                                        if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { clockStyle = index }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "24-HOUR FORMAT",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = is24Hour,
                            onCheckedChange = { is24Hour = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFFF3B30),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RED ACCENTS / SECOND HAND",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = showSecondsHand,
                            onCheckedChange = { showSecondsHand = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFFF3B30),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showClockCustomizer = false }) {
                    Text("DONE", color = Color(0xFFFF3B30), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF161616),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp
        )
    }
}

// --- WIDGET 2: NOTHING QUICK TOGGLES & BATTERY ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NothingQuickTogglesWidget(
    size: String = "medium",
    heightScale: Float = 1.0f,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isFlashlightOn by remember { mutableStateOf(false) }
    var batteryPercentage by remember { mutableStateOf(100) }
    var batteryStatusText by remember { mutableStateOf("DISCHARGING") }
    var ringerMode by remember { mutableStateOf(AudioManager.RINGER_MODE_NORMAL) }

    var showTogglesCustomizer by remember { mutableStateOf(false) }
    var batteryRingStyle by remember { mutableStateOf(0) } // 0 = Standard, 1 = Dot Segmented
    var batteryColorOption by remember { mutableStateOf(0) } // 0 = Auto (White/Red), 1 = Always Red, 2 = Modern White

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }

    // Toggle Flashlight safely
    val toggleFlashlight = {
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull()
            if (cameraId != null) {
                isFlashlightOn = !isFlashlightOn
                cameraManager.setTorchMode(cameraId, isFlashlightOn)
            }
        } catch (e: Exception) {
            isFlashlightOn = false
            Log.e("NothingWidgets", "Flashlight toggle failed: ${e.message}")
        }
    }

    // Toggle Ringer Mode
    val toggleRinger = {
        val nextMode = when (ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
            AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
            else -> AudioManager.RINGER_MODE_NORMAL
        }
        try {
            audioManager.ringerMode = nextMode
            ringerMode = nextMode
        } catch (e: Exception) {
            // Some systems require Do Not Disturb permissions, fallback to checking mode and opening settings if failed
            try {
                ringerMode = audioManager.ringerMode
            } catch (ignored: Exception) {}
            // Gracefully open setting if failed
            val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    // Monitor Battery state
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (level != -1 && scale != -1) {
                        batteryPercentage = (level * 100 / scale.toFloat()).toInt()
                    }
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    batteryStatusText = when (status) {
                        BatteryManager.BATTERY_STATUS_CHARGING -> "CHARGING"
                        BatteryManager.BATTERY_STATUS_FULL -> "FULL"
                        else -> "DISCHARGING"
                    }
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose {
            context.unregisterReceiver(receiver)
            // Ensure flashlight turns off on dispose
            if (isFlashlightOn) {
                try {
                    val cameraId = cameraManager.cameraIdList.firstOrNull()
                    if (cameraId != null) cameraManager.setTorchMode(cameraId, false)
                } catch (ignored: Exception) {}
            }
        }
    }

    val defaultHeight = if (size == "small") 110.dp else 165.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(defaultHeight * heightScale)
            .combinedClickable(
                onClick = { /* Primary actions are discrete buttons inside, but tap of container is safe */ },
                onLongClick = { onLongClick?.invoke() ?: run { showTogglesCustomizer = true } }
            ),
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.LocalCardBackgroundColor.current),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.5.dp, com.example.ui.theme.LocalCardBorderColor.current)
    ) {
        val cardTextColor = com.example.ui.theme.LocalCardTextColor.current
        val cardSecondaryTextColor = com.example.ui.theme.LocalCardSecondaryTextColor.current
        Row(
            modifier = Modifier
                .padding(if (size == "small") 8.dp else 16.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Circular Battery Ring
            Box(
                modifier = Modifier
                    .size(if (size == "small") 56.dp else 80.dp)
                    .clip(CircleShape)
                    .background(cardTextColor.copy(alpha = 0.03f))
                    .border(1.dp, cardTextColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val animatedProgress = remember { Animatable(0f) }
                LaunchedEffect(batteryPercentage) {
                    animatedProgress.animateTo(
                        targetValue = batteryPercentage / 100f,
                        animationSpec = tween(durationMillis = 1000)
                    )
                }

                Canvas(modifier = Modifier.size(70.dp)) {
                    // Gray Background ring
                    drawCircle(
                        color = cardTextColor.copy(alpha = 0.08f),
                        style = Stroke(width = 4.dp.toPx())
                    )

                    val dashEffect = if (batteryRingStyle == 1) {
                        androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                    } else {
                        null
                    }

                    val ringColor = when (batteryColorOption) {
                        1 -> Color(0xFFFF3B30) // Always Red
                        2 -> cardTextColor // Always Adaptive Text Color
                        else -> {
                            // Standard Auto
                            if (batteryPercentage < 20) Color(0xFFFF3B30) else cardTextColor
                        }
                    }

                    // Pinned White/Adaptive ring
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress.value,
                        useCenter = false,
                        style = Stroke(
                            width = 4.dp.toPx(), 
                            cap = StrokeCap.Round,
                            pathEffect = dashEffect
                        )
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$batteryPercentage%",
                        color = cardTextColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = com.example.ui.theme.LocalAppFont.current
                    )
                    Text(
                        text = if (batteryStatusText == "CHARGING") "CHRG" else "BAT",
                        color = cardSecondaryTextColor,
                        fontSize = 8.sp,
                        fontFamily = com.example.ui.theme.LocalAppFont.current,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(60.dp)
                    .background(cardTextColor.copy(alpha = 0.15f))
            )

            // Quick Actions List
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Torch Toggle Button
                    val cardBgColor = com.example.ui.theme.LocalCardBackgroundColor.current
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isFlashlightOn) cardTextColor else cardTextColor.copy(alpha = 0.05f))
                            .clickable { toggleFlashlight() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFlashlightOn) Icons.Filled.FlashlightOn else Icons.Outlined.FlashlightOff,
                            contentDescription = "Flashlight",
                            tint = if (isFlashlightOn) cardBgColor else cardTextColor
                        )
                    }

                    // Sound Ring Toggle Button
                    val ringerIcon = when (ringerMode) {
                        AudioManager.RINGER_MODE_NORMAL -> Icons.Filled.VolumeUp
                        AudioManager.RINGER_MODE_VIBRATE -> Icons.Filled.Vibration
                        else -> Icons.Filled.VolumeMute
                    }
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(cardTextColor.copy(alpha = 0.05f))
                            .clickable { toggleRinger() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ringerIcon,
                            contentDescription = "Ringer Mode",
                            tint = cardTextColor
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Wi-Fi settings shortcut button
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(cardTextColor.copy(alpha = 0.05f))
                            .clickable {
                                try {
                                    val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e("NothingWidgets", "Failed to launch wifi settings: ${e.message}")
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Wifi,
                            contentDescription = "Wi-Fi Settings",
                            tint = cardTextColor
                        )
                    }

                    // System Settings shortcut button
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(cardTextColor.copy(alpha = 0.05f))
                            .clickable {
                                try {
                                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e("NothingWidgets", "Failed to launch system settings: ${e.message}")
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "System Settings",
                            tint = cardTextColor
                        )
                    }
                }
            }
        }
    }

    // Custom Options Dialog for Toggles/Battery widget
    if (showTogglesCustomizer) {
        AlertDialog(
            onDismissRequest = { showTogglesCustomizer = false },
            title = {
                Text(
                    text = "BATTERY WIDGET OPTIONS",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "RING STYLE",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SOLID RING", "DOT SEGMENTED").forEachIndexed { index, name ->
                            val isSelected = batteryRingStyle == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFFFF3B30) else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .background(
                                        if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { batteryRingStyle = index }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = "HIGHLIGHT COLOR",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("AUTO (RED/WHT)", "ACCENT RED", "SILENT WHITE").forEachIndexed { index, name ->
                            val isSelected = batteryColorOption == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFFFF3B30) else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .background(
                                        if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { batteryColorOption = index }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTogglesCustomizer = false }) {
                    Text("DONE", color = Color(0xFFFF3B30), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF161616),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp
        )
    }
}

// --- WIDGET 3: NOTHING MONOCHROME WEATHER ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NothingWeatherWidget(
    size: String = "medium",
    heightScale: Float = 1.0f,
    weatherState: WeatherState = WeatherState(),
    hasPermission: Boolean = true,
    onRequestPermission: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current
    var useFahrenheit by remember { mutableStateOf(false) }
    var showWeatherCustomizer by remember { mutableStateOf(false) }

    val cities = listOf("NEW YORK", "LONDON", "TOKYO")
    var cityIndex by remember { mutableStateOf(0) }
    val conditions = listOf("CLEAR", "OVERCAST", "STORMY", "RAINY")
    var conditionIndex by remember { mutableStateOf(0) }

    val currentCity = if (weatherState.locationName.isNotEmpty()) weatherState.locationName.uppercase() else cities[cityIndex]
    val currentCondition = if (weatherState.locationName.isNotEmpty()) weatherState.condition.uppercase() else conditions[conditionIndex]
    val currentTempRaw = weatherState.tempCelsius
    val currentTemp = if (useFahrenheit) (currentTempRaw * 9 / 5) + 32 else currentTempRaw
    val unitSuffix = if (useFahrenheit) "°F" else "°C"

    val defaultHeight = if (size == "small") 110.dp else 165.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(defaultHeight * heightScale)
            .combinedClickable(
                onClick = { if (!hasPermission) onRequestPermission() else onRefresh() },
                onLongClick = { onLongClick?.invoke() ?: run { showWeatherCustomizer = true } }
            ),
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.LocalCardBackgroundColor.current),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.5.dp, com.example.ui.theme.LocalCardBorderColor.current)
    ) {
        val cardTextColor = com.example.ui.theme.LocalCardTextColor.current
        val cardSecondaryTextColor = com.example.ui.theme.LocalCardSecondaryTextColor.current
        if (!hasPermission) {
            Box(
                modifier = Modifier
                    .padding(if (size == "small") 12.dp else 18.dp)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "WEATHER",
                        color = cardTextColor,
                        fontSize = if (size == "small") 11.sp else 13.sp,
                        fontFamily = com.example.ui.theme.LocalAppFont.current,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "TAP TO ENABLE GPS",
                        color = accentColor,
                        fontSize = if (size == "small") 8.sp else 10.sp,
                        fontFamily = com.example.ui.theme.LocalAppFont.current,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .padding(if (size == "small") 12.dp else 18.dp)
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                if (size != "small") {
                    Text(
                        text = currentCity,
                        color = cardTextColor,
                        fontSize = 14.sp,
                        fontFamily = com.example.ui.theme.LocalAppFont.current,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentCondition,
                        color = cardSecondaryTextColor,
                        fontSize = 11.sp,
                        fontFamily = com.example.ui.theme.LocalAppFont.current,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                // Beautiful dot-matrix digital temp
                DotMatrixText(
                    text = "${currentTemp.toInt()}${unitSuffix}",
                    dotColor = cardTextColor,
                    inactiveDotColor = cardTextColor.copy(alpha = 0.03f),
                    dotSize = if (size == "small") 2.5f else 3.5f,
                    spacing = if (size == "small") 1f else 1.5f
                )
            }

            // Beautiful Weather Art drawn on Canvas (Nothing Style dots)
            Box(
                modifier = Modifier
                    .size(if (size == "small") 48.dp else 76.dp)
                    .clip(CircleShape)
                    .background(cardTextColor.copy(alpha = 0.03f)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(if (size == "small") 36.dp else 60.dp)) {
                    val width = this.size.width
                    val height = this.size.height
                    val center = Offset(width / 2, height / 2)

                    when (currentCondition) {
                        "CLEAR" -> {
                            // Circular Dot Matrix Sun
                            drawCircle(
                                color = cardTextColor,
                                radius = 14.dp.toPx(),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                            // Sunburst rays as dots
                            for (angle in 0 until 360 step 45) {
                                val rad = Math.toRadians(angle.toDouble())
                                val x = center.x + 22.dp.toPx() * cos(rad).toFloat()
                                val y = center.y + 22.dp.toPx() * sin(rad).toFloat()
                                drawCircle(
                                    color = accentColor,
                                    radius = 2.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }
                        }
                        "OVERCAST" -> {
                            // Flat geometric cloud silhouette using dot lines
                            for (x in 12..48 step 6) {
                                drawCircle(
                                    color = cardTextColor,
                                    radius = 6.dp.toPx(),
                                    center = Offset(x.dp.toPx(), 26.dp.toPx())
                                )
                            }
                            for (x in 18..42 step 6) {
                                drawCircle(
                                    color = cardTextColor,
                                    radius = 8.dp.toPx(),
                                    center = Offset(x.dp.toPx(), 22.dp.toPx())
                                )
                            }
                        }
                        "STORMY" -> {
                            // Storm Cloud + lightning strike line
                            drawCircle(
                                color = cardTextColor.copy(alpha = 0.4f),
                                radius = 12.dp.toPx(),
                                center = Offset(center.x, center.y - 4.dp.toPx())
                            )
                            // Red high contrast lightning bolt
                            drawLine(
                                color = accentColor,
                                start = Offset(center.x + 2.dp.toPx(), center.y - 2.dp.toPx()),
                                end = Offset(center.x - 4.dp.toPx(), center.y + 10.dp.toPx()),
                                strokeWidth = 2.dp.toPx()
                            )
                            drawLine(
                                color = accentColor,
                                start = Offset(center.x - 4.dp.toPx(), center.y + 10.dp.toPx()),
                                end = Offset(center.x + 4.dp.toPx(), center.y + 8.dp.toPx()),
                                strokeWidth = 2.dp.toPx()
                            )
                            drawLine(
                                color = accentColor,
                                start = Offset(center.x + 4.dp.toPx(), center.y + 8.dp.toPx()),
                                end = Offset(center.x - 2.dp.toPx(), center.y + 22.dp.toPx()),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                        "RAINY" -> {
                            // Cloud and raindrops as circles
                            drawCircle(
                                color = cardTextColor,
                                radius = 10.dp.toPx(),
                                center = Offset(center.x, center.y - 6.dp.toPx())
                            )
                            // Drops
                            drawCircle(color = cardTextColor, radius = 1.5.dp.toPx(), center = Offset(center.x - 6.dp.toPx(), center.y + 12.dp.toPx()))
                            drawCircle(color = cardTextColor, radius = 1.5.dp.toPx(), center = Offset(center.x, center.y + 16.dp.toPx()))
                            drawCircle(color = cardTextColor, radius = 1.5.dp.toPx(), center = Offset(center.x + 6.dp.toPx(), center.y + 12.dp.toPx()))
                        }
                        else -> {
                            // Default clear
                            drawCircle(
                                color = cardTextColor,
                                radius = 12.dp.toPx(),
                                center = center
                            )
                        }
                    }
                }
            }
            }
        }
    }

    // Weather options dialog
    if (showWeatherCustomizer) {
        AlertDialog(
            onDismissRequest = { showWeatherCustomizer = false },
            title = {
                Text(
                    text = "WEATHER CONFIGURATION",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "SELECT CITY",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        cities.forEachIndexed { index, name ->
                            val isSelected = cityIndex == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (isSelected) accentColor else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .background(
                                        if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { cityIndex = index }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name.take(5),
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = "WEATHER CONDITION",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        conditions.forEachIndexed { index, name ->
                            val isSelected = conditionIndex == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (isSelected) accentColor else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .background(
                                        if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { conditionIndex = index }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "USE FAHRENHEIT (°F)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = useFahrenheit,
                            onCheckedChange = { useFahrenheit = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accentColor,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWeatherCustomizer = false }) {
                    Text("DONE", color = accentColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF161616),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp
        )
    }
}

// --- WIDGET 4: NOTHING QUICK NOTES ---
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NothingNotesWidget(
    size: String = "medium",
    heightScale: Float = 1.0f,
    savedNote: String,
    onSaveNote: (String) -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    var isEditing by remember { mutableStateOf(false) }
    var textState by remember { mutableStateOf(savedNote) }

    LaunchedEffect(savedNote) {
        textState = savedNote
    }

    val defaultHeight = if (size == "small") 110.dp else 165.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(defaultHeight * heightScale)
            .combinedClickable(
                onClick = {},
                onLongClick = { onLongClick?.invoke() }
            ),
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.LocalCardBackgroundColor.current),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.5.dp, com.example.ui.theme.LocalCardBorderColor.current)
    ) {
        val cardTextColor = com.example.ui.theme.LocalCardTextColor.current
        val cardSecondaryTextColor = com.example.ui.theme.LocalCardSecondaryTextColor.current
        Column(
            modifier = Modifier
                .padding(if (size == "small") 12.dp else 18.dp)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QUICK NOTE",
                    color = cardSecondaryTextColor,
                    fontSize = 11.sp,
                    fontFamily = com.example.ui.theme.LocalAppFont.current,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Icon(
                    imageVector = if (isEditing) Icons.Filled.CheckCircle else Icons.Filled.Edit,
                    contentDescription = "Edit Note",
                    tint = if (isEditing) Color(0xFFFF3B30) else cardTextColor,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable {
                            if (isEditing) {
                                onSaveNote(textState)
                                isEditing = false
                            } else {
                                isEditing = true
                            }
                        }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (isEditing) {
                BasicTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    textStyle = TextStyle(
                        color = cardTextColor,
                        fontSize = 14.sp,
                        fontFamily = com.example.ui.theme.LocalAppFont.current
                    ),
                    cursorBrush = SolidColor(Color(0xFFFF3B30)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardTextColor.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .height(60.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        onSaveNote(textState)
                        isEditing = false
                    })
                )
            } else {
                Text(
                    text = if (textState.isBlank()) "Tap the edit icon to add a quick note..." else textState,
                    color = if (textState.isBlank()) cardSecondaryTextColor else cardTextColor,
                    fontSize = 13.sp,
                    fontFamily = com.example.ui.theme.LocalAppFont.current,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp)
                )
            }
        }
    }
}

// --- WIDGET 5: PERFORMANCE STATS WIDGET ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NothingStatsWidget(
    size: String = "medium",
    heightScale: Float = 1.0f,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val initialMemInfo = remember(context) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        val mem = android.app.ActivityManager.MemoryInfo()
        am?.getMemoryInfo(mem)
        mem
    }
    var freeRamGb by remember { mutableStateOf(if (initialMemInfo != null) initialMemInfo.availMem / (1024f * 1024f * 1024f) else 0f) }
    var totalRamGb by remember { mutableStateOf(if (initialMemInfo != null) initialMemInfo.totalMem / (1024f * 1024f * 1024f) else 0f) }
    var uptimeString by remember { mutableStateOf("00:00") }

    var showStatsCustomizer by remember { mutableStateOf(false) }
    var secondaryStatMode by remember { mutableStateOf(0) } // 0 = Uptime, 1 = Storage
    var ramWarningThreshold by remember { mutableStateOf(80) } // 70%, 80%, 90%

    // Storage info calculation
    val storageText = remember {
        try {
            val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
            val totalBytes = stat.totalBytes
            val freeBytes = stat.availableBytes
            val usedBytes = totalBytes - freeBytes
            val totalGb = totalBytes / (1024f * 1024f * 1024f)
            val usedGb = usedBytes / (1024f * 1024f * 1024f)
            String.format(Locale.US, "%.1fGB / %.1fGB", usedGb, totalGb)
        } catch (e: Exception) {
            "N/A"
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            // Read memory info
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            if (activityManager != null) {
                val memInfo = android.app.ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memInfo)
                freeRamGb = memInfo.availMem / (1024f * 1024f * 1024f)
                totalRamGb = memInfo.totalMem / (1024f * 1024f * 1024f)
            }

            // Launcher uptime
            val uptimeSec = SystemClock.elapsedRealtime() / 1000
            val hours = uptimeSec / 3600
            val minutes = (uptimeSec % 3600) / 60
            uptimeString = String.format(Locale.US, "%02d:%02d", hours, minutes)

            delay(3000)
        }
    }

    val usedRamPercent = remember(freeRamGb, totalRamGb) {
        if (totalRamGb > 0) {
            ((totalRamGb - freeRamGb) / totalRamGb * 100).toInt()
        } else {
            0
        }
    }

    val defaultHeight = if (size == "small") 110.dp else 165.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(defaultHeight * heightScale)
            .combinedClickable(
                onClick = { /* Discrete tap is safe */ },
                onLongClick = { onLongClick?.invoke() ?: run { showStatsCustomizer = true } }
            ),
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.LocalCardBackgroundColor.current),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.5.dp, com.example.ui.theme.LocalCardBorderColor.current)
    ) {
        Column(
            modifier = Modifier
                .padding(if (size == "small") 12.dp else 18.dp)
                .fillMaxSize()
        ) {
            if (size != "small") {
                Text(
                    text = "LAUNCHER PERFORMANCE",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RAM USAGE",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "%.1fGB / %.1fGB", totalRamGb - freeRamGb, totalRamGb),
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .size(if (size == "small") 36.dp else 44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$usedRamPercent%",
                        color = if (usedRamPercent > ramWarningThreshold) Color(0xFFFF3B30) else Color.White,
                        fontSize = if (size == "small") 9.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (size != "small") {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (secondaryStatMode == 0) "UPTIME" else "STORAGE",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (secondaryStatMode == 0) uptimeString else storageText,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }

    // Stats options customizer dialog
    if (showStatsCustomizer) {
        AlertDialog(
            onDismissRequest = { showStatsCustomizer = false },
            title = {
                Text(
                    text = "PERFORMANCE CONFIG",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "BOTTOM STATISTIC VIEW",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("LAUNCHER UPTIME", "DEVICE STORAGE").forEachIndexed { index, name ->
                            val isSelected = secondaryStatMode == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFFFF3B30) else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .background(
                                        if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { secondaryStatMode = index }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = "RAM WARNING ALERT LIMIT",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(70, 80, 90).forEach { threshold ->
                            val isSelected = ramWarningThreshold == threshold
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFFFF3B30) else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .background(
                                        if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { ramWarningThreshold = threshold }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "> $threshold%",
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatsCustomizer = false }) {
                    Text("DONE", color = Color(0xFFFF3B30), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF161616),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp
        )
    }
}

// --- WIDGET 6: NOTHING CALENDAR WIDGET ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NothingCalendarWidget(
    size: String = "medium",
    heightScale: Float = 1.0f,
    modifier: Modifier = Modifier,
    hasPermission: Boolean = false,
    onRequestPermission: (() -> Unit)? = null,
    events: List<com.example.ui.CalendarEvent> = emptyList(),
    onLongClick: (() -> Unit)? = null
) {
    val currentTime = remember { Calendar.getInstance() }
    val dayOfMonth = currentTime.get(Calendar.DAY_OF_MONTH).toString()
    val dayOfWeekName = SimpleDateFormat("EEE", Locale.US).format(currentTime.time).uppercase()
    val monthName = SimpleDateFormat("MMM", Locale.US).format(currentTime.time).uppercase()
    val year = currentTime.get(Calendar.YEAR).toString()

    val defaultHeight = if (size == "small") 110.dp else 165.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(defaultHeight * heightScale)
            .combinedClickable(
                onClick = {
                    if (!hasPermission) {
                        onRequestPermission?.invoke()
                    }
                },
                onLongClick = { onLongClick?.invoke() }
            ),
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.LocalCardBackgroundColor.current),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.5.dp, com.example.ui.theme.LocalCardBorderColor.current)
    ) {
        when (size) {
            "small" -> {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(110.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dayOfWeekName,
                        color = Color(0xFFFF3B30),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dayOfMonth,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = monthName,
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            "large" -> {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$monthName $year",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFF3B30))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "TODAY",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        daysOfWeek.forEach { day ->
                            Text(
                                text = day,
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    val todayInt = dayOfMonth.toIntOrNull() ?: 12
                    val startDayOffset = (todayInt % 7)
                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 0 until 7) {
                                val currentDayNum = row * 7 + col + 1 - startDayOffset
                                val isToday = currentDayNum == todayInt
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(if (isToday) Color(0xFFFF3B30) else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (currentDayNum in 1..31) {
                                        Text(
                                            text = currentDayNum.toString(),
                                            color = if (isToday) Color.White else Color.LightGray,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                        )
                                    } else {
                                        Text("", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                Row(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$dayOfWeekName, $monthName $dayOfMonth".uppercase(),
                            color = com.example.ui.theme.LocalCardTextColor.current,
                            fontSize = 14.sp,
                            fontFamily = com.example.ui.theme.LocalAppFont.current,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (!hasPermission) {
                            Text(
                                text = "TAP TO ENABLE CALENDAR",
                                color = Color(0xFFFF3B30),
                                fontSize = 10.sp,
                                fontFamily = com.example.ui.theme.LocalAppFont.current,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            if (events.isNotEmpty()) {
                                events.take(2).forEachIndexed { index, event ->
                                    val color = if (index == 0) com.example.ui.theme.LocalCardSecondaryTextColor.current else Color(0xFFFF3B30)
                                    Text(
                                        text = "${event.startTime} • ${event.title.uppercase()}",
                                        color = color,
                                        fontSize = 10.sp,
                                        fontFamily = com.example.ui.theme.LocalAppFont.current,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                Text(
                                    text = "NO EVENTS TODAY",
                                    color = com.example.ui.theme.LocalCardSecondaryTextColor.current,
                                    fontSize = 10.sp,
                                    fontFamily = com.example.ui.theme.LocalAppFont.current,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = dayOfMonth,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = monthName,
                                color = Color.Gray,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- WIDGET 7: NOTHING MUSIC PLAYER WIDGET ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NothingMusicWidget(
    size: String = "medium",
    heightScale: Float = 1.0f,
    mediaState: MediaState = MediaState(),
    hasPermission: Boolean = false,
    onRequestPermission: () -> Unit = {},
    onTogglePlay: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current
    val cardBg = LocalCardBackgroundColor.current
    val cardText = LocalCardTextColor.current
    val cardSecText = LocalCardSecondaryTextColor.current
    val cardBorderColor = LocalCardBorderColor.current
    val isPlaying = mediaState.isPlaying && hasPermission
    var rotationAngle by remember { mutableStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                rotationAngle = (rotationAngle + 2f) % 360f
                delay(16)
            }
        }
    }

    var playMode by remember { mutableIntStateOf(0) } // 0: Normal, 1: Repeat One, 2: Repeat All, 3: Shuffle
    
    // Synchronize playMode with the active system media player state
    LaunchedEffect(mediaState.repeatMode, mediaState.shuffleMode) {
        val rep = mediaState.repeatMode
        val shuf = mediaState.shuffleMode
        playMode = if (shuf != 0) {
            3
        } else {
            when (rep) {
                1 -> 1 // REPEAT_MODE_ONE
                2 -> 2 // REPEAT_MODE_ALL
                else -> 0
            }
        }
    }
    var currentPosition by remember(mediaState.position, isPlaying) { mutableLongStateOf(mediaState.position) }
    val duration = if (mediaState.duration > 0) mediaState.duration else 180000L // 3 minutes fallback
    
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                delay(500)
                if (mediaState.duration > 0) {
                    com.example.services.NothingMediaListenerService.activeController?.playbackState?.let { pbState ->
                        if (pbState.state == android.media.session.PlaybackState.STATE_PLAYING) {
                            val timeDiff = System.currentTimeMillis() - pbState.lastPositionUpdateTime
                            currentPosition = pbState.position + timeDiff
                        }
                    }
                } else {
                    currentPosition = (currentPosition + 500) % duration
                }
            }
        }
    }

    val defaultHeight = if (size == "small") 110.dp else 165.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(defaultHeight * heightScale)
            .combinedClickable(
                onClick = { if (!hasPermission) onRequestPermission() },
                onLongClick = { onLongClick?.invoke() }
            ),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.5.dp, cardBorderColor)
    ) {
        if (!hasPermission) {
            // Permission Prompt Layout (Nothing OS style)
            Column(
                modifier = Modifier
                    .padding(if (size == "small") 10.dp else 18.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "MEDIA PLAYER",
                    color = cardSecText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ACCESS RESTRICTED",
                    color = cardText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                if (size != "small") {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "TAP TO ENABLE CONTROLS",
                        color = Color(0xFFFF3B30),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        } else {
            // Media Player Layout
            when (size) {
                "small" -> {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(cardText.copy(alpha = 0.05f))
                                .border(1.dp, cardBorderColor, CircleShape)
                                .rotate(rotationAngle),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(cardBg, radius = this.size.minDimension / 2.5f)
                                drawCircle(cardSecText.copy(alpha = 0.3f), radius = this.size.minDimension / 3.5f, style = Stroke(width = 1f))
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        IconButton(
                            onClick = { onTogglePlay() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = cardText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                "large" -> {
                    Column(
                        modifier = Modifier
                            .padding(18.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "NOW PLAYING",
                            color = cardSecText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardText.copy(alpha = 0.03f))
                                .border(1.dp, cardBorderColor, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(0.85f),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(cardText.copy(alpha = 0.08f))
                                        .rotate(rotationAngle)
                                        .border(1.5.dp, cardBorderColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(modifier = Modifier.size(14.dp, 2.dp).background(cardText.copy(alpha = 0.4f)))
                                    Box(modifier = Modifier.size(2.dp, 14.dp).background(cardText.copy(alpha = 0.4f)))
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = mediaState.packageName.substringAfterLast(".").uppercase(),
                                        color = accentColor,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text("SIDE A", color = cardSecText, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(cardText.copy(alpha = 0.08f))
                                        .rotate(rotationAngle)
                                        .border(1.5.dp, cardBorderColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(modifier = Modifier.size(14.dp, 2.dp).background(cardText.copy(alpha = 0.4f)))
                                    Box(modifier = Modifier.size(2.dp, 14.dp).background(cardText.copy(alpha = 0.4f)))
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = mediaState.title,
                                        color = cardText,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            playMode = (playMode + 1) % 4
                                            try {
                                                when (playMode) {
                                                    0 -> {
                                                        com.example.services.NothingMediaListenerService.setRepeatMode(com.example.services.NothingMediaListenerService.REPEAT_MODE_NONE)
                                                        com.example.services.NothingMediaListenerService.setShuffleMode(com.example.services.NothingMediaListenerService.SHUFFLE_MODE_NONE)
                                                    }
                                                    1 -> {
                                                        com.example.services.NothingMediaListenerService.setRepeatMode(com.example.services.NothingMediaListenerService.REPEAT_MODE_ONE)
                                                    }
                                                    2 -> {
                                                        com.example.services.NothingMediaListenerService.setRepeatMode(com.example.services.NothingMediaListenerService.REPEAT_MODE_ALL)
                                                    }
                                                    3 -> {
                                                        com.example.services.NothingMediaListenerService.setShuffleMode(com.example.services.NothingMediaListenerService.SHUFFLE_MODE_ALL)
                                                    }
                                                }
                                            } catch (e: Exception) {}
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = when (playMode) {
                                                1 -> Icons.Filled.RepeatOne
                                                2 -> Icons.Filled.Repeat
                                                3 -> Icons.Filled.Shuffle
                                                else -> Icons.Filled.Repeat
                                            },
                                            contentDescription = "Playback Mode",
                                            tint = if (playMode == 0) cardSecText else accentColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = mediaState.artist,
                                    color = cardSecText,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onPrevious() }) {
                                    Icon(Icons.Filled.SkipPrevious, null, tint = cardText)
                                }
                                IconButton(
                                    onClick = { onTogglePlay() },
                                    modifier = Modifier
                                        .background(cardText, CircleShape)
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = cardBg,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(onClick = { onNext() }) {
                                    Icon(Icons.Filled.SkipNext, null, tint = cardText)
                                }
                            }
                        }
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier
                            .padding(18.dp)
                            .fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = mediaState.title,
                                    color = cardText,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        playMode = (playMode + 1) % 4
                                        try {
                                            when (playMode) {
                                                0 -> {
                                                    com.example.services.NothingMediaListenerService.setRepeatMode(com.example.services.NothingMediaListenerService.REPEAT_MODE_NONE)
                                                    com.example.services.NothingMediaListenerService.setShuffleMode(com.example.services.NothingMediaListenerService.SHUFFLE_MODE_NONE)
                                                }
                                                1 -> {
                                                    com.example.services.NothingMediaListenerService.setRepeatMode(com.example.services.NothingMediaListenerService.REPEAT_MODE_ONE)
                                                }
                                                2 -> {
                                                    com.example.services.NothingMediaListenerService.setRepeatMode(com.example.services.NothingMediaListenerService.REPEAT_MODE_ALL)
                                                }
                                                3 -> {
                                                    com.example.services.NothingMediaListenerService.setShuffleMode(com.example.services.NothingMediaListenerService.SHUFFLE_MODE_ALL)
                                                }
                                            }
                                        } catch (e: Exception) {}
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = when (playMode) {
                                            1 -> Icons.Filled.RepeatOne
                                            2 -> Icons.Filled.Repeat
                                            3 -> Icons.Filled.Shuffle
                                            else -> Icons.Filled.Repeat
                                        },
                                        contentDescription = "Playback Mode",
                                        tint = if (playMode == 0) cardSecText else accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Text(
                                text = mediaState.artist,
                                color = cardSecText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Interactive/Functional Seekable Progress Bar
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .height(18.dp)
                                    .pointerInput(duration) {
                                        detectTapGestures { offset ->
                                            val fraction = (offset.x / this.size.width.toFloat()).coerceIn(0f, 1f)
                                            val newPos = (fraction * duration).toLong()
                                            currentPosition = newPos
                                            try {
                                                com.example.services.NothingMediaListenerService.activeController?.transportControls?.seekTo(newPos)
                                            } catch (e: Exception) {}
                                        }
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val widthPx = constraints.maxWidth.toFloat()
                                val fraction = (currentPosition.toFloat() / duration).coerceIn(0f, 1f)
                                
                                // Background Track
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(cardText.copy(alpha = 0.15f))
                                )
                                
                                // Foreground progress
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(accentColor)
                                )
                                
                                // Mini knob (Nothing style small dot)
                                val knobOffset = with(LocalDensity.current) { (fraction * widthPx).toDp() }
                                Box(
                                    modifier = Modifier
                                        .offset(x = knobOffset - 4.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { onPrevious() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Filled.SkipPrevious, null, tint = cardText, modifier = Modifier.size(20.dp))
                            }
                            IconButton(
                                onClick = { onTogglePlay() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(cardText.copy(alpha = 0.05f), CircleShape)
                                    .border(1.dp, cardBorderColor, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = cardText,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(
                                onClick = { onNext() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Filled.SkipNext, null, tint = cardText, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- WIDGET 8: NOTHING FITNESS STEP TRACKER ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NothingFitnessWidget(
    size: String = "medium",
    heightScale: Float = 1.0f,
    stepCount: Int = 0,
    hasPermission: Boolean = true,
    onRequestPermission: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current
    val targetSteps = 10000
    val distanceKm = remember(stepCount) { String.format(Locale.US, "%.2f km", stepCount * 0.00075) }
    val caloriesKcal = remember(stepCount) { "${(stepCount * 0.04).toInt()} kcal" }
    val progressPercent = remember(stepCount) { (stepCount.toFloat() / targetSteps).coerceIn(0f, 1f) }

    val defaultHeight = if (size == "small") 110.dp else 165.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(defaultHeight * heightScale)
            .combinedClickable(
                onClick = { if (!hasPermission) onRequestPermission() },
                onLongClick = { onLongClick?.invoke() }
            ),
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.LocalCardBackgroundColor.current),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.5.dp, com.example.ui.theme.LocalCardBorderColor.current)
    ) {
        val cardTextColor = com.example.ui.theme.LocalCardTextColor.current
        val cardSecondaryTextColor = com.example.ui.theme.LocalCardSecondaryTextColor.current
        if (!hasPermission) {
            Box(
                modifier = Modifier
                    .padding(if (size == "small") 12.dp else 18.dp)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "STEP TRACKER",
                        color = cardTextColor,
                        fontSize = if (size == "small") 11.sp else 13.sp,
                        fontFamily = com.example.ui.theme.LocalAppFont.current,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "TAP TO ENABLE TRACKING",
                        color = accentColor,
                        fontSize = if (size == "small") 8.sp else 10.sp,
                        fontFamily = com.example.ui.theme.LocalAppFont.current,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            when (size) {
            "small" -> {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Steps",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${String.format(Locale.US, "%.1fk", stepCount / 1000f)}",
                        color = cardTextColor,
                        fontSize = 18.sp,
                        fontFamily = com.example.ui.theme.LocalAppFont.current,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "STEPS",
                        color = cardSecondaryTextColor,
                        fontSize = 8.sp,
                        fontFamily = com.example.ui.theme.LocalAppFont.current,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            "large" -> {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "FITNESS ACTIVITY",
                        color = cardSecondaryTextColor,
                        fontSize = 10.sp,
                        fontFamily = com.example.ui.theme.LocalAppFont.current,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$stepCount / $targetSteps",
                                color = cardTextColor,
                                fontSize = 22.sp,
                                fontFamily = com.example.ui.theme.LocalAppFont.current,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "DAILY STEPS",
                                color = cardSecondaryTextColor,
                                fontSize = 11.sp,
                                fontFamily = com.example.ui.theme.LocalAppFont.current
                            )
                        }

                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = cardTextColor.copy(alpha = 0.05f),
                                    style = Stroke(width = 6f)
                                )
                                drawArc(
                                    color = accentColor,
                                    startAngle = -90f,
                                    sweepAngle = progressPercent * 360f,
                                    useCenter = false,
                                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                                )
                            }
                            Text(
                                text = "${(progressPercent * 100).toInt()}%",
                                color = cardTextColor,
                                fontSize = 10.sp,
                                fontFamily = com.example.ui.theme.LocalAppFont.current,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = distanceKm, color = cardTextColor, fontSize = 12.sp, fontFamily = com.example.ui.theme.LocalAppFont.current, fontWeight = FontWeight.Bold)
                            Text(text = "DISTANCE", color = cardSecondaryTextColor, fontSize = 9.sp, fontFamily = com.example.ui.theme.LocalAppFont.current)
                        }
                        Column {
                            Text(text = caloriesKcal, color = cardTextColor, fontSize = 12.sp, fontFamily = com.example.ui.theme.LocalAppFont.current, fontWeight = FontWeight.Bold)
                            Text(text = "CALORIES", color = cardSecondaryTextColor, fontSize = 9.sp, fontFamily = com.example.ui.theme.LocalAppFont.current)
                        }
                        Column {
                            Text(text = "48 min", color = cardTextColor, fontSize = 12.sp, fontFamily = com.example.ui.theme.LocalAppFont.current, fontWeight = FontWeight.Bold)
                            Text(text = "ACTIVE", color = cardSecondaryTextColor, fontSize = 9.sp, fontFamily = com.example.ui.theme.LocalAppFont.current)
                        }
                    }
                }
            }
            else -> {
                Row(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$stepCount STEPS",
                            color = cardTextColor,
                            fontSize = 16.sp,
                            fontFamily = com.example.ui.theme.LocalAppFont.current,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$distanceKm • $caloriesKcal",
                            color = cardSecondaryTextColor,
                            fontSize = 11.sp,
                            fontFamily = com.example.ui.theme.LocalAppFont.current
                        )
                    }

                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = cardTextColor.copy(alpha = 0.05f),
                                style = Stroke(width = 5f)
                            )
                            drawArc(
                                color = accentColor,
                                startAngle = -90f,
                                sweepAngle = progressPercent * 360f,
                                useCenter = false,
                                style = Stroke(width = 5f, cap = StrokeCap.Round)
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = cardTextColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
        }
    }
}

// --- WIDGET 9: NOTHING TO-DO ---
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NothingTodoWidget(
    size: String = "medium",
    heightScale: Float = 1.0f,
    todoItems: List<com.example.data.TodoEntity>,
    onAddTodo: (String) -> Unit,
    onToggleTodo: (com.example.data.TodoEntity) -> Unit,
    onDeleteTodo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    var isAdding by remember { mutableStateOf(false) }
    var todoText by remember { mutableStateOf("") }
    
    val defaultHeight = if (size == "small") 110.dp else if (size == "large") 240.dp else 170.dp
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(defaultHeight * heightScale)
            .combinedClickable(
                onClick = {},
                onLongClick = { onLongClick?.invoke() }
            ),
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.LocalCardBackgroundColor.current),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.5.dp, com.example.ui.theme.LocalCardBorderColor.current)
    ) {
        val cardTextColor = com.example.ui.theme.LocalCardTextColor.current
        val cardSecondaryTextColor = com.example.ui.theme.LocalCardSecondaryTextColor.current
        val accentColor = com.example.ui.theme.LocalAccentColor.current
        val appFont = com.example.ui.theme.LocalAppFont.current
        
        Column(
            modifier = Modifier
                .padding(if (size == "small") 12.dp else 18.dp)
                .fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TO-DO LIST",
                    color = cardSecondaryTextColor,
                    fontSize = 11.sp,
                    fontFamily = appFont,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isAdding) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Save Todo",
                            tint = accentColor,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    if (todoText.isNotBlank()) {
                                        onAddTodo(todoText.trim())
                                        todoText = ""
                                    }
                                    isAdding = false
                                }
                        )
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cancel",
                            tint = cardTextColor.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    todoText = ""
                                    isAdding = false
                                }
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Todo",
                            tint = cardTextColor,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    isAdding = true
                                }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isAdding) {
                // Quick add item text input
                BasicTextField(
                    value = todoText,
                    onValueChange = { todoText = it },
                    textStyle = TextStyle(
                        color = cardTextColor,
                        fontSize = 13.sp,
                        fontFamily = appFont
                    ),
                    cursorBrush = SolidColor(accentColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardTextColor.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (todoText.isNotBlank()) {
                                onAddTodo(todoText.trim())
                                todoText = ""
                            }
                            isAdding = false
                        }
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (todoItems.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NO TASKS",
                        color = cardSecondaryTextColor.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontFamily = appFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(todoItems) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(cardTextColor.copy(alpha = 0.02f))
                                .clickable { onToggleTodo(item) }
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Custom Checkbox
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .border(
                                            1.5.dp,
                                            if (item.isCompleted) accentColor else cardTextColor.copy(alpha = 0.4f),
                                            CircleShape
                                        )
                                        .background(
                                            if (item.isCompleted) accentColor.copy(alpha = 0.15f) else Color.Transparent,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (item.isCompleted) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(accentColor, CircleShape)
                                        )
                                    }
                                }
                                
                                Text(
                                    text = item.title,
                                    color = if (item.isCompleted) cardTextColor.copy(alpha = 0.4f) else cardTextColor,
                                    fontSize = 12.sp,
                                    fontFamily = appFont,
                                    textDecoration = if (item.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete Todo",
                                tint = cardSecondaryTextColor.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onDeleteTodo(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

