package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import kotlin.math.roundToInt
import com.example.ui.AppModel
import com.example.ui.LauncherViewModel
import com.example.ui.NothingAppIcon
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalAppFont
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerScreen(
    viewModel: LauncherViewModel,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val filteredApps by viewModel.filteredApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val pinnedApps by viewModel.pinnedApps.collectAsState()
    val settingsMap by viewModel.settingsMap.collectAsState()

    val accentColor = LocalAccentColor.current
    val appFont = LocalAppFont.current
    val isMonochrome = (settingsMap["monochrome_icons"] ?: "true") == "true"
    val vibrateEnabled = (settingsMap["vibrate_on_scroll"] ?: "true") == "true"

    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    var longPressedApp by remember { mutableStateOf<AppModel?>(null) }
    var isDialogVisible by remember { mutableStateOf(false) }

    var activeLetter by remember { mutableStateOf<String?>(null) }
    var sidebarHeight by remember { mutableStateOf(1) }
    var activeLetterTouchY by remember { mutableStateOf<Float?>(null) }
    var scrollJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val view = LocalView.current
    val density = LocalDensity.current

    // List of letters for the quick scroller (continuous Niagara-style index)
    val alphabets = remember {
        listOf("☆", "#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z")
    }

    // Categories
    val categories = listOf("ALL", "SYSTEM", "SOCIAL", "MEDIA", "UTILITY", "GAMES")

    val themeMode = settingsMap["theme_mode"] ?: "oled"
    val themeSelector = settingsMap["theme_mode_selector"] ?: "dark"
    val darkTheme = when (themeSelector) {
        "light" -> false
        "auto" -> androidx.compose.foundation.isSystemInDarkTheme()
        else -> true
    }

    val backgroundColor = if (darkTheme) {
        if (themeMode == "oled") Color(0xFF050505) else Color(0xFF121212)
    } else {
        Color(0xFFF6F6F6)
    }

    val textColor = if (darkTheme) Color.White else Color.Black
    val secondaryTextColor = if (darkTheme) Color.Gray else Color(0xFF555555)
    val searchBarBg = if (darkTheme) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.03f)
    val searchBarBorder = if (darkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.1f)
    val searchPlaceholderColor = if (darkTheme) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.4f)
    val closeIconTint = if (darkTheme) Color.White else Color.Black

    // Precomputed O(1) map of alphabet chars to the first matching app index for buttery-smooth scrolling
    val letterIndexMap = remember(filteredApps) {
        val map = mutableMapOf<String, Int>()
        if (filteredApps.isNotEmpty()) {
            map["☆"] = 0
            
            val alphabetChars = ('A'..'Z').toList()
            for (char in alphabetChars) {
                val idx = filteredApps.indexOfFirst { app ->
                    val firstChar = app.label.firstOrNull()?.uppercaseChar() ?: ' '
                    firstChar.isLetter() && firstChar >= char
                }
                if (idx != -1) {
                    map[char.toString()] = idx
                } else {
                    val lastLetterIdx = filteredApps.indexOfLast { it.label.firstOrNull()?.isLetter() == true }
                    map[char.toString()] = if (lastLetterIdx != -1) lastLetterIdx else 0
                }
            }
            
            val nonLetterIdx = filteredApps.indexOfFirst { !(it.label.firstOrNull()?.isLetter() ?: true) }
            map["#"] = if (nonLetterIdx != -1) nonLetterIdx else 0
        }
        map
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(56.dp))

        // --- 1. Nothing Dot-Matrix Search Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(searchBarBg, RoundedCornerShape(20.dp))
                .border(1.dp, searchBarBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
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
                        text = "SEARCH APPLICATIONS...",
                        color = searchPlaceholderColor,
                        fontSize = 12.sp,
                        fontFamily = appFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    textStyle = TextStyle(
                        color = textColor,
                        fontSize = 13.sp,
                        fontFamily = appFont,
                        letterSpacing = 0.5.sp
                    ),
                    cursorBrush = SolidColor(accentColor),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { /* dismiss keyboard */ })
                )
            }
            if (searchQuery.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Clear",
                    tint = closeIconTint,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { viewModel.setSearchQuery("") }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 2. Category Selectors ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .border(
                        1.dp, 
                        if (darkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f), 
                        RoundedCornerShape(12.dp)
                    )
            ) {
                // We show an horizontal scrollable selection
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(categories.size) { index ->
                        val cat = categories[index]
                        val isSelected = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) textColor else Color.Transparent)
                                .clickable {
                                    viewModel.setSelectedCategory(cat)
                                    // Reset grid scroll
                                    coroutineScope.launch { gridState.scrollToItem(0) }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) { if (darkTheme) Color.Black else Color.White } else secondaryTextColor,
                                fontSize = 10.sp,
                                fontFamily = appFont,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. App Grid/List with Side Alphabet Fast Scroller ---
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val isListView = (settingsMap["drawer_list_view"] ?: "false") == "true"
            
            // Main Grid/List
            if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NO MATCHING APPS",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontFamily = appFont,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try refining your query...",
                            color = secondaryTextColor,
                            fontSize = 11.sp,
                            fontFamily = appFont
                        )
                    }
                }
            } else if (!isListView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    state = gridState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        count = filteredApps.size,
                        key = { index -> "${filteredApps[index].packageName}/${filteredApps[index].activityName}" }
                    ) { index ->
                        val app = filteredApps[index]
                        val pinnedPackageNames = remember(pinnedApps) { pinnedApps.map { it.packageName }.toSet() }
                        val isPinned = pinnedPackageNames.contains(app.packageName)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { viewModel.launchApp(context, app) },
                                    onLongClick = {
                                        longPressedApp = app
                                        isDialogVisible = true
                                    }
                                )
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                NothingAppIcon(
                                    packageName = app.packageName,
                                    label = app.label,
                                    isMonochrome = isMonochrome,
                                    context = context,
                                    size = 54.dp,
                                    settingsMap = settingsMap
                                )
                                if (isPinned) {
                                    Box(
                                        modifier = Modifier
                                            .offset(x = 2.dp, y = (-2).dp)
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(accentColor)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = app.label,
                                color = textColor,
                                fontSize = 11.sp,
                                fontFamily = appFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                // List View Implementation
                androidx.compose.foundation.lazy.LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredApps.size) { index ->
                        val app = filteredApps[index]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { viewModel.launchApp(context, app) },
                                    onLongClick = {
                                        longPressedApp = app
                                        isDialogVisible = true
                                    }
                                )
                                .padding(vertical = 8.dp)
                        ) {
                            NothingAppIcon(
                                packageName = app.packageName,
                                label = app.label,
                                isMonochrome = isMonochrome,
                                context = context,
                                size = 40.dp,
                                settingsMap = settingsMap
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = app.label,
                                color = textColor,
                                fontSize = 16.sp,
                                fontFamily = appFont,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Side Alphabet Fast Scroller (Niagara-style gesture based)
            if (alphabets.isNotEmpty() && filteredApps.size > 12) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = 12.dp, bottom = 90.dp, end = 4.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    // Floating Letter Popover (Bubble) - Niagara style
                    activeLetter?.let { letter ->
                        val letterIndex = alphabets.indexOf(letter)
                        if (letterIndex != -1 && sidebarHeight > 1) {
                            val progress = letterIndex.toFloat() / (alphabets.size - 1)
                            val bubbleCenterYDp = with(density) { (progress * sidebarHeight).toDp() }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(
                                        x = (-50).dp,
                                        y = (bubbleCenterYDp - 28.dp).coerceIn(0.dp, with(density) { sidebarHeight.toFloat().toDp() } - 56.dp)
                                    )
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                                    .border(2.dp, if (accentColor == Color.White) Color.Black else Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter,
                                    color = if (accentColor == Color.White) Color.Black else Color.White,
                                    fontSize = 24.sp,
                                    fontFamily = appFont,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    // Alphabet Sidebar
                    Column(
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                sidebarHeight = coords.size.height
                            }
                            .background(textColor.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 6.dp, vertical = 8.dp)
                            .pointerInput(alphabets, filteredApps) {
                                awaitPointerEventScope {
                                    while (true) {
                                        // Wait for touch down
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        var lastActiveIndex = -1

                                        activeLetterTouchY = down.position.y
                                        if (sidebarHeight > 1) {
                                            val paddingPx = with(density) { 8.dp.toPx() }
                                            val activeHeight = sidebarHeight - (paddingPx * 2)
                                            val progress = if (activeHeight > 1) {
                                                ((down.position.y - paddingPx) / activeHeight).coerceIn(0f, 1f)
                                            } else {
                                                0f
                                            }
                                            val letterIndex = (progress * (alphabets.size - 1)).roundToInt()
                                            if (letterIndex in alphabets.indices) {
                                                val letter = alphabets[letterIndex]
                                                activeLetter = letter
                                                lastActiveIndex = letterIndex

                                                val scrollTarget = letterIndexMap[letter] ?: 0

                                                if (scrollTarget != -1) {
                                                    scrollJob?.cancel()
                                                    scrollJob = coroutineScope.launch {
                                                        if (isListView) {
                                                            listState.scrollToItem(scrollTarget)
                                                        } else {
                                                            gridState.scrollToItem(scrollTarget)
                                                        }
                                                    }
                                                }
                                                if (vibrateEnabled) {
                                                    try {
                                                        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                                    } catch (e: Exception) {}
                                                }
                                            }
                                        }

                                        // Keep tracking drag moves
                                        val pointerId = down.id
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val anyPressed = event.changes.any { it.pressed }
                                            if (!anyPressed) {
                                                activeLetter = null
                                                activeLetterTouchY = null
                                                break
                                            }
                                            val change = event.changes.firstOrNull { it.id == pointerId }
                                            if (change != null) {
                                                activeLetterTouchY = change.position.y
                                                if (sidebarHeight > 1) {
                                                    val paddingPx = with(density) { 8.dp.toPx() }
                                                    val activeHeight = sidebarHeight - (paddingPx * 2)
                                                    val progress = if (activeHeight > 1) {
                                                        ((change.position.y - paddingPx) / activeHeight).coerceIn(0f, 1f)
                                                    } else {
                                                        0f
                                                    }
                                                    val letterIndex = (progress * (alphabets.size - 1)).roundToInt()
                                                    if (letterIndex in alphabets.indices && letterIndex != lastActiveIndex) {
                                                        lastActiveIndex = letterIndex
                                                        val letter = alphabets[letterIndex]
                                                        activeLetter = letter

                                                        val scrollTarget = letterIndexMap[letter] ?: 0

                                                        if (scrollTarget != -1) {
                                                            scrollJob?.cancel()
                                                            scrollJob = coroutineScope.launch {
                                                                if (isListView) {
                                                                    listState.scrollToItem(scrollTarget)
                                                                } else {
                                                                    gridState.scrollToItem(scrollTarget)
                                                                }
                                                            }
                                                        }
                                                        if (vibrateEnabled) {
                                                            try {
                                                                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                                            } catch (e: Exception) {}
                                                        }
                                                    }
                                                }
                                                change.consume()
                                            }
                                        }
                                    }
                                }
                            },
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val sigma = with(density) { 36.dp.toPx() }
                        alphabets.forEachIndexed { idx, letter ->
                            val isCurrentActive = letter == activeLetter
                            Text(
                                text = letter,
                                color = if (isCurrentActive) accentColor else textColor,
                                fontSize = 10.sp,
                                fontFamily = appFont,
                                fontWeight = if (isCurrentActive) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .width(16.dp)
                                    .graphicsLayer {
                                        // Read activeLetterTouchY and activeLetter inside the graphicsLayer lambda.
                                        // This defers the state-read to the drawing phase, eliminating recompositions!
                                        val factor = if (activeLetterTouchY != null && activeLetter != null) {
                                            val touchY = activeLetterTouchY!!
                                            val letterY = (idx + 0.5f) * (sidebarHeight.toFloat() / alphabets.size)
                                            val dist = kotlin.math.abs(letterY - touchY)
                                            kotlin.math.exp(- (dist * dist) / (2 * sigma * sigma)).toFloat()
                                        } else {
                                            0f
                                        }

                                        scaleX = 1f + factor * 0.8f
                                        scaleY = 1f + factor * 0.8f
                                        translationX = factor * (-24.dp.toPx())
                                        alpha = 0.35f + factor * 0.65f
                                    }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Long Press Dialog Sheet ---
    if (isDialogVisible && longPressedApp != null) {
        val app = longPressedApp!!
        val isPinned = remember(pinnedApps, app.packageName) {
            pinnedApps.any { it.packageName == app.packageName }
        }

        AlertDialog(
            onDismissRequest = { isDialogVisible = false },
            title = {
                Text(
                    text = app.label.uppercase(),
                    color = textColor,
                    fontSize = 14.sp,
                    fontFamily = appFont,
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
                            if (isPinned) {
                                viewModel.unpinApp(app.packageName)
                            } else {
                                viewModel.pinApp(app)
                            }
                            isDialogVisible = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = textColor.copy(alpha = 0.05f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.PushPin, contentDescription = null, tint = if (isPinned) accentColor else textColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPinned) "UNPIN FROM HOMESCREEN" else "PIN TO HOMESCREEN",
                            color = textColor,
                            fontFamily = appFont,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.hideApp(app.packageName)
                            isDialogVisible = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = textColor.copy(alpha = 0.05f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.VisibilityOff, contentDescription = null, tint = textColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "HIDE APPLICATION",
                            color = textColor,
                            fontFamily = appFont,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { isDialogVisible = false }) {
                    Text("CLOSE", color = secondaryTextColor, fontFamily = appFont)
                }
            },
            containerColor = if (darkTheme) Color(0xFF121212) else Color(0xFFFFFFFF),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp
        )
    }
}
