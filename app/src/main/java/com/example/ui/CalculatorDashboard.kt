package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.HistoryItem
import com.example.data.db.ReminderTask
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class NavItem(val route: String, val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorDashboard(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val isBiometricLocked by viewModel.isBiometricLocked.collectAsStateWithLifecycle()
    val activeNotifications by viewModel.activeNotifications.collectAsStateWithLifecycle()
    val exportMsg by viewModel.exportMessage.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf("calc") }

    // Display Toast / Banner for export statuses
    LaunchedEffect(exportMsg) {
        if (exportMsg != null) {
            // Can show snackbar etc, let's keep it visible inside of in-app banners
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonDark)
    ) {
        // Main Screen Scaffold
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = CarbonDark,
                        titleContentColor = CarbonWhite
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (isOnline) Color.Green else Color.Red,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isOnline) "Облачная синхронизация" else "Автономный режим",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextGray
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.toggleOnlineMode() },
                            modifier = Modifier.testTag("action_toggle_online")
                        ) {
                            val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
                            Icon(
                                imageVector = if (isOnline) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                                contentDescription = "Переключить режим работы",
                                tint = if (isOnline) MatrixCyan else TextGray
                            )
                        }
                        IconButton(
                            onClick = { viewModel.lockManual() },
                            modifier = Modifier.testTag("action_lock")
                        ) {
                            val isLockEnabled by viewModel.isLockEnabled.collectAsStateWithLifecycle()
                            Icon(
                                imageVector = if (isLockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Заблокировать экран",
                                tint = if (isLockEnabled) ScientificOrange else TextGray
                            )
                        }
                    }
                )
            },
            bottomBar = {
                // Persistent elegant navigation bottom bar with system safe offsets
                NavigationBar(
                    containerColor = CarbonSurface,
                    contentColor = CarbonWhite,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    val items = listOf(
                        NavItem("calc", "Главная", Icons.Default.Calculate),
                        NavItem("converter", "Конвертер", Icons.Default.CompareArrows),
                        NavItem("equations", "Уравнения", Icons.Default.Functions),
                        NavItem("graph", "График", Icons.Default.ShowChart),
                        NavItem("sync", "История", Icons.Default.History),
                        NavItem("tasks", "Задачи", Icons.Default.Alarm)
                    )
                    items.forEach { item ->
                        NavigationBarItem(
                            selected = currentTab == item.route,
                            onClick = { currentTab = item.route },
                            modifier = Modifier.testTag("nav_tab_${item.route}"),
                            icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                            label = { Text(item.label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CarbonDark,
                                unselectedIconColor = TextGray,
                                selectedTextColor = MatrixCyan,
                                unselectedTextColor = TextGray,
                                indicatorColor = MatrixCyan
                            )
                        )
                    }
                }
            },
            containerColor = CarbonDark
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Render Tab Container
                when (currentTab) {
                    "calc" -> ScientificCalcTab(viewModel)
                    "converter" -> ConverterTab(viewModel)
                    "equations" -> EquationsTab()
                    "graph" -> GraphTab(viewModel)
                    "sync" -> CloudHistoryTab(viewModel, onRestoreExpr = { expr ->
                        coroutineScope.launch {
                            viewModel.loadHistoryDirectly(expr)
                            currentTab = "calc"
                        }
                    })
                    "tasks" -> TasksSchedulesTab(viewModel)
                }

                // Temporary export outcome card
                if (exportMsg != null) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .shadow(8.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CarbonSurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Подтверждение экспорта",
                                tint = MatrixCyan
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = exportMsg!!,
                                color = CarbonWhite,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.clearExportMessage() }) {
                                Text("ОК", color = MatrixCyan)
                            }
                        }
                    }
                }
            }
        }

        // 1. Interactive Heads up dynamic notifications system
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                activeNotifications.forEach { notif ->
                    Card(
                        modifier = Modifier
                            .padding(vertical = 4.dp, horizontal = 24.dp)
                            .shadow(6.dp)
                            .fillMaxWidth()
                            .clickable { viewModel.dismissNotification(notif.id) },
                        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationImportant,
                                contentDescription = "In-App Notification Bubble",
                                tint = ScientificOrange
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(notif.title, fontWeight = FontWeight.Bold, color = CarbonWhite, fontSize = 14.sp)
                                Text(notif.body, color = TextGray, fontSize = 12.sp)
                            }
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Bubble",
                                tint = TextGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Full Secure Biometric & PIN lock overlay
        AnimatedVisibility(
            visible = isBiometricLocked,
            enter = fadeIn() + expandIn(),
            exit = fadeOut() + shrinkOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            LockOverlayScreen(viewModel)
        }
    }
}

// ----------------------------------------------------
// TABS & COMPONENTS
// ----------------------------------------------------

