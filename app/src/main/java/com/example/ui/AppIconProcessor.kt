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

import android.content.Intent
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStreamReader
import com.example.ui.theme.LocalAccentColor

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height

data class IconPackInfo(
    val packageName: String,
    val name: String
)

object AppIconProcessor {
    private val iconCache = ConcurrentHashMap<String, Bitmap>()
    val fgIconCache = ConcurrentHashMap<String, Bitmap>()
    val adaptiveAppsMap = ConcurrentHashMap<String, Boolean>()
    private var cachedPackName: String? = null
    private val appFilterMap = ConcurrentHashMap<String, String>() // packageName -> drawableName

    fun getCachedIcon(packageName: String): Bitmap? {
        return iconCache[packageName]
    }

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

    fun getAppIconForeground(context: Context, packageName: String): Bitmap? {
        fgIconCache[packageName]?.let { return it }

        try {
            val pm = context.packageManager
            val iconDrawable = pm.getApplicationIcon(packageName)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && 
                iconDrawable is android.graphics.drawable.AdaptiveIconDrawable) {
                val fgDrawable = iconDrawable.foreground
                val bitmap = drawableToBitmap(fgDrawable)
                if (bitmap != null) {
                    fgIconCache[packageName] = bitmap
                    return bitmap
                }
            } else {
                val bitmap = drawableToBitmap(iconDrawable)
                if (bitmap != null) {
                    fgIconCache[packageName] = bitmap
                    return bitmap
                }
            }
        } catch (e: Exception) {
            Log.e("AppIconProcessor", "Error loading foreground icon for $packageName", e)
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
        cachedPackName = null
        appFilterMap.clear()
    }

    fun getInstalledIconPacks(context: Context): List<IconPackInfo> {
        val pm = context.packageManager
        val iconPacks = mutableListOf<IconPackInfo>()
        val intents = listOf(
            Intent("com.novalauncher.THEME"),
            Intent("org.adw.launcher.THEMES.action.SINGLET_ICON_THEME"),
            Intent("com.anddoes.launcher.THEME"),
            Intent("com.teslacoilsw.launcher.THEME")
        )
        val packagesSet = mutableSetOf<String>()
        for (intent in intents) {
            try {
                val resolveInfos = pm.queryIntentActivities(intent, 0)
                for (ri in resolveInfos) {
                    val packageName = ri.activityInfo.packageName
                    if (!packagesSet.contains(packageName)) {
                        packagesSet.add(packageName)
                        val label = ri.loadLabel(pm).toString()
                        iconPacks.add(IconPackInfo(packageName, label))
                    }
                }
            } catch (e: Exception) {
                Log.e("AppIconProcessor", "Error querying icon packs for intent ${intent.action}", e)
            }
        }
        return iconPacks
    }

