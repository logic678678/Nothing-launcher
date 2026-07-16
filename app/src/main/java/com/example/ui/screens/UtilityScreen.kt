package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TodoEntity
import com.example.ui.LauncherViewModel
import java.text.DecimalFormat
import kotlin.math.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UtilityScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val cardBg = com.example.ui.theme.LocalCardBackgroundColor.current
    val cardBorder = com.example.ui.theme.LocalCardBorderColor.current
    val cardTextColor = com.example.ui.theme.LocalCardTextColor.current
    val cardSecondaryTextColor = com.example.ui.theme.LocalCardSecondaryTextColor.current
    val accentColor = com.example.ui.theme.LocalAccentColor.current
    val appFont = com.example.ui.theme.LocalAppFont.current

    val todoItems by viewModel.todoItems.collectAsState()
    val quickNote by viewModel.quickNote.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Sync Board, 1: Calculator

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // --- Header Section ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .background(cardTextColor.copy(alpha = 0.05f), CircleShape)
                        .border(1.dp, cardBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Go Back",
                        tint = cardTextColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = "UTILITY HUB",
                        color = cardTextColor,
                        fontSize = 15.sp,
                        fontFamily = appFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "NOTHING COMPANION",
                        color = cardSecondaryTextColor,
                        fontSize = 8.sp,
                        fontFamily = appFont,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Custom Segmented Control for Tabs
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardTextColor.copy(alpha = 0.05f))
                    .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf("SYNC BOARD", "CALCULATOR").forEachIndexed { index, label ->
                    val isSelected = activeTab == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) cardTextColor.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { activeTab = index }
                            .padding(vertical = 6.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) cardTextColor else cardSecondaryTextColor,
                            fontSize = 8.5.sp,
                            fontFamily = appFont,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- Core Tab Views ---
        Box(modifier = Modifier.weight(1f)) {
            if (activeTab == 0) {
                SyncDashboardTab(
                    todoItems = todoItems,
                    quickNote = quickNote,
                    onAddTodo = { viewModel.addTodoItem(it) },
                    onToggleTodo = { viewModel.toggleTodoItem(it) },
                    onDeleteTodo = { viewModel.deleteTodoItem(it) },
                    onClearCompletedTodos = { viewModel.clearCompletedTodoItems() },
                    onSaveNote = { viewModel.saveQuickNote(it) },
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    cardTextColor = cardTextColor,
                    cardSecondaryTextColor = cardSecondaryTextColor,
                    accentColor = accentColor,
                    appFont = appFont
                )
            } else {
                CalculatorTab(
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    cardTextColor = cardTextColor,
                    cardSecondaryTextColor = cardSecondaryTextColor,
                    accentColor = accentColor,
                    appFont = appFont
                )
            }
        }
    }
}

