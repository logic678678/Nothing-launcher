package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import java.util.concurrent.ConcurrentHashMap

object AppIconProcessor {
    private val iconCache = ConcurrentHashMap<String, Bitmap>()

    fun getAppIcon(context: Context, packageName: String): Bitmap? {
        iconCache[packageName]?.let { return it }

        try {
            val pm = context.packageManager
            val iconDrawable = pm.getApplicationIcon(packageName)
            val bitmap = drawableToBitmap(iconDrawable)
            if (bitmap != null) {
                iconCache[packageName] = bitmap
                return bitmap
            }
        } catch (e: Exception) {
            Log.e("AppIconProcessor", "Error loading icon for $packageName", e)
        }
        return null
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 150
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 150
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun clearCache() {
        iconCache.clear()
    }
}

@Composable
fun NothingAppIcon(
    packageName: String,
    label: String,
    isMonochrome: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    context: Context
) {
    val bitmap = remember(packageName) {
        AppIconProcessor.getAppIcon(context, packageName)
    }

    // Beautiful filled round background plate for all icons
    val circularBackground = Color(0xFF222222)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(circularBackground),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
            if (isMonochrome) {
                // Apply a monochrome greyscale matrix with boosted contrast
                val monochromeMatrix = remember {
                    ColorMatrix(
                        floatArrayOf(
                            1.5f, 1.5f, 1.5f, 0f, -120f, // Red channel high contrast
                            1.5f, 1.5f, 1.5f, 0f, -120f, // Green
                            1.5f, 1.5f, 1.5f, 0f, -120f, // Blue
                            0f,   0f,   0f,   1f, 0f       // Alpha
                        )
                    )
                }
                Image(
                    bitmap = imageBitmap,
                    contentDescription = label,
                    modifier = Modifier
                        .fillMaxSize(0.62f)
                        .clip(CircleShape),
                    colorFilter = ColorFilter.colorMatrix(monochromeMatrix)
                )
            } else {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = label,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
        } else {
            // Draw custom 2-letter Nothing style dot placeholder
            val text = remember(label) {
                if (label.length >= 2) {
                    label.substring(0, 2).uppercase()
                } else if (label.isNotEmpty()) {
                    label.substring(0, 1).uppercase()
                } else {
                    "?"
                }
            }

            Text(
                text = text,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = (size.value * 0.35f).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}