@Composable
fun ScientificCalcTab(viewModel: CalculatorViewModel) {
    val calcInput by viewModel.calcInput.collectAsStateWithLifecycle()
    val calcResult by viewModel.calcResult.collectAsStateWithLifecycle()
    val calcError by viewModel.calcError.collectAsStateWithLifecycle()
    val isDegreeMode by viewModel.isDegreeMode.collectAsStateWithLifecycle()

    var showScientificKeys by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Output Displays
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 130.dp, max = 200.dp)
                .weight(1f)
                .shadow(4.dp),
            colors = CardDefaults.cardColors(containerColor = CarbonSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                // Formula input
                Text(
                    text = calcInput.ifEmpty { "0" },
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                    color = CarbonWhite,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Error / Result Line
                if (calcError != null) {
                    Text(
                        text = calcError!!,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (calcResult.isNotEmpty()) {
                    Text(
                        text = "= $calcResult",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MatrixCyan,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    val calcPreview by viewModel.calcPreview.collectAsStateWithLifecycle()
                    if (calcPreview.isNotEmpty()) {
                        Text(
                            text = calcPreview,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal,
                            color = MatrixCyan.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = if (isDegreeMode) "Режим: Градусы" else "Режим: Радианы",
                            fontSize = 12.sp,
                            color = TextGray,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Dynamic scientific option row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showScientificKeys) "Инженерная клавиатура" else "Простая клавиатура",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (showScientificKeys) MatrixCyan else TextGray
            )
            TextButton(
                onClick = { showScientificKeys = !showScientificKeys },
                modifier = Modifier.testTag("btn_toggle_scientific_kb")
            ) {
                Text(
                    text = if (showScientificKeys) "СКРЫТЬ ДОП" else "ПОКАЗАТЬ ДОП",
                    color = ScientificOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Keyboard grid
        val keys = if (showScientificKeys) {
            listOf(
                "sin", "cos", "tan", "ctg",
                "ln", "log", "deg", "√",
                "^", "!", "π", "e",
                "abs", "±", "(", ")",
                "C", "⌫", "%", "÷",
                "7", "8", "9", "×",
                "4", "5", "6", "-",
                "1", "2", "3", "+",
                "0", ".", "x", "="
            )
        } else {
            listOf(
                "C", "⌫", "%", "÷",
                "7", "8", "9", "×",
                "4", "5", "6", "-",
                "1", "2", "3", "+",
                "0", ".", "(", "="
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(keys, key = { it }) { key ->
                val isOperatorState = key in listOf("/", "÷", "*", "×", "-", "+", "=", "^", "%")
                val isActionState = key in listOf("C", "⌫")
                val isScientificState = key in listOf("sin", "cos", "tan", "ctg", "ln", "log", "deg", "√", "!", "π", "e", "abs", "±", "x")

                val btnBg = when {
                    isActionState -> Color(red = 239, green = 83, blue = 80, alpha = 40)
                    isOperatorState -> OrangeBg
                    isScientificState -> CyanBg
                    else -> CarbonSurface
                }
                val textTint = when {
                    isActionState -> Color(0xFFEF5350)
                    isOperatorState -> ScientificOrange
                    isScientificState -> MatrixCyan
                    else -> CarbonWhite
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .aspectRatio(if (showScientificKeys) 1.8f else 1.35f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(btnBg)
                        .clickable { viewModel.onCalcClick(key) }
                        .border(
                            1.dp,
                            if (key == "=") MatrixCyan else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .testTag("calc_key_$key")
                ) {
                    val labelToShow = if (key == "deg") {
                        if (isDegreeMode) "ГРАД" else "РАД"
                    } else key

                    Text(
                        text = labelToShow,
                        color = textTint,
                        fontWeight = if (key == "=" || isOperatorState) FontWeight.Black else FontWeight.Bold,
                        fontSize = if (labelToShow.length > 3) 13.sp else 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

private fun convert(
    value: Double,
    fromUnit: String,
    toUnit: String,
    category: String,
    currencyRates: Map<String, Double> = emptyMap()
): Double {
    if (category == "Валюта") {
        val fromCode = fromUnit.substringBefore(" ").trim()
        val toCode = toUnit.substringBefore(" ").trim()
        val rateFrom = currencyRates[fromCode] ?: 1.0
        val rateTo = currencyRates[toCode] ?: 1.0
        return (value / rateFrom) * rateTo
    }

    if (category == "Температура") {
        val celsius = when (fromUnit) {
            "Цельсий (°C)" -> value
            "Фаренгейт (°F)" -> (value - 32.0) / 1.8
            "Кельвин (K)" -> value - 273.15
            else -> value
        }
        return when (toUnit) {
            "Цельсий (°C)" -> celsius
            "Фаренгейт (°F)" -> celsius * 1.8 + 32.0
            "Кельвин (K)" -> celsius + 273.15
            else -> celsius
        }
    }

    val rateFrom = when (category) {
        "Длина" -> when (fromUnit) {
            "Метры (м)" -> 1.0
            "Километры (км)" -> 1000.0
            "Сантиметры (см)" -> 0.01
            "Миллиметры (мм)" -> 0.001
            "Дюймы (in)" -> 0.0254
            "Футы (ft)" -> 0.3048
            else -> 1.0
        }
        "Вес" -> when (fromUnit) {
            "Килограммы (кг)" -> 1.0
            "Граммы (г)" -> 0.001
            "Фунты (lb)" -> 0.45359237
            "Унции (oz)" -> 0.028349523
            "Тонны (т)" -> 1000.0
            else -> 1.0
        }
        "Площадь" -> when (fromUnit) {
            "Кв. метры (м²)" -> 1.0
            "Кв. километры (км²)" -> 1000000.0
            "Гектары (га)" -> 10000.0
            "Акры (ac)" -> 4046.8564
            "Кв. сантиметры (см²)" -> 0.0001
            else -> 1.0
        }
        "Время" -> when (fromUnit) {
            "Секунды (с)" -> 1.0
            "Минуты (мин)" -> 60.0
            "Часы (ч)" -> 3600.0
            "Сутки (дн)" -> 86400.0
            "Недели (нед)" -> 604800.0
            "Года (г)" -> 31536000.0
            else -> 1.0
        }
        "Данные" -> when (fromUnit) {
            "Байты (Б)" -> 1.0
            "Килобайты (КБ)" -> 1024.0
            "Мегабайты (МБ)" -> 1048576.0
            "Гигабайты (ГБ)" -> 1073741824.0
            "Терабайты (ТБ)" -> 1099511627776.0
            else -> 1.0
        }
        "Скорость" -> when (fromUnit) {
            "м/с" -> 1.0
            "км/ч" -> 1.0 / 3.6
            "миль/ч (mph)" -> 0.44704
            "узлы (kt)" -> 0.514444
            else -> 1.0
        }
        else -> 1.0
    }

    val rateTo = when (category) {
        "Длина" -> when (toUnit) {
            "Метры (м)" -> 1.0
            "Километры (км)" -> 1000.0
            "Сантиметры (см)" -> 0.01
            "Миллиметры (мм)" -> 0.001
            "Дюймы (in)" -> 0.0254
            "Футы (ft)" -> 0.3048
            else -> 1.0
        }
        "Вес" -> when (toUnit) {
            "Килограммы (кг)" -> 1.0
            "Граммы (г)" -> 0.001
            "Фунты (lb)" -> 0.45359237
            "Унции (oz)" -> 0.028349523
            "Тонны (т)" -> 1000.0
            else -> 1.0
        }
        "Площадь" -> when (toUnit) {
            "Кв. метры (м²)" -> 1.0
            "Кв. километры (км²)" -> 1000000.0
            "Гектары (га)" -> 10000.0
            "Акры (ac)" -> 4046.8564
            "Кв. сантиметры (см²)" -> 0.0001
            else -> 1.0
        }
        "Время" -> when (toUnit) {
            "Секунды (с)" -> 1.0
            "Минуты (мин)" -> 60.0
            "Часы (ч)" -> 3600.0
            "Сутки (дн)" -> 86400.0
            "Недели (нед)" -> 604800.0
            "Года (г)" -> 31536000.0
            else -> 1.0
        }
        "Данные" -> when (toUnit) {
            "Байты (Б)" -> 1.0
            "Килобайты (КБ)" -> 1024.0
            "Мегабайты (МБ)" -> 1048576.0
            "Гигабайты (ГБ)" -> 1073741824.0
            "Терабайты (ТБ)" -> 1099511627776.0
            else -> 1.0
        }
        "Скорость" -> when (toUnit) {
            "м/с" -> 1.0
            "км/ч" -> 1.0 / 3.6
            "миль/ч (mph)" -> 0.44704
            "узлы (kt)" -> 0.514444
            else -> 1.0
        }
        else -> 1.0
    }

    return (value * rateFrom) / rateTo
}

@Composable
fun ConverterTab(viewModel: CalculatorViewModel) {
    fun findGcd(a: Long, b: Long): Long {
        var x = Math.abs(a)
        var y = Math.abs(b)
        while (y != 0L) {
            val t = y
            y = x % y
            x = t
        }
        return if (x == 0L) 1L else x
    }

    fun findLcm(a: Long, b: Long): Long {
        if (a == 0L || b == 0L) return 0L
        return Math.abs(a * b) / findGcd(a, b)
    }

    var selectedCategory by remember { mutableStateOf("Длина") }

    val categories = listOf("Длина", "Вес", "Температура", "Площадь", "Время", "Данные", "Скорость", "Валюта", "Скидки", "ИМТ", "Дроби")

    val unitsMap = mapOf(
        "Длина" to listOf("Метры (м)", "Километры (км)", "Сантиметры (см)", "Миллиметры (мм)", "Дюймы (in)", "Футы (ft)"),
        "Вес" to listOf("Килограммы (кг)", "Граммы (г)", "Фунты (lb)", "Унции (oz)", "Тонны (т)"),
        "Температура" to listOf("Цельсий (°C)", "Фаренгейт (°F)", "Кельвин (K)"),
        "Площадь" to listOf("Кв. метры (м²)", "Кв. километры (км²)", "Гектары (га)", "Акры (ac)", "Кв. сантиметры (см²)"),
        "Время" to listOf("Секунды (с)", "Минуты (мин)", "Часы (ч)", "Сутки (дн)", "Недели (нед)", "Года (г)"),
        "Данные" to listOf("Байты (Б)", "Килобайты (КБ)", "Мегабайты (МБ)", "Гигабайты (ГБ)", "Терабайты (ТБ)"),
        "Скорость" to listOf("м/с", "км/ч", "миль/ч (mph)", "узлы (kt)"),
        "Валюта" to listOf("USD ($)", "EUR (€)", "RUB (₽)", "UAH (₴)", "GBP (£)", "CNY (¥)")
    )

    // Currencies from ViewModel
    val currencyRates by viewModel.currencyRates.collectAsStateWithLifecycle()
    val currencyStatus by viewModel.currencyStatus.collectAsStateWithLifecycle()

    var fromUnit by remember(selectedCategory) {
        mutableStateOf(unitsMap[selectedCategory]?.get(0) ?: "")
    }
    var toUnit by remember(selectedCategory) {
        mutableStateOf(unitsMap[selectedCategory]?.get(1) ?: "")
    }

    var inputValueStr by remember { mutableStateOf("1") }
    var copiedFeedback by remember { mutableStateOf(false) }

    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    val inputValue = inputValueStr.toDoubleOrNull() ?: 0.0

    val conversionResult = try {
        if (inputValueStr.isEmpty()) 0.0 else convert(inputValue, fromUnit, toUnit, selectedCategory, currencyRates)
    } catch (e: Exception) {
        0.0
    }

    val symbols = java.text.DecimalFormatSymbols(java.util.Locale.US)
    val formatter = java.text.DecimalFormat("0.######", symbols)
    val formattedResult = if (Math.abs(conversionResult - conversionResult.toLong()) < 1e-9) {
        conversionResult.toLong().toString()
    } else {
        formatter.format(conversionResult)
    }

    // Discount Form State
    var originalPriceStr by remember { mutableStateOf("1000") }
    var discountPercentStr by remember { mutableStateOf("15") }
    var taxPercentStr by remember { mutableStateOf("0") }

    // BMI State
    var heightStr by remember { mutableStateOf("175") }
    var weightStr by remember { mutableStateOf("70") }

    // Fractions Calculator State
    var fracWhole1Str by remember { mutableStateOf("0") }
    var fracNum1Str by remember { mutableStateOf("1") }
    var fracDen1Str by remember { mutableStateOf("2") }

    var fracWhole2Str by remember { mutableStateOf("0") }
    var fracNum2Str by remember { mutableStateOf("3") }
    var fracDen2Str by remember { mutableStateOf("4") }

    var fracOperator by remember { mutableStateOf("+") }
    var showFracSteps by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Смарт-Конвертер Величин",
                        fontWeight = FontWeight.Black,
                        color = MatrixCyan,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Включает физические меры, скидки, ИМТ и отслеживание курсов валют в реальном времени.",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Horizontal Category Switcher
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategory == category
                    Button(
                        onClick = {
                            selectedCategory = category
                            copiedFeedback = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MatrixCyan else CarbonSurface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("converter_cat_$category")
                    ) {
                        Text(
                            text = category,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isSelected) CarbonDark else CarbonWhite
                        )
                    }
                }
            }
        }

        // Conditional forms
        if (selectedCategory == "Скидки") {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Смарт-Калькулятор Скидок (Гривны ₴)", fontWeight = FontWeight.Bold, color = MatrixCyan, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Исходная цена (грн)", color = TextGray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = originalPriceStr,
                            onValueChange = { originalPriceStr = it },
                            textStyle = TextStyle(color = CarbonWhite, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MatrixCyan, unfocusedBorderColor = TextGray),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("discount_price")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Скидка (%)", color = TextGray, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = discountPercentStr,
                                    onValueChange = { discountPercentStr = it },
                                    textStyle = TextStyle(color = CarbonWhite, fontFamily = FontFamily.Monospace),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MatrixCyan, unfocusedBorderColor = TextGray),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.testTag("discount_percent")
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Налог (%)", color = TextGray, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = taxPercentStr,
                                    onValueChange = { taxPercentStr = it },
                                    textStyle = TextStyle(color = CarbonWhite, fontFamily = FontFamily.Monospace),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MatrixCyan, unfocusedBorderColor = TextGray),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.testTag("discount_tax")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Fast percentage presets layout (extremely premium feature)
                        Text("Быстрый выбор скидки:", color = TextGray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            listOf("5", "10", "15", "20", "30", "50", "70").forEach { pct ->
                                val isSelected = discountPercentStr == pct
                                Surface(
                                    modifier = Modifier.clickable { discountPercentStr = pct },
                                    color = if (isSelected) MatrixCyan else CarbonDark,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "$pct%",
                                        color = if (isSelected) CarbonDark else CarbonWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        val op = originalPriceStr.toDoubleOrNull() ?: 0.0
                        val dp = discountPercentStr.toDoubleOrNull() ?: 0.0
                        val tp = taxPercentStr.toDoubleOrNull() ?: 0.0
                        val amountSaved = op * (dp / 100.0)
                        val afterDiscount = op - amountSaved
                        val taxAmt = afterDiscount * (tp / 100.0)
                        val finalPrice = afterDiscount + taxAmt

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CarbonDark, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Итоговая цена:", color = CarbonWhite, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${formatter.format(finalPrice)} ₴",
                                        color = MatrixCyan,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 18.sp
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Сэкономлено:", color = TextGray, fontSize = 13.sp)
                                    Text(
                                        text = "${formatter.format(amountSaved)} ₴ (${formatter.format(dp)}%)",
                                        color = ScientificOrange,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Dynamic visual indicator (Pay vs Save proportional bar)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CarbonSurface)
                                ) {
                                    val spentRatio = if (op > 0) finalPrice / op else 1.0
                                    val savedRatio = if (op > 0) amountSaved / op else 0.0
                                    
                                    if (spentRatio > 0 && op > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(spentRatio.coerceIn(0.01, 1.0).toFloat())
                                                .background(MatrixCyan)
                                        )
                                    }
                                    if (savedRatio > 0 && op > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(savedRatio.coerceIn(0.0, 1.0).toFloat())
                                                .background(ScientificOrange)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Оплата (${formatter.format(if(op>0) finalPrice/op*100 else 100.0)}%)", color = MatrixCyan, fontSize = 9.sp)
                                    Text("Сэкономлено (${formatter.format(if(op>0) amountSaved/op*100 else 0.0)}%)", color = ScientificOrange, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedCategory == "Дроби") {
            val w1 = fracWhole1Str.toLongOrNull() ?: 0L
            val n1 = fracNum1Str.toLongOrNull() ?: 0L
            val d1 = fracDen1Str.toLongOrNull() ?: 1L

            val w2 = fracWhole2Str.toLongOrNull() ?: 0L
            val n2 = fracNum2Str.toLongOrNull() ?: 0L
            val d2 = fracDen2Str.toLongOrNull() ?: 1L

            val mathD1 = if (d1 == 0L) 1L else d1
            val mathD2 = if (d2 == 0L) 1L else d2

            // Improper calculation
            val impN1 = w1 * mathD1 + n1
            val impD1 = mathD1

            val impN2 = w2 * mathD2 + n2
            val impD2 = mathD2

            var resN = 0L
            var resD = 1L
            var isDivideByZero = false
            var fractionStepText = ""

            when (fracOperator) {
                "+" -> {
                    val lcmD = findLcm(impD1, impD2)
                    val m1 = lcmD / impD1
                    val m2 = lcmD / impD2
                    val term1 = impN1 * m1
                    val term2 = impN2 * m2
                    resN = term1 + term2
                    resD = lcmD
                    
                    fractionStepText = "1. Перевод в неправильные дроби:\n" +
                            "   A = $w1 $n1/$d1 = $impN1/$impD1\n" +
                            "   B = $w2 $n2/$d2 = $impN2/$impD2\n\n" +
                            "2. Общий знаменатель: НОК($impD1, $impD2) = $lcmD\n" +
                            "   A = ${impN1} × $m1 / $lcmD = $term1/$lcmD\n" +
                            "   B = ${impN2} × $m2 / $lcmD = $term2/$lcmD\n\n" +
                            "3. Складываем числители:\n" +
                            "   $term1 + $term2 = $resN\n" +
                            "   Дробь: $resN/$resD"
                }
                "-" -> {
                    val lcmD = findLcm(impD1, impD2)
                    val m1 = lcmD / impD1
                    val m2 = lcmD / impD2
                    val term1 = impN1 * m1
                    val term2 = impN2 * m2
                    resN = term1 - term2
                    resD = lcmD
                    
                    fractionStepText = "1. Перевод в неправильные дроби:\n" +
                            "   A = $w1 $n1/$d1 = $impN1/$impD1\n" +
                            "   B = $w2 $n2/$d2 = $impN2/$impD2\n\n" +
                            "2. Общий знаменатель: НОК($impD1, $impD2) = $lcmD\n" +
                            "   A = ${impN1} × $m1 / $lcmD = $term1/$lcmD\n" +
                            "   B = ${impN2} × $m2 / $lcmD = $term2/$lcmD\n\n" +
                            "3. Вычитаем числители:\n" +
                            "   $term1 - $term2 = $resN\n" +
                            "   Дробь: $resN/$resD"
                }
                "×" -> {
                    resN = impN1 * impN2
                    resD = impD1 * impD2
                    
                    fractionStepText = "1. Перевод в неправильные дроби:\n" +
                            "   A = $impN1/$impD1, B = $impN2/$impD2\n\n" +
                            "2. Перемножаем числители и знаменатели:\n" +
                            "   Числители: $impN1 × $impN2 = $resN\n" +
                            "   Знаменатели: $impD1 × $impD2 = $resD\n" +
                            "   Дробь: $resN/$resD"
                }
                "÷" -> {
                    if (impN2 == 0L) {
                        isDivideByZero = true
                    } else {
                        resN = impN1 * impD2
                        resD = impD1 * impN2
                        if (resD < 0L) {
                            resN = -resN
                            resD = -resD
                        }
                        fractionStepText = "1. Перевод в неправильные дроби:\n" +
                                "   A = $impN1/$impD1, B = $impN2/$impD2\n\n" +
                                "2. Умножаем первую на обратную вторую:\n" +
                                "   ($impN1/$impD1) × ($impD2/$impN2)\n" +
                                "   Числители: $impN1 × $impD2 = $resN\n" +
                                "   Знаменатели: $impD1 × $impN2 = $resD\n" +
                                "   Дробь: $resN/$resD"
                    }
                }
            }

            // Simplify if possible
            val gcdValue = if (!isDivideByZero) findGcd(resN, resD) else 1L
            val simplifiedN = if (!isDivideByZero) resN / gcdValue else 0L
            val simplifiedD = if (!isDivideByZero) resD / gcdValue else 1L

            val finalSign = if (simplifiedN < 0) -1 else 1
            val absSimp = Math.abs(simplifiedN)
            val finalWhole = absSimp / simplifiedD
            val finalRem = absSimp % simplifiedD

            val decimalValue = if (!isDivideByZero) simplifiedN.toDouble() / simplifiedD.toDouble() else 0.0

            val simplificationStep = if (!isDivideByZero && gcdValue > 1L) {
                "\n\n4. Сокращаем на НОД($resN, $resD) = $gcdValue:\n" +
                "   $resN / $gcdValue = $simplifiedN\n" +
                "   $resD / $gcdValue = $simplifiedD\n" +
                "   Результат сокращения: $simplifiedN/$simplifiedD"
            } else {
                "\n\n4. Дробь несократима (НОД = 1): $simplifiedN/$simplifiedD"
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Калькулятор Дробей 🎛️", fontWeight = FontWeight.Bold, color = MatrixCyan, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Interactive fractions layout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Fraction A Container
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(CarbonDark, RoundedCornerShape(10.dp))
                                    .border(1.dp, MatrixCyan.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Дробь A", color = MatrixCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Whole part column
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(CarbonSurface, CircleShape)
                                                .clickable {
                                                    val v = (fracWhole1Str.toLongOrNull() ?: 0L) + 1L
                                                    fracWhole1Str = v.toString()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("+", color = MatrixCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(36.dp)
                                                .height(28.dp)
                                                .background(CarbonSurface, RoundedCornerShape(4.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            androidx.compose.foundation.text.BasicTextField(
                                                value = fracWhole1Str,
                                                onValueChange = { fracWhole1Str = it },
                                                textStyle = TextStyle(color = CarbonWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp, textAlign = TextAlign.Center),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(CarbonSurface, CircleShape)
                                                .clickable {
                                                    val v = (fracWhole1Str.toLongOrNull() ?: 0L) - 1L
                                                    fracWhole1Str = Math.max(0L, v).toString()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("-", color = MatrixCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text("Целая", color = TextGray, fontSize = 8.sp)
                                    }

                                    // Divider line
                                    Text("•", color = TextGray, fontSize = 12.sp)

                                    // Numerator and Denominator
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        // Num
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(CarbonSurface, CircleShape)
                                                .clickable {
                                                    val v = (fracNum1Str.toLongOrNull() ?: 0L) + 1L
                                                    fracNum1Str = v.toString()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("+", color = MatrixCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(36.dp)
                                                .height(28.dp)
                                                .background(CarbonSurface, RoundedCornerShape(4.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            androidx.compose.foundation.text.BasicTextField(
                                                value = fracNum1Str,
                                                onValueChange = { fracNum1Str = it },
                                                textStyle = TextStyle(color = CarbonWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp, textAlign = TextAlign.Center),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        // Line
                                        Box(modifier = Modifier.width(34.dp).height(2.dp).background(MatrixCyan))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(36.dp)
                                                .height(28.dp)
                                                .background(CarbonSurface, RoundedCornerShape(4.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            androidx.compose.foundation.text.BasicTextField(
                                                value = fracDen1Str,
                                                onValueChange = { fracDen1Str = it },
                                                textStyle = TextStyle(color = CarbonWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp, textAlign = TextAlign.Center),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(CarbonSurface, CircleShape)
                                                .clickable {
                                                    val v = (fracDen1Str.toLongOrNull() ?: 1L) - 1L
                                                    fracDen1Str = Math.max(1L, v).toString()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("-", color = MatrixCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text("Дробная", color = TextGray, fontSize = 8.sp)
                                    }
                                }
                            }

                            // Operator selection
                            Column(
                                modifier = Modifier.padding(horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                listOf("+", "-", "×", "÷").forEach { op ->
                                    val isAct = fracOperator == op
                                    Box(
                                        modifier = Modifier
                                            .padding(vertical = 2.dp)
                                            .size(28.dp)
                                            .background(if (isAct) ScientificOrange else CarbonDark, RoundedCornerShape(6.dp))
                                            .border(1.dp, if (isAct) ScientificOrange else TextGray.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                            .clickable { fracOperator = op },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(op, color = if (isAct) CarbonDark else CarbonWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Fraction B Container
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(CarbonDark, RoundedCornerShape(10.dp))
                                    .border(1.dp, ScientificOrange.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Дробь B", color = ScientificOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Whole part column
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(CarbonSurface, CircleShape)
                                                .clickable {
                                                    val v = (fracWhole2Str.toLongOrNull() ?: 0L) + 1L
                                                    fracWhole2Str = v.toString()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("+", color = ScientificOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(36.dp)
                                                .height(28.dp)
                                                .background(CarbonSurface, RoundedCornerShape(4.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            androidx.compose.foundation.text.BasicTextField(
                                                value = fracWhole2Str,
                                                onValueChange = { fracWhole2Str = it },
                                                textStyle = TextStyle(color = CarbonWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp, textAlign = TextAlign.Center),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(CarbonSurface, CircleShape)
                                                .clickable {
                                                    val v = (fracWhole2Str.toLongOrNull() ?: 0L) - 1L
                                                    fracWhole2Str = Math.max(0L, v).toString()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("-", color = ScientificOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text("Целая", color = TextGray, fontSize = 8.sp)
                                    }

                                    // Divider line
                                    Text("•", color = TextGray, fontSize = 12.sp)

                                    // Numerator and Denominator
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        // Num
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(CarbonSurface, CircleShape)
                                                .clickable {
                                                    val v = (fracNum2Str.toLongOrNull() ?: 0L) + 1L
                                                    fracNum2Str = v.toString()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("+", color = ScientificOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(36.dp)
                                                .height(28.dp)
                                                .background(CarbonSurface, RoundedCornerShape(4.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            androidx.compose.foundation.text.BasicTextField(
                                                value = fracNum2Str,
                                                onValueChange = { fracNum2Str = it },
                                                textStyle = TextStyle(color = CarbonWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp, textAlign = TextAlign.Center),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        // Line
                                        Box(modifier = Modifier.width(34.dp).height(2.dp).background(ScientificOrange))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(36.dp)
                                                .height(28.dp)
                                                .background(CarbonSurface, RoundedCornerShape(4.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            androidx.compose.foundation.text.BasicTextField(
                                                value = fracDen2Str,
                                                onValueChange = { fracDen2Str = it },
                                                textStyle = TextStyle(color = CarbonWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp, textAlign = TextAlign.Center),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(CarbonSurface, CircleShape)
                                                .clickable {
                                                    val v = (fracDen2Str.toLongOrNull() ?: 1L) - 1L
                                                    fracDen2Str = Math.max(1L, v).toString()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("-", color = ScientificOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text("Дробная", color = TextGray, fontSize = 8.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isDivideByZero || d1 == 0L || d2 == 0L) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x33D32F2F), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Ошибка: Деление на ноль или неверный знаменатель!", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else {
                            // Render multi-format results!
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CarbonDark, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Результат вычислений:", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Смешанная дробь:", color = CarbonWhite, fontSize = 12.sp)
                                        val mixedRepresent = buildString {
                                            if (finalSign < 0) append("-")
                                            if (finalWhole > 0 || finalRem == 0L) append(finalWhole)
                                            if (finalRem > 0) {
                                                append(if (finalWhole > 0) " " else "")
                                                append("$finalRem/$simplifiedD")
                                            }
                                        }
                                        Text(mixedRepresent, color = MatrixCyan, fontWeight = FontWeight.Black, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Неправильная дробь:", color = CarbonWhite, fontSize = 12.sp)
                                        Text("$simplifiedN/$simplifiedD", color = ScientificOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Десятичная дробь:", color = CarbonWhite, fontSize = 12.sp)
                                        Text(formatter.format(decimalValue), color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Step-by-step resolution button
                            Button(
                                onClick = { showFracSteps = !showFracSteps },
                                colors = ButtonDefaults.buttonColors(containerColor = CarbonDark),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (showFracSteps) "Скрыть пошаговое решение ▲" else "Показать пошаговое решение ▼",
                                    color = MatrixCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (showFracSteps) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CarbonDark.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .border(1.dp, TextGray.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text("ПОШАГОВЫЙ АЛГОРИТМ:", color = MatrixCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = fractionStepText + simplificationStep,
                                            color = TextGray,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedCategory == "ИМТ") {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Индекс Массы Тела (ИМТ)", fontWeight = FontWeight.Bold, color = MatrixCyan, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Рост (см)", color = TextGray, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = heightStr,
                                    onValueChange = { heightStr = it },
                                    textStyle = TextStyle(color = CarbonWhite, fontFamily = FontFamily.Monospace),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MatrixCyan, unfocusedBorderColor = TextGray),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.testTag("bmi_height")
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Вес (кг)", color = TextGray, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = weightStr,
                                    onValueChange = { weightStr = it },
                                    textStyle = TextStyle(color = CarbonWhite, fontFamily = FontFamily.Monospace),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MatrixCyan, unfocusedBorderColor = TextGray),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.testTag("bmi_weight")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val hM = (heightStr.toDoubleOrNull() ?: 175.0) / 100.0
                        val wKg = weightStr.toDoubleOrNull() ?: 70.0
                        val bmi = if (hM > 0) wKg / (hM * hM) else 0.0

                        val bmiClass = when {
                            bmi < 18.5 -> Pair("Дефицит массы тела", Color(0xFFFFB300))
                            bmi in 18.5..24.9 -> Pair("Нормальный вес", Color(0xFF2E7D32))
                            bmi in 25.0..29.9 -> Pair("Избыточный вес", Color(0xFFF57C00))
                            else -> Pair("Ожирение", Color(0xFFD32F2F))
                        }

                        val minIdeal = 18.5 * hM * hM
                        val maxIdeal = 24.9 * hM * hM

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CarbonDark, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Ваш ИМТ:", color = CarbonWhite, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = formatter.format(bmi),
                                        color = bmiClass.second,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 18.sp
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Категория:", color = TextGray, fontSize = 13.sp)
                                    Text(
                                        text = bmiClass.first,
                                        color = bmiClass.second,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Рекомендуемый здоровый вес для вашего роста: ${formatter.format(minIdeal)} - ${formatter.format(maxIdeal)} кг",
                                    color = TextGray,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Standard Conversion Tab
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (selectedCategory == "Валюта") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Слежение за курсом", color = MatrixCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                IconButton(
                                    onClick = { viewModel.fetchExchangeRates() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Обновить сейчас", tint = MatrixCyan, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(text = currencyStatus, color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Text(text = "Что перевести (Из):", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = inputValueStr,
                                onValueChange = {
                                    if (it.all { char -> char.isDigit() || char == '.' || char == '-' }) {
                                        inputValueStr = it
                                        copiedFeedback = false
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("converter_input_val"),
                                textStyle = TextStyle(color = CarbonWhite, fontFamily = FontFamily.Monospace, fontSize = 16.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MatrixCyan,
                                    unfocusedBorderColor = TextGray,
                                    cursorColor = MatrixCyan
                                ),
                                placeholder = { Text("0.0", color = TextGray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )

                            Box(modifier = Modifier.weight(1.2f)) {
                                Button(
                                    onClick = { fromExpanded = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = CarbonDark),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("converter_from_unit_btn")
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = fromUnit,
                                            color = CarbonWhite,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Выбор меры",
                                            tint = MatrixCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = fromExpanded,
                                    onDismissRequest = { fromExpanded = false },
                                    modifier = Modifier.background(CarbonSurface).border(1.dp, TextGray.copy(alpha = 0.3f))
                                ) {
                                    unitsMap[selectedCategory]?.forEach { u ->
                                        DropdownMenuItem(
                                            text = { Text(u, color = CarbonWhite, fontSize = 12.sp) },
                                            onClick = {
                                                fromUnit = u
                                                fromExpanded = false
                                                copiedFeedback = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = {
                                    val temp = fromUnit
                                    fromUnit = toUnit
                                    toUnit = temp
                                    copiedFeedback = false
                                },
                                modifier = Modifier
                                    .background(MatrixCyan, CircleShape)
                                    .size(36.dp)
                                    .testTag("converter_swap_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Поменять местами",
                                    tint = CarbonDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(text = "Результат перевода (В):", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .background(CarbonDark, RoundedCornerShape(8.dp))
                                    .border(1.dp, TextGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 14.dp)
                            ) {
                                Text(
                                    text = formattedResult,
                                    color = MatrixCyan,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Box(modifier = Modifier.weight(1.2f)) {
                                Button(
                                    onClick = { toExpanded = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = CarbonDark),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("converter_to_unit_btn")
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = toUnit,
                                            color = CarbonWhite,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Выбор меры",
                                            tint = MatrixCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = toExpanded,
                                    onDismissRequest = { toExpanded = false },
                                    modifier = Modifier.background(CarbonSurface).border(1.dp, TextGray.copy(alpha = 0.3f))
                                ) {
                                    unitsMap[selectedCategory]?.forEach { u ->
                                        DropdownMenuItem(
                                            text = { Text(u, color = CarbonWhite, fontSize = 12.sp) },
                                            onClick = {
                                                toUnit = u
                                                toExpanded = false
                                                copiedFeedback = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(formattedResult))
                                copiedFeedback = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (copiedFeedback) Color(0xFF2E7D32) else MatrixCyan
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("converter_copy_btn")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (copiedFeedback) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "Копировать",
                                    tint = if (copiedFeedback) Color.White else CarbonDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (copiedFeedback) "УСПЕШНО СКОПИРОВАНО! ✓" else "СКОПИРОВАТЬ РЕЗУЛЬТАТ",
                                    color = if (copiedFeedback) Color.White else CarbonDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Справочная информация:",
                        fontWeight = FontWeight.Bold,
                        color = CarbonWhite,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when (selectedCategory) {
                            "Длина" -> "Базовой единицей является Метр. Дюйм равен 2.54 см, а Фут приблизительно равен 30.48 см."
                            "Вес" -> "Базовой единицей является Килограмм. Один Фунт (lb) составляет ровно 0.45359237 кг, Тонна равна 1000 кг."
                            "Температура" -> "Поддерживает автоматический пересчет шкал Цельсия, Фаренгейта и Кельвина с учетом смещения и коэффициентов."
                            "Площадь" -> "Базовая единица — Квадратный метр. Один Акр равен 4046.85 м², Гектар равен 10,000 м² (100 на 100 метров)."
                            "Время" -> "Единицы измерения времени приведены к Базовой мере (Секунда). Год рассчитывается как 365 суток."
                            "Данные" -> "Преобразование размеров файлов построено традиционно на основе двоичного шага в 1024 единицы."
                            "Скорость" -> "Базовый коэффициент рассчитан относительно м/с. 1 узел равен 1.852 км/ч."
                            "Валюта" -> "Курсы рассчитываются динамически с помощью бесплатного API. В случае сбоя сети используются предустановленные значения."
                            "Скидки" -> "Калькулятор рассчитывает скидку, итоговый налог с продаж (НДС) и наглядный отчет о сэкономленных средствах."
                            "ИМТ" -> "Индекс Массы Тела (ИМТ) — стандарт ВОЗ. Рекомендуемый вес вычисляется согласно идеальному диапазоны ИМТ от 18.5 до 24.9."
                            "Дроби" -> "Калькулятор дробей выполняет сложение, вычитание, умножение и деление обыкновенных, смешанных и десятичных дробей с НОК и НОД."
                            else -> ""
                        },
                        color = TextGray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EquationsTab() {
    var aStr by remember { mutableStateOf("1") }
    var bStr by remember { mutableStateOf("-5") }
    var cStr by remember { mutableStateOf("6") }

    var copiedRootsFeedback by remember { mutableStateOf(false) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    val a = aStr.toDoubleOrNull() ?: 0.0
    val b = bStr.toDoubleOrNull() ?: 0.0
    val c = cStr.toDoubleOrNull() ?: 0.0

    // Solve logic
    val isQuadratic = a != 0.0
    val isIncomplete = (b == 0.0 || c == 0.0) && isQuadratic

    // Discriminant D
    val d = if (isQuadratic) b * b - 4 * a * c else 0.0

    val symbols = java.text.DecimalFormatSymbols(java.util.Locale.US)
    val formatter = java.text.DecimalFormat("0.####", symbols)

    val equationText = buildString {
        if (a != 0.0) {
            val aF = formatter.format(a)
            append(if (a == 1.0) "x²" else if (a == -1.0) "-x²" else "${aF}x²")
        }
        if (b != 0.0) {
            val bF = formatter.format(Math.abs(b))
            if (b > 0) {
                append(if (a != 0.0) " + " else "")
                append(if (b == 1.0) "x" else "${bF}x")
            } else {
                append(if (a != 0.0) " - " else "-")
                append(if (b == -1.0) "x" else "${bF}x")
            }
        }
        if (c != 0.0) {
            val cF = formatter.format(Math.abs(c))
            if (c > 0) {
                append(if (a != 0.0 || b != 0.0) " + " else "")
                append(cF)
            } else {
                append(if (a != 0.0 || b != 0.0) " - " else "-")
                append(cF)
            }
        }
        if (a == 0.0 && b == 0.0 && c == 0.0) {
            append("0")
        }
        append(" = 0")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Решение квадратных уравнений",
                        fontWeight = FontWeight.Black,
                        color = MatrixCyan,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Введите коэффициенты a, b, c для автоматического вычисления корней (включая комплексные) с подробным описанием решения.",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Коэффициенты уравнения ax² + bx + c = 0",
                        fontWeight = FontWeight.Bold,
                        color = CarbonWhite,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Coefficient a
                        Column(modifier = Modifier.weight(1f)) {
                            Text("a", color = MatrixCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = aStr,
                                onValueChange = {
                                    if (it.isEmpty() || it == "-" || it.all { char -> char.isDigit() || char == '.' || char == '-' }) {
                                        aStr = it
                                        copiedRootsFeedback = false
                                    }
                                },
                                textStyle = TextStyle(color = CarbonWhite, fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MatrixCyan,
                                    unfocusedBorderColor = TextGray
                                ),
                                placeholder = { Text("1", color = TextGray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.testTag("eq_coef_a")
                            )
                        }

                        // Coefficient b
                        Column(modifier = Modifier.weight(1f)) {
                            Text("b", color = ScientificOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = bStr,
                                onValueChange = {
                                    if (it.isEmpty() || it == "-" || it.all { char -> char.isDigit() || char == '.' || char == '-' }) {
                                        bStr = it
                                        copiedRootsFeedback = false
                                    }
                                },
                                textStyle = TextStyle(color = CarbonWhite, fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MatrixCyan,
                                    unfocusedBorderColor = TextGray
                                ),
                                placeholder = { Text("0", color = TextGray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.testTag("eq_coef_b")
                            )
                        }

                        // Coefficient c
                        Column(modifier = Modifier.weight(1f)) {
                            Text("c", color = CarbonWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = cStr,
                                onValueChange = {
                                    if (it.isEmpty() || it == "-" || it.all { char -> char.isDigit() || char == '.' || char == '-' }) {
                                        cStr = it
                                        copiedRootsFeedback = false
                                    }
                                },
                                textStyle = TextStyle(color = CarbonWhite, fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MatrixCyan,
                                    unfocusedBorderColor = TextGray
                                ),
                                placeholder = { Text("0", color = TextGray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.testTag("eq_coef_c")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                aStr = "1"
                                bStr = "-5"
                                cStr = "6"
                                copiedRootsFeedback = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CarbonDark),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Пример 1", fontSize = 11.sp, color = CarbonWhite)
                        }
                        Button(
                            onClick = {
                                aStr = "2"
                                bStr = "0"
                                cStr = "-8"
                                copiedRootsFeedback = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CarbonDark),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Неполн. 1", fontSize = 11.sp, color = CarbonWhite)
                        }
                        Button(
                            onClick = {
                                aStr = "1"
                                bStr = "4"
                                cStr = "5"
                                copiedRootsFeedback = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CarbonDark),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Комплекс.", fontSize = 11.sp, color = CarbonWhite)
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Тип уравнения:", color = TextGray, fontSize = 11.sp)
                    Text(
                        text = if (!isQuadratic) "Линейное уравнение" else if (isIncomplete) "Неполное квадратное уравнение" else "Полное квадратное уравнение",
                        color = if (isQuadratic) MatrixCyan else ScientificOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Распознанное уравнение:", color = TextGray, fontSize = 11.sp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CarbonDark, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = equationText,
                            color = CarbonWhite,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MatrixCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "ПОШАГОВОЕ РЕШЕНИЕ И КОРНИ",
                        fontWeight = FontWeight.Black,
                        color = MatrixCyan,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (aStr.isEmpty() || aStr == "-" || bStr == "-" || cStr == "-") {
                        Text("Введите корректные коэффициенты для решения...", color = TextGray, fontSize = 13.sp)
                    } else if (!isQuadratic) {
                        // Linear bx + c = 0
                        Text("Уравнение ax² + bx + c = 0 вырождается в линейное bx + c = 0:", color = TextGray, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        val bF = formatter.format(b)
                        val cF = formatter.format(c)
                        Text("Решение: ${bF}x + ${cF} = 0", color = CarbonWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                        if (b == 0.0) {
                            if (c == 0.0) {
                                Text("Коэффициенты b = 0 и c = 0. Любое число x является решением (бесконечно корней).", color = Color.Green, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Противоречие: ${cF} = 0. Уравнение не имеет решений.", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            val root = -c / b
                            val rootFormatted = formatter.format(root)
                            Text("Один корень: x = -c / b = -$cF / $bF = $rootFormatted", color = MatrixCyan, fontSize = 15.sp, fontWeight = FontWeight.Black)

                            Spacer(modifier = Modifier.height(12.dp))
                            RootBox(label = "x", value = rootFormatted)
                        }
                    } else {
                        // Quadratic
                        val aF = formatter.format(a)
                        val bF = formatter.format(b)
                        val cF = formatter.format(c)
                        val dF = formatter.format(d)

                        Text("1. Вычисляем дискриминант D:", color = TextGray, fontSize = 12.sp)
                        Text("D = b² - 4ac = ($bF)² - 4 * ($aF) * ($cF)", color = CarbonWhite, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        Text("D = $dF", color = if (d >= 0) MatrixCyan else Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

                        Spacer(modifier = Modifier.height(10.dp))

                        if (d > 0) {
                            Text("D > 0, следовательно уравнение имеет два действительных корня:", color = TextGray, fontSize = 12.sp)
                            val sqrtD = Math.sqrt(d)
                            val sqrtDF = formatter.format(sqrtD)
                            Text("√D ≈ $sqrtDF", color = TextGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)

                            val x1 = (-b + sqrtD) / (2 * a)
                            val x2 = (-b - sqrtD) / (2 * a)
                            val x1F = formatter.format(x1)
                            val x2F = formatter.format(x2)

                            Text("x₁ = (-b + √D) / 2a = (-$bF + $sqrtDF) / (2 * $aF) = $x1F", color = CarbonWhite, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            Text("x₂ = (-b - √D) / 2a = (-$bF - $sqrtDF) / (2 * $aF) = $x2F", color = CarbonWhite, fontSize = 13.sp, fontFamily = FontFamily.Monospace)

                            Spacer(modifier = Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.weight(1f)) { RootBox(label = "x₁", value = x1F) }
                                Box(modifier = Modifier.weight(1f)) { RootBox(label = "x₂", value = x2F) }
                            }
                        } else if (d == 0.0) {
                            Text("D = 0, уравнение имеет один единственный действительный корень (или два совпадающих):", color = TextGray, fontSize = 12.sp)
                            val x = -b / (2 * a)
                            val xF = formatter.format(x)
                            Text("x = -b / 2a = -$bF / (2 * $aF) = $xF", color = CarbonWhite, fontSize = 13.sp, fontFamily = FontFamily.Monospace)

                            Spacer(modifier = Modifier.height(14.dp))
                            RootBox(label = "x", value = xF)
                        } else {
                            Text("D < 0, уравнение имеет два комплексных корня:", color = TextGray, fontSize = 12.sp)
                            val sqrtAbsD = Math.sqrt(-d)
                            val sqrtAbsDF = formatter.format(sqrtAbsD)
                            Text("√|D| ≈ $sqrtAbsDF, корни имеют вид x = ( -b ± i√|D| ) / 2a", color = TextGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)

                            val realPart = -b / (2 * a)
                            val imaginaryPart = sqrtAbsD / (2 * a)
                            val rF = formatter.format(realPart)
                            val iF = formatter.format(Math.abs(imaginaryPart))

                            val x1Str = if (realPart == 0.0) "${iF}i" else "$rF + ${iF}i"
                            val x2Str = if (realPart == 0.0) "-${iF}i" else "$rF - ${iF}i"

                            Spacer(modifier = Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.weight(1f)) { RootBox(label = "x₁ (комплексный)", value = x1Str) }
                                Box(modifier = Modifier.weight(1f)) { RootBox(label = "x₂ (комплексный)", value = x2Str) }
                            }
                        }
                    }

                    if (aStr.isNotEmpty() && aStr != "-" && bStr != "-" && cStr != "-") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val rootsText = if (!isQuadratic) {
                                    val bF = formatter.format(b)
                                    val cF = formatter.format(c)
                                    if (b == 0.0) "Нет решений" else "x = " + formatter.format(-c / b)
                                } else {
                                    val aF = formatter.format(a)
                                    val bF = formatter.format(b)
                                    val cF = formatter.format(c)
                                    val dF = formatter.format(d)
                                    if (d > 0) {
                                        val x1 = (-b + Math.sqrt(d)) / (2 * a)
                                        val x2 = (-b - Math.sqrt(d)) / (2 * a)
                                        "x1 = ${formatter.format(x1)}, x2 = ${formatter.format(x2)}"
                                    } else if (d == 0.0) {
                                        val x = -b / (2 * a)
                                        "x = ${formatter.format(x)}"
                                    } else {
                                        val realPart = -b / (2 * a)
                                        val imaginaryPart = Math.sqrt(-d) / (2 * a)
                                        val rF = formatter.format(realPart)
                                        val iF = formatter.format(Math.abs(imaginaryPart))
                                        "x1 = $rF + ${iF}i, x2 = $rF - ${iF}i"
                                    }
                                }
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(rootsText))
                                copiedRootsFeedback = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (copiedRootsFeedback) Color(0xFF2E7D32) else MatrixCyan
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (copiedRootsFeedback) "КОРНИ СКОПИРОВАНЫ! ✓" else "СКОПИРОВАТЬ ОТВЕТ",
                                color = if (copiedRootsFeedback) Color.White else CarbonDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RootBox(label: String, value: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CarbonDark, RoundedCornerShape(8.dp))
            .border(1.dp, MatrixCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(label, color = MatrixCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(value, color = CarbonWhite, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun GraphTab(viewModel: CalculatorViewModel) {
    val expr by viewModel.graphExpression.collectAsStateWithLifecycle()
    val points by viewModel.graphPoints.collectAsStateWithLifecycle()
    val error by viewModel.graphError.collectAsStateWithLifecycle()

    var scaleX by remember { mutableStateOf(15f) } // Pixels per math unit
    var scaleY by remember { mutableStateOf(15f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CarbonSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Построение графиков функций",
                    fontWeight = FontWeight.Bold,
                    color = MatrixCyan,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = expr,
                    onValueChange = { viewModel.onGraphExprChange(it) },
                    modifier = Modifier.fillMaxWidth().testTag("graph_expression_input"),
                    label = { Text("f(x) =", color = MatrixCyan) },
                    textStyle = TextStyle(color = CarbonWhite, fontFamily = FontFamily.Monospace, fontSize = 16.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MatrixCyan,
                        unfocusedBorderColor = TextGray
                    ),
                    singleLine = true
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        }

        // Canvas coordinate drawing system
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MatrixCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0C))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("graph_canvas")
                ) {
                    val w = size.width
                    val h = size.height

                    val centerX = w / 2
                    val centerY = h / 2

                    // 1. Draw grid lines
                    val stepGrid = 40f
                    // Vertical grid lines
                    var currX = centerX
                    while (currX < w) {
                        drawLine(Color.White.copy(alpha = 0.08f), Offset(currX, 0f), Offset(currX, h), 1f)
                        currX += stepGrid
                    }
                    currX = centerX
                    while (currX > 0) {
                        drawLine(Color.White.copy(alpha = 0.08f), Offset(currX, 0f), Offset(currX, h), 1f)
                        currX -= stepGrid
                    }

                    // Horizontal grid lines
                    var currY = centerY
                    while (currY < h) {
                        drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, currY), Offset(w, currY), 1f)
                        currY += stepGrid
                    }
                    currY = centerY
                    while (currY > 0) {
                        drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, currY), Offset(w, currY), 1f)
                        currY -= stepGrid
                    }

                    // 2. Main axes
                    drawLine(TextGray.copy(alpha = 0.5f), Offset(0f, centerY), Offset(w, centerY), 2.5f) // X-axis
                    drawLine(TextGray.copy(alpha = 0.5f), Offset(centerX, 0f), Offset(centerX, h), 2.5f) // Y-axis

                    // 3. Draw math relation points path
                    if (points.isNotEmpty()) {
                        val path = Path()
                        var first = true

                        for (pt in points) {
                            val px = centerX + pt.first * scaleX
                            val py = centerY - pt.second * scaleY

                            // Don't draw coordinates outside screen bounds to maintain smoothness
                            if (px in -50f..(w + 50f) && py in -50f..(h + 50f)) {
                                                    if (first) {
                                                        path.moveTo(px, py)
                                                        first = false
                                                    } else {
                                                        path.lineTo(px, py)
                                                    }
                                                }
                                            }
                                            drawPath(
                                                path = path,
                                                color = MatrixCyan,
                                                style = Stroke(width = 4.5f)
                                            )
                                        }
                                    }

                                    // Grid math annotations (labels) overlay
                                    Text(
                                        text = "Ось X (от -10 до 10)",
                                        color = TextGray,
                                        fontSize = 11.sp,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 6.dp)
                                    )
                                    Text(
                                        text = "Ось Y",
                                        color = TextGray,
                                        fontSize = 11.sp,
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(start = 6.dp)
                                    )
                                }
                            }

                            // Domain Zoom controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Масштаб: ", color = CarbonWhite, fontSize = 13.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = {
                                            scaleX = maxOf(5f, scaleX - 4f)
                                            scaleY = maxOf(5f, scaleY - 4f)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CarbonSurface),
                                        modifier = Modifier.testTag("btn_zoom_out")
                                    ) {
                                        Icon(imageVector = Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Меньше", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            scaleX = minOf(60f, scaleX + 4f)
                                            scaleY = minOf(60f, scaleY + 4f)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CarbonSurface),
                                        modifier = Modifier.testTag("btn_zoom_in")
                                    ) {
                                        Icon(imageVector = Icons.Default.ZoomIn, contentDescription = "Zoom In")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Больше", fontSize = 12.sp)
                                    }
                                }
                            }
    }
}

@Composable
fun CloudHistoryTab(
    viewModel: CalculatorViewModel,
    onRestoreExpr: (String) -> Unit
) {
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val lastSyncedStr by viewModel.lastSyncedStr.collectAsStateWithLifecycle()
    val histories by viewModel.historyItems.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Sync & Backup Hub Controls
        Card(
            colors = CardDefaults.cardColors(containerColor = CarbonSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Синхронизация и облачное хранилище",
                    fontWeight = FontWeight.Bold,
                    color = MatrixCyan,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isOnline) "Статус облака: СЕТЬ" else "Статус облака: АВТОНОМНО",
                            fontWeight = FontWeight.Bold,
                            color = if (isOnline) Color.Green else Color.Red,
                            fontSize = 12.sp
                        )
                        Text(
                            text = lastSyncedStr.replace("Synced at", "Синхро:").replace("Never synced", "Не синхронизировано"),
                            color = TextGray,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = { viewModel.triggerManualSync() },
                        colors = ButtonDefaults.buttonColors(containerColor = MatrixCyan),
                        enabled = !isSyncing,
                        modifier = Modifier.testTag("btn_force_sync")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CarbonDark)
                        } else {
                            Icon(imageVector = Icons.Default.Sync, contentDescription = "Sync", tint = CarbonDark)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("СИНХР", color = CarbonDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Export Data Controllers
        Card(
            colors = CardDefaults.cardColors(containerColor = CarbonSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Экспорт базы данных вычислений", fontWeight = FontWeight.Bold, color = CarbonWhite, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.exportHistoryToCSV() },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeBg),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_export_csv")
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = "CSV", tint = ScientificOrange)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Экспорт CSV", color = ScientificOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.exportHistoryToPDF() },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeBg),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_export_pdf")
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = ScientificOrange)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Экспорт PDF", color = ScientificOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // History logs view
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Логи истории вычислений", color = CarbonWhite, fontWeight = FontWeight.Black, fontSize = 14.sp)
            TextButton(
                onClick = { viewModel.clearHistory() },
                modifier = Modifier.testTag("btn_clear_history")
            ) {
                Text("Очистить всё", color = Color.Red, fontSize = 12.sp)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = CarbonSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (histories.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.HistoryToggleOff, contentDescription = "Empty", tint = TextGray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("История вычислений пока пуста.", color = TextGray, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(histories, key = { it.id }) { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CarbonDark)
                                .clickable { onRestoreExpr(log.expression) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = log.expression,
                                    color = CarbonWhite,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "= ${log.result}",
                                    color = MatrixCyan,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (log.isSynced) "В облаке" else "В очереди",
                                    color = if (log.isSynced) Color.Green else ScientificOrange,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TasksSchedulesTab(viewModel: CalculatorViewModel) {
    val tasks by viewModel.taskItems.collectAsStateWithLifecycle()

    var taskTitle by remember { mutableStateOf("") }
    var taskDesc by remember { mutableStateOf("") }
    var triggerSecondsStr by remember { mutableStateOf("10") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Планирование умных задач",
                        fontWeight = FontWeight.Bold,
                        color = MatrixCyan,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        modifier = Modifier.fillMaxWidth().testTag("task_title_input"),
                        label = { Text("Заголовок задачи", color = TextGray) },
                        textStyle = TextStyle(color = CarbonWhite),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MatrixCyan,
                            unfocusedBorderColor = TextGray
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = taskDesc,
                        onValueChange = { taskDesc = it },
                        modifier = Modifier.fillMaxWidth().testTag("task_desc_input"),
                        label = { Text("Описание / Математическая задача", color = TextGray) },
                        textStyle = TextStyle(color = CarbonWhite),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MatrixCyan,
                            unfocusedBorderColor = TextGray
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = triggerSecondsStr,
                        onValueChange = { triggerSecondsStr = it },
                        modifier = Modifier.fillMaxWidth().testTag("task_delay_input"),
                        label = { Text("Задержка уведомления (секунды)", color = TextGray) },
                        textStyle = TextStyle(color = CarbonWhite),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MatrixCyan,
                            unfocusedBorderColor = TextGray
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val secs = triggerSecondsStr.toLongOrNull() ?: 10L
                            if (taskTitle.isNotBlank()) {
                                viewModel.addTaskReminder(taskTitle, taskDesc, secs)
                                taskTitle = ""
                                taskDesc = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MatrixCyan),
                        modifier = Modifier.fillMaxWidth().testTag("btn_add_task")
                    ) {
                        Text("ЗАПЛАНИРОВАТЬ НАПОМИНАНИЕ", color = CarbonDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Защита приложения PIN-кодом / Биометрией", fontWeight = FontWeight.Bold, color = CarbonWhite, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    val lockEnabled by viewModel.isLockEnabled.collectAsStateWithLifecycle()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lockEnabled) "Защита экрана активна" else "Экран не защищен",
                            color = if (lockEnabled) MatrixCyan else TextGray,
                            fontSize = 13.sp
                        )
                        Switch(
                            checked = lockEnabled,
                            onCheckedChange = { viewModel.setLockActive(it) },
                            modifier = Modifier.testTag("switch_lock_protection"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MatrixCyan,
                                checkedTrackColor = CyanBg
                            )
                        )
                    }
                }
            }
        }

        item {
            Text("Очередь активных задач", color = CarbonWhite, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }

        if (tasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Нет запланированных напоминаний.", color = TextGray)
                    }
                }
            }
        } else {
            items(tasks, key = { it.id }) { task ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { viewModel.toggleTaskCompleted(task) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MatrixCyan,
                                checkmarkColor = CarbonDark
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                fontWeight = FontWeight.Bold,
                                color = if (task.isCompleted) TextGray else CarbonWhite,
                                fontSize = 15.sp,
                                modifier = Modifier.animateContentSize()
                            )
                            if (task.description.isNotBlank()) {
                                Text(
                                    text = task.description,
                                    color = TextGray,
                                    fontSize = 12.sp
                                )
                            }
                            val dateObj = Date(task.timeMillis)
                            val df = SimpleDateFormat("dd MMM, HH:mm:ss", Locale("ru"))
                            Text(
                                text = "Запуск: ${df.format(dateObj)}",
                                color = ScientificOrange,
                                fontSize = 10.sp
                            )
                        }

                        IconButton(
                            onClick = { viewModel.deleteTask(task) },
                            modifier = Modifier.testTag("btn_delete_task_${task.id}")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// SECURE LOCK OVERLAY COMPOSABLE
// ----------------------------------------------------

@Composable
fun LockOverlayScreen(viewModel: CalculatorViewModel) {
    val enteredPin by viewModel.enteredPin.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60A0A0C))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = "Сканировать отпечаток пальца",
            tint = MatrixCyan,
            modifier = Modifier
                .size(90.dp)
                .clickable { viewModel.simulateFingerprintAuth() }
                .testTag("biometric_fingerprint_touch")
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "БЕЗОПАСНАЯ ЗОНА ЗАЩИЩЕНА БИОМЕТРИЕЙ",
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            color = MatrixCyan,
            letterSpacing = 2.sp
        )
        Text(
            text = "Нажмите на сканер отпечатков или введите PIN-код для доступа к приложению",
            textAlign = TextAlign.Center,
            color = TextGray,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Dots representing inputPIN digits
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 4) {
                val filled = enteredPin.length > i
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            if (filled) MatrixCyan else Color.Transparent,
                            CircleShape
                        )
                        .border(1.5.dp, MatrixCyan, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Custom Numeric Pin Keypad
        val numKeys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "⌫")

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.width(260.dp)
        ) {
            items(numKeys, key = { it }) { key ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(CarbonSurface)
                        .clickable {
                            when (key) {
                                "C" -> { /* do nothing / can be custom reset */ }
                                "⌫" -> viewModel.deletePinDigit()
                                else -> viewModel.onPinDigit(key)
                            }
                        }
                        .testTag("keypad_key_$key")
                ) {
                    Text(
                        text = key,
                        color = if (key == "⌫") ScientificOrange else CarbonWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "(Подсказка: 1234)",
            color = TextGray.copy(alpha = 0.5f),
            fontSize = 11.sp
        )
    }
}