@Composable
fun SyncDashboardTab(
    todoItems: List<TodoEntity>,
    quickNote: String,
    onAddTodo: (String) -> Unit,
    onToggleTodo: (TodoEntity) -> Unit,
    onDeleteTodo: (Long) -> Unit,
    onClearCompletedTodos: () -> Unit,
    onSaveNote: (String) -> Unit,
    cardBg: Color,
    cardBorder: Color,
    cardTextColor: Color,
    cardSecondaryTextColor: Color,
    accentColor: Color,
    appFont: androidx.compose.ui.text.font.FontFamily
) {
    var notepadText by remember(quickNote) { mutableStateOf(quickNote) }
    var todoFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Active, 2: Completed
    var newTodoText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Compute stats
    val totalTodos = todoItems.size
    val completedTodos = todoItems.count { it.isCompleted }
    val progress = if (totalTodos > 0) completedTodos.toFloat() / totalTodos else 0f

    val filteredTodos = when (todoFilter) {
        1 -> todoItems.filter { !it.isCompleted }
        2 -> todoItems.filter { it.isCompleted }
        else -> todoItems
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // --- 1. Advanced Notepad Card ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, cardBorder),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(accentColor, CircleShape)
                            )
                            Text(
                                text = "NOTES",
                                color = cardTextColor,
                                fontSize = 10.sp,
                                fontFamily = appFont,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Copy button
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(notepadText))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy Note",
                                    tint = cardSecondaryTextColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            // Clear button
                            IconButton(
                                onClick = {
                                    notepadText = ""
                                    onSaveNote("")
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Clear Note",
                                    tint = cardSecondaryTextColor,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    BasicTextField(
                        value = notepadText,
                        onValueChange = {
                            notepadText = it
                            onSaveNote(it)
                        },
                        textStyle = TextStyle(
                            color = cardTextColor,
                            fontSize = 13.sp,
                            fontFamily = appFont,
                            lineHeight = 18.sp
                        ),
                        cursorBrush = SolidColor(accentColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 240.dp)
                            .background(cardTextColor.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                            .border(1.dp, cardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${notepadText.length} characters",
                            color = cardSecondaryTextColor,
                            fontSize = 8.sp,
                            fontFamily = appFont
                        )
                        Text(
                            text = "Auto-saves to Home widget",
                            color = cardSecondaryTextColor.copy(alpha = 0.7f),
                            fontSize = 8.sp,
                            fontFamily = appFont
                        )
                    }
                }
            }
        }

        // --- 2. Advanced To-Do List Card ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, cardBorder),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header with Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(accentColor, CircleShape)
                            )
                            Text(
                                text = "TO-DOs",
                                color = cardTextColor,
                                fontSize = 10.sp,
                                fontFamily = appFont,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        // Statistics pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentColor.copy(alpha = 0.1f))
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        ) {
                            Text(
                                text = "$completedTodos/$totalTodos DONE",
                                color = accentColor,
                                fontSize = 8.sp,
                                fontFamily = appFont,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress indicator
                    if (totalTodos > 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = accentColor,
                                trackColor = cardTextColor.copy(alpha = 0.05f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    // Add Todo Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardTextColor.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                            .border(1.dp, cardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = newTodoText,
                            onValueChange = { newTodoText = it },
                            textStyle = TextStyle(
                                color = cardTextColor,
                                fontSize = 12.5.sp,
                                fontFamily = appFont
                            ),
                            cursorBrush = SolidColor(accentColor),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (newTodoText.isNotBlank()) {
                                        onAddTodo(newTodoText.trim())
                                        newTodoText = ""
                                    }
                                }
                            )
                        )

                        IconButton(
                            onClick = {
                                if (newTodoText.isNotBlank()) {
                                    onAddTodo(newTodoText.trim())
                                    newTodoText = ""
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(cardTextColor.copy(alpha = 0.05f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add Task",
                                tint = cardTextColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filters & Action Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // All, Active, Completed Segment
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardTextColor.copy(alpha = 0.03f))
                                .padding(2.dp)
                        ) {
                            listOf("ALL", "ACTIVE", "DONE").forEachIndexed { index, filterLabel ->
                                val isSelected = todoFilter == index
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) cardTextColor.copy(alpha = 0.08f) else Color.Transparent)
                                        .clickable { todoFilter = index }
                                        .padding(vertical = 4.dp, horizontal = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = filterLabel,
                                        color = if (isSelected) cardTextColor else cardSecondaryTextColor,
                                        fontSize = 8.sp,
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Clear completed button
                        if (completedTodos > 0) {
                            Text(
                                text = "CLEAR COMPLETED",
                                color = accentColor,
                                fontSize = 8.sp,
                                fontFamily = appFont,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onClearCompletedTodos() }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tasks list rendering (showing filtered ones)
                    if (filteredTodos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "NO TASKS FOUND",
                                color = cardSecondaryTextColor.copy(alpha = 0.4f),
                                fontSize = 10.sp,
                                fontFamily = appFont,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            filteredTodos.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cardTextColor.copy(alpha = 0.02f))
                                        .clickable { onToggleTodo(item) }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
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
                                            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteTodo(item.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete Todo",
                                            tint = cardSecondaryTextColor.copy(alpha = 0.5f),
                                            modifier = Modifier.size(14.dp)
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

@Composable
fun CalculatorTab(
    cardBg: Color,
    cardBorder: Color,
    cardTextColor: Color,
    cardSecondaryTextColor: Color,
    accentColor: Color,
    appFont: androidx.compose.ui.text.font.FontFamily
) {
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var isScientific by remember { mutableStateOf(false) }

    fun handleBtnClick(symbol: String) {
        when (symbol) {
            "C" -> {
                input = ""
                result = ""
            }
            "⌫" -> {
                if (input.isNotEmpty()) {
                    input = input.substring(0, input.length - 1)
                }
            }
            "=" -> {
                try {
                    val evaluated = MathExpressionEvaluator.eval(input)
                    val df = DecimalFormat("#.########")
                    result = df.format(evaluated)
                } catch (e: Exception) {
                    result = "ERROR"
                }
            }
            else -> {
                input += symbol
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, cardBorder),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header for Calculator Tab
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(accentColor, CircleShape)
                    )
                    Text(
                        text = "NOTHING CALC",
                        color = cardTextColor,
                        fontSize = 10.sp,
                        fontFamily = appFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Scientific mode toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardTextColor.copy(alpha = 0.05f))
                        .clickable { isScientific = !isScientific }
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isScientific) Icons.Filled.Science else Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = if (isScientific) accentColor else cardSecondaryTextColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "SCIENTIFIC",
                        color = if (isScientific) cardTextColor else cardSecondaryTextColor,
                        fontSize = 8.sp,
                        fontFamily = appFont,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display Screen Area (Styled like Nothing OS screen dot matrix/clean screen)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Input Display
                    Text(
                        text = if (input.isEmpty()) "0" else input,
                        color = Color.White,
                        fontSize = if (input.length > 15) 20.sp else 32.sp,
                        fontFamily = appFont,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )

                    // Live / Calculated Result
                    Text(
                        text = result,
                        color = accentColor,
                        fontSize = 18.sp,
                        fontFamily = appFont,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons Layout
            val standardButtons = listOf(
                listOf("C", "⌫", "%", "/"),
                listOf("7", "8", "9", "*"),
                listOf("4", "5", "6", "-"),
                listOf("1", "2", "3", "+"),
                listOf("0", ".", "(", "=")
            )

            val scientificButtons = listOf(
                listOf("sin(", "cos(", "tan(", "^"),
                listOf("sqrt(", "log(", "ln(", ")"),
                listOf("pi", "e", "C", "⌫"),
                listOf("7", "8", "9", "/"),
                listOf("4", "5", "6", "*"),
                listOf("1", "2", "3", "-"),
                listOf("0", ".", "+", "=")
            )

            val buttonRows = if (isScientific) scientificButtons else standardButtons

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                buttonRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { symbol ->
                            val isAction = symbol in listOf("C", "⌫", "%", "/", "*", "-", "+", "=", "^", "sin(", "cos(", "tan(", "sqrt(", "log(", "ln(")
                            val isEquals = symbol == "="
                            val isClear = symbol == "C"

                            val btnBg = when {
                                isEquals -> accentColor
                                isClear -> Color(0xFFFF3B30).copy(alpha = 0.15f)
                                isAction -> cardTextColor.copy(alpha = 0.08f)
                                else -> cardTextColor.copy(alpha = 0.03f)
                            }

                            val btnTextCol = when {
                                isEquals -> Color.White
                                isClear -> Color(0xFFFF3B30)
                                isAction -> accentColor
                                else -> cardTextColor
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(if (isScientific) 1.6f else 1f)
                                    .clip(RoundedCornerShape(if (isScientific) 12.dp else 24.dp))
                                    .background(btnBg)
                                    .border(
                                        1.dp,
                                        if (isEquals) accentColor else cardBorder.copy(alpha = 0.6f),
                                        RoundedCornerShape(if (isScientific) 12.dp else 24.dp)
                                    )
                                    .clickable { handleBtnClick(symbol) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = symbol.replace("(", ""),
                                    color = btnTextCol,
                                    fontSize = if (isScientific && symbol.length > 2) 10.sp else 14.sp,
                                    fontFamily = appFont,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Robust mathematical expression parser supporting scientific calculations.
 */
object MathExpressionEvaluator {
    fun eval(str: String): Double {
        val formatted = str
            .replace("pi", "3.14159265359")
            .replace("e", "2.71828182845")

        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < formatted.length) formatted[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < formatted.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            // Expression: addition and subtraction
            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm() // addition
                    else if (eat('-'.code)) x -= parseTerm() // subtraction
                    else return x
                }
            }

            // Term: multiplication and division
            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor() // multiplication
                    else if (eat('/'.code)) x /= parseFactor() // division
                    else if (eat('%'.code)) x %= parseFactor() // modulo
                    else return x
                }
            }

            // Factor: powers, numbers, parentheses, and functions
            fun parseFactor(): Double {
                if (eat('+'.code)) return +parseFactor() // unary plus
                if (eat('-'.code)) return -parseFactor() // unary minus

                var x: Double
                val startPos = this.pos
                if (eat('('.code)) { // parentheses
                    x = parseExpression()
                    if (!eat(')'.code)) throw RuntimeException("Missing closing parenthesis")
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) { // numbers
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    x = formatted.substring(startPos, this.pos).toDouble()
                } else if (ch >= 'a'.code && ch <= 'z'.code) { // functions
                    while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                    val func = formatted.substring(startPos, this.pos)
                    if (eat('('.code)) {
                        x = parseExpression()
                        if (!eat(')'.code)) throw RuntimeException("Missing closing parenthesis for function $func")
                    } else {
                        x = parseFactor()
                    }
                    x = when (func) {
                        "sqrt" -> sqrt(x)
                        "sin" -> sin(Math.toRadians(x))
                        "cos" -> cos(Math.toRadians(x))
                        "tan" -> tan(Math.toRadians(x))
                        "log" -> log10(x)
                        "ln" -> ln(x)
                        else -> throw RuntimeException("Unknown function: $func")
                    }
                } else {
                    throw RuntimeException("Unexpected character: " + ch.toChar())
                }

                if (eat('^'.code)) x = x.pow(parseFactor()) // exponentiation

                return x
            }
        }.parse()
    }
}