    private fun loadAppFilter(context: Context, packPackageName: String) {
        if (cachedPackName == packPackageName) return
        appFilterMap.clear()
        cachedPackName = packPackageName

        try {
            val packContext = context.createPackageContext(packPackageName, Context.CONTEXT_IGNORE_SECURITY)
            val assets = packContext.assets
            var inputStream = try {
                assets.open("appfilter.xml")
            } catch (e: Exception) {
                null
            }

            if (inputStream == null) {
                val resId = packContext.resources.getIdentifier("appfilter", "xml", packPackageName)
                if (resId != 0) {
                    inputStream = packContext.resources.openRawResource(resId)
                }
            }

            if (inputStream != null) {
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = true
                val parser = factory.newPullParser()
                parser.setInput(InputStreamReader(inputStream))

                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        val tagName = parser.name
                        if (tagName == "item") {
                            val component = parser.getAttributeValue(null, "component")
                            val drawable = parser.getAttributeValue(null, "drawable")
                            if (component != null && drawable != null) {
                                val startIdx = component.indexOf("{")
                                val endIdx = component.indexOf("/")
                                if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                                    val pkg = component.substring(startIdx + 1, endIdx)
                                    appFilterMap[pkg] = drawable
                                } else if (!component.contains("{") && component.contains("/")) {
                                    val end = component.indexOf("/")
                                    val pkg = component.substring(0, end)
                                    appFilterMap[pkg] = drawable
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }
                inputStream.close()
                Log.d("AppIconProcessor", "Parsed appfilter.xml. Found ${appFilterMap.size} mappings.")
            }
        } catch (e: Exception) {
            Log.e("AppIconProcessor", "Error loading appfilter.xml for $packPackageName", e)
        }
    }

    fun getIconFromPack(context: Context, packPackageName: String, appPackageName: String): Bitmap? {
        try {
            loadAppFilter(context, packPackageName)
            val drawableName = appFilterMap[appPackageName] ?: return null
            val packContext = context.createPackageContext(packPackageName, Context.CONTEXT_IGNORE_SECURITY)
            val resId = packContext.resources.getIdentifier(drawableName, "drawable", packPackageName)
            if (resId != 0) {
                val drawable = packContext.resources.getDrawable(resId, null)
                return drawableToBitmap(drawable)
            }
        } catch (e: Exception) {
            Log.e("AppIconProcessor", "Error loading icon from pack $packPackageName for $appPackageName", e)
        }
        return null
    }
}

@Composable
fun NothingAppIcon(
    packageName: String,
    label: String,
    isMonochrome: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    context: Context,
    settingsMap: Map<String, String> = emptyMap()
) {
    val iconPackType = settingsMap["icon_pack_type"] ?: "default"
    val accentColor = LocalAccentColor.current

    if (iconPackType == "adaptive") {
        val bgSetting = settingsMap["adaptive_icon_bg"] ?: "white"
        val fgSetting = settingsMap["adaptive_icon_fg"] ?: "black"

        val bgColor = when (bgSetting) {
            "white" -> Color.White
            "black" -> Color.Black
            "accent" -> accentColor
            "custom_grey" -> Color(0xFF222222)
            "none" -> Color.Transparent
            else -> Color.White
        }

        val fgColor = when (fgSetting) {
            "black" -> Color.Black
            "white" -> Color.White
            "accent" -> accentColor
            else -> Color.Black
        }

        // Asynchronously load the icon so it NEVER blocks the main/UI thread!
        var bitmapState by remember(packageName) { 
            mutableStateOf<Bitmap?>(if (AppIconProcessor.adaptiveAppsMap[packageName] != false) AppIconProcessor.fgIconCache[packageName] else AppIconProcessor.getCachedIcon(packageName)) 
        }
        var isAdaptiveState by remember(packageName) {
            mutableStateOf(AppIconProcessor.adaptiveAppsMap[packageName] ?: true)
        }
        
        LaunchedEffect(packageName) {
            withContext(Dispatchers.IO) {
                try {
                    val pm = context.packageManager
                    val iconDrawable = pm.getApplicationIcon(packageName)
                    val isAdaptive = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && 
                            iconDrawable is android.graphics.drawable.AdaptiveIconDrawable
                    isAdaptiveState = isAdaptive
                    AppIconProcessor.adaptiveAppsMap[packageName] = isAdaptive
                    
                    if (isAdaptive) {
                        val bmp = AppIconProcessor.getAppIconForeground(context, packageName)
                        bitmapState = bmp
                    } else {
                        val bmp = AppIconProcessor.getAppIcon(context, packageName)
                        bitmapState = bmp
                    }
                } catch (e: Exception) {
                    Log.e("NothingAppIcon", "Error loading adaptive/fallback icon", e)
                }
            }
        }

        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .then(
                    if (bgColor != Color.Transparent) {
                        Modifier.background(bgColor)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            val currentBitmap = bitmapState
            if (currentBitmap != null) {
                val imageBitmap = remember(currentBitmap) { currentBitmap.asImageBitmap() }
                
                if (isAdaptiveState) {
                    // Foreground detailed silhouette layer
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = label,
                        modifier = Modifier.fillMaxSize(if (bgColor != Color.Transparent) 0.60f else 1.0f),
                        colorFilter = ColorFilter.tint(fgColor)
                    )
                } else {
                    // Fallback for non-adaptive app icons (preserves textures and gradients)
                    if (isMonochrome) {
                        val monochromeMatrix = remember {
                            ColorMatrix(
                                floatArrayOf(
                                    1.5f, 1.5f, 1.5f, 0f, -120f,
                                    1.5f, 1.5f, 1.5f, 0f, -120f,
                                    1.5f, 1.5f, 1.5f, 0f, -120f,
                                    0f,   0f,   0f,   1f, 0f
                                )
                            )
                        }
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = label,
                            modifier = Modifier
                                .fillMaxSize(0.60f)
                                .clip(CircleShape),
                            colorFilter = ColorFilter.colorMatrix(monochromeMatrix)
                        )
                    } else {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = label,
                            modifier = Modifier
                                .fillMaxSize(0.60f)
                                .clip(CircleShape)
                        )
                    }
                }
            } else {
                val text = remember(label) {
                    if (label.length >= 2) label.substring(0, 2).uppercase()
                    else if (label.isNotEmpty()) label.substring(0, 1).uppercase()
                    else "?"
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(if (bgColor == Color.Transparent) Color(0xFF222222) else bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        color = fgColor,
                        fontSize = (size.value * 0.35f).sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    } else if (iconPackType == "custom") {
        val activeCustomPack = settingsMap["active_custom_icon_pack"] ?: ""
        
        // Asynchronously load custom icon to prevent drawer lag!
        var customIconState by remember(packageName, activeCustomPack) { 
            mutableStateOf<Bitmap?>(null) 
        }
        var hasAttemptedLoad by remember(packageName, activeCustomPack) { mutableStateOf(false) }

        LaunchedEffect(packageName, activeCustomPack) {
            if (activeCustomPack.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    val bmp = AppIconProcessor.getIconFromPack(context, activeCustomPack, packageName)
                    customIconState = bmp
                    hasAttemptedLoad = true
                }
            } else {
                hasAttemptedLoad = true
            }
        }

        val currentCustomBitmap = customIconState
        if (currentCustomBitmap != null) {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val imageBitmap = remember(currentCustomBitmap) { currentCustomBitmap.asImageBitmap() }
                Image(
                    bitmap = imageBitmap,
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else if (hasAttemptedLoad) {
            StandardNothingAppIcon(
                packageName = packageName,
                label = label,
                isMonochrome = isMonochrome,
                modifier = modifier,
                size = size,
                context = context
            )
        } else {
            // Placeholder while loading
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color(0xFF222222))
            )
        }
    } else {
        StandardNothingAppIcon(
            packageName = packageName,
            label = label,
            isMonochrome = isMonochrome,
            modifier = modifier,
            size = size,
            context = context
        )
    }
}

@Composable
fun StandardNothingAppIcon(
    packageName: String,
    label: String,
    isMonochrome: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    context: Context
) {
    // Asynchronously load the standard icon to avoid any scroll lag!
    var bitmapState by remember(packageName) { 
        mutableStateOf<Bitmap?>(AppIconProcessor.getCachedIcon(packageName)) 
    }
    
    LaunchedEffect(packageName) {
        if (bitmapState == null) {
            withContext(Dispatchers.IO) {
                val bmp = AppIconProcessor.getAppIcon(context, packageName)
                bitmapState = bmp
            }
        }
    }

    val circularBackground = Color(0xFF222222)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(circularBackground),
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap = bitmapState
        if (currentBitmap != null) {
            val imageBitmap = remember(currentBitmap) { currentBitmap.asImageBitmap() }
            if (isMonochrome) {
                val monochromeMatrix = remember {
                    ColorMatrix(
                        floatArrayOf(
                            1.5f, 1.5f, 1.5f, 0f, -120f,
                            1.5f, 1.5f, 1.5f, 0f, -120f,
                            1.5f, 1.5f, 1.5f, 0f, -120f,
                            0f,   0f,   0f,   1f, 0f
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
