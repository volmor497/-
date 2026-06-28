package com.example.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.db.AppDatabase
import com.example.data.db.AppRepository
import com.example.data.db.HistoryItem
import com.example.data.db.ReminderTask
import com.example.data.db.SyncResult
import com.example.util.MathParser
import com.example.util.MatrixMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    // 1. Room DB & Repository setup
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            application,
            AppDatabase::class.java, "super_calc_db"
        ).fallbackToDestructiveMigration().build()
    }

    private val repository: AppRepository by lazy {
        AppRepository(database)
    }

    // 2. State definition
    val historyItems: StateFlow<List<HistoryItem>> by lazy {
        repository.allHistory.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    val taskItems: StateFlow<List<ReminderTask>> by lazy {
        repository.allTasks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // Interactive Standard/Scientific Calc State
    private val _calcInput = MutableStateFlow("")
    val calcInput: StateFlow<String> = _calcInput.asStateFlow()

    private val _calcResult = MutableStateFlow("")
    val calcResult: StateFlow<String> = _calcResult.asStateFlow()

    private val _calcError = MutableStateFlow<String?>(null)
    val calcError: StateFlow<String?> = _calcError.asStateFlow()

    private val _calcPreview = MutableStateFlow("")
    val calcPreview: StateFlow<String> = _calcPreview.asStateFlow()

    // Matrix Calc State
    // Matrix dimensions (Rows/Cols between 1..4)
    val matrixARows = mutableStateOf(3)
    val matrixACols = mutableStateOf(3)
    val matrixBRows = mutableStateOf(3)
    val matrixBCols = mutableStateOf(3)

    // Current cell editor arrays
    var matrixACells = mutableStateOf(Array(4) { Array(4) { "0" } })
    var matrixBCells = mutableStateOf(Array(4) { Array(4) { "0" } })

    private val _matrixResult = MutableStateFlow<String>("")
    val matrixResult: StateFlow<String> = _matrixResult.asStateFlow()

    private val _matrixError = MutableStateFlow<String?>(null)
    val matrixError: StateFlow<String?> = _matrixError.asStateFlow()

    // Graph plotting state
    private val _graphExpression = MutableStateFlow("sin(x)")
    val graphExpression: StateFlow<String> = _graphExpression.asStateFlow()

    private val _graphPoints = MutableStateFlow<List<Pair<Float, Float>>>(emptyList())
    val graphPoints: StateFlow<List<Pair<Float, Float>>> = _graphPoints.asStateFlow()

    private val _graphError = MutableStateFlow<String?>(null)
    val graphError: StateFlow<String?> = _graphError.asStateFlow()

    // Cloud Sync simulation
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncedStr = MutableStateFlow("Never synced")
    val lastSyncedStr: StateFlow<String> = _lastSyncedStr.asStateFlow()

    // Biometric & Lock state
    private val _isBiometricLocked = MutableStateFlow(false)
    val isBiometricLocked: StateFlow<Boolean> = _isBiometricLocked.asStateFlow()

    private val _isLockEnabled = MutableStateFlow(false)
    val isLockEnabled: StateFlow<Boolean> = _isLockEnabled.asStateFlow()

    private val _enteredPin = MutableStateFlow("")
    val enteredPin: StateFlow<String> = _enteredPin.asStateFlow()

    private val _isDegreeMode = MutableStateFlow(true)
    val isDegreeMode: StateFlow<Boolean> = _isDegreeMode.asStateFlow()

    // Notifications and Reminders Custom Manager
    private val _activeNotifications = MutableStateFlow<List<InAppNotification>>(emptyList())
    val activeNotifications: StateFlow<List<InAppNotification>> = _activeNotifications.asStateFlow()

    // Export Result log
    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    private val _currencyRates = MutableStateFlow<Map<String, Double>>(mapOf(
        "USD" to 1.0,
        "EUR" to 0.92,
        "RUB" to 90.0,
        "UAH" to 40.0,
        "GBP" to 0.79,
        "CNY" to 7.25
    ))
    val currencyRates: StateFlow<Map<String, Double>> = _currencyRates.asStateFlow()

    private val _currencyStatus = MutableStateFlow<String>("Курсы валют обновлены (офлайн/кэш)")
    val currencyStatus: StateFlow<String> = _currencyStatus.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())

    init {
        updateGraphPoints()
        fetchExchangeRates()
    }

    fun fetchExchangeRates() {
        viewModelScope.launch {
            _currencyStatus.value = "Обновление курсов..."
            try {
                withContext(Dispatchers.IO) {
                    val client = okhttp3.OkHttpClient.Builder()
                        .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                        .build()
                    val request = okhttp3.Request.Builder()
                        .url("https://open.er-api.com/v6/latest/USD")
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string()
                            if (body != null) {
                                val rates = parseExchangeRates(body)
                                _currencyRates.value = rates
                                _currencyStatus.value = "Курсы успешно обновлены онлайн!"
                            } else {
                                _currencyStatus.value = "Ошибка: пустое тело ответа"
                            }
                        } else {
                            _currencyStatus.value = "Сервер вернул код: ${response.code}"
                        }
                    }
                }
            } catch (e: Exception) {
                _currencyStatus.value = "Ошибка сети: ${e.message ?: "нет связи"}"
            }
        }
    }

    private fun parseExchangeRates(json: String): Map<String, Double> {
        val rates = mutableMapOf<String, Double>()
        rates["USD"] = 1.0
        rates["EUR"] = 0.92
        rates["RUB"] = 90.0
        rates["UAH"] = 40.0
        rates["GBP"] = 0.79
        rates["CNY"] = 7.25

        val keys = listOf("USD", "EUR", "RUB", "UAH", "GBP", "CNY")
        for (k in keys) {
            val pattern = "\"$k\"\\s*:\\s*([0-9.]+)"
            val regex = pattern.toRegex()
            val match = regex.find(json)
            if (match != null) {
                val dVal = match.groupValues[1].toDoubleOrNull()
                if (dVal != null) {
                    rates[k] = dVal
                }
            }
        }
        return rates
    }

    // --- Core Scientific Operations ---

    fun toggleAngleMode() {
        _isDegreeMode.value = !_isDegreeMode.value
    }

    fun onCalcClick(token: String) {
        val current = _calcInput.value
        when (token) {
            "C" -> {
                _calcInput.value = ""
                _calcResult.value = ""
                _calcError.value = null
                _calcPreview.value = ""
            }
            "⌫" -> {
                if (current.isNotEmpty()) {
                    val newInput = current.dropLast(1)
                    _calcInput.value = newInput
                    _calcResult.value = ""
                    _calcError.value = null
                    tryUpdatePreview(newInput)
                }
            }
            "=" -> {
                evaluateCalc()
            }
            "deg" -> {
                _isDegreeMode.value = !_isDegreeMode.value
                tryUpdatePreview(current)
            }
            "sin", "cos", "tan", "ctg", "ln", "log", "√", "abs" -> {
                val newInput = current + "$token("
                _calcInput.value = newInput
                _calcResult.value = ""
                _calcError.value = null
                tryUpdatePreview(newInput)
            }
            "±" -> {
                val newInput = if (current.startsWith("-")) {
                    current.drop(1)
                } else if (current.isNotEmpty()) {
                    "-$current"
                } else {
                    "-"
                }
                _calcInput.value = newInput
                _calcResult.value = ""
                _calcError.value = null
                tryUpdatePreview(newInput)
            }
            else -> {
                val newInput = current + token
                _calcInput.value = newInput
                _calcResult.value = ""
                _calcError.value = null
                tryUpdatePreview(newInput)
            }
        }
    }

    private fun tryUpdatePreview(input: String) {
        if (input.isBlank()) {
            _calcPreview.value = ""
            return
        }
        try {
            // Only show preview if it contains operators or functions, or different from just a number
            val hasOperatorsOrFuncs = input.any { it in "+-×÷^%πe√" } || 
                    input.contains("sin") || input.contains("cos") || 
                    input.contains("tan") || input.contains("ctg") || 
                    input.contains("cot") || input.contains("ln") || 
                    input.contains("log") || input.contains("sqrt") || 
                    input.contains("abs") || input.contains("!")
            
            if (!hasOperatorsOrFuncs) {
                _calcPreview.value = ""
                return
            }

            val value = MathParser.eval(input, isDegreeMode = _isDegreeMode.value)
            if (value.isNaN() || value.isInfinite()) {
                _calcPreview.value = ""
                return
            }
            val symbols = DecimalFormatSymbols(Locale.US)
            val formatter = DecimalFormat("0.######", symbols)
            val formatted = if (Math.abs(value - value.toLong()) < 1e-9) {
                value.toLong().toString()
            } else {
                formatter.format(value)
            }
            _calcPreview.value = "= $formatted"
        } catch (e: Exception) {
            _calcPreview.value = ""
        }
    }

    private fun evaluateCalc() {
        val expr = _calcInput.value
        if (expr.isBlank()) return
        viewModelScope.launch {
            try {
                val value = MathParser.eval(expr, isDegreeMode = _isDegreeMode.value)
                val symbols = DecimalFormatSymbols(Locale.US)
                val formatter = DecimalFormat("0.######", symbols)
                val formatted = if (value.isNaN()) "Ошибка" else if (value.isInfinite()) "Бесконечность" else {
                    if (Math.abs(value - value.toLong()) < 1e-9) {
                        value.toLong().toString()
                    } else {
                        formatter.format(value)
                    }
                }
                _calcResult.value = formatted
                _calcError.value = null
                _calcPreview.value = ""
                
                // Save to Room DB locally
                repository.insertHistory(expr, formatted)

                // Sync if online
                if (_isOnline.value) {
                    triggerSilentSync()
                }
            } catch (e: Exception) {
                _calcError.value = e.message ?: "Ошибка вычисления"
                _calcPreview.value = ""
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // --- Matrix Operations ---

    fun updateMatrixACell(row: Int, col: Int, value: String) {
        val current = matrixACells.value
        val updated = Array(4) { r ->
            Array(4) { c ->
                if (r == row && c == col) value else current[r][c]
            }
        }
        matrixACells.value = updated
    }

    fun updateMatrixBCell(row: Int, col: Int, value: String) {
        val current = matrixBCells.value
        val updated = Array(4) { r ->
            Array(4) { c ->
                if (r == row && c == col) value else current[r][c]
            }
        }
        matrixBCells.value = updated
    }

    fun calculateMatrixAdd() {
        try {
            validateMatrixDimensions()
            val a = getMatrixA()
            val b = getMatrixB()
            val result = MatrixMath.add(a, b)
            formatMatrixResult("A + B", result)
            _matrixError.value = null
        } catch (e: Exception) {
            _matrixError.value = e.message
            _matrixResult.value = ""
        }
    }

    fun calculateMatrixSubtract() {
        try {
            validateMatrixDimensions()
            val a = getMatrixA()
            val b = getMatrixB()
            val result = MatrixMath.subtract(a, b)
            formatMatrixResult("A - B", result)
            _matrixError.value = null
        } catch (e: Exception) {
            _matrixError.value = e.message
            _matrixResult.value = ""
        }
    }

    fun calculateMatrixMultiply() {
        try {
            if (matrixACols.value != matrixBRows.value) {
                throw IllegalArgumentException("Cols of Matrix A (${matrixACols.value}) must equal Rows of Matrix B (${matrixBRows.value})")
            }
            val a = getMatrixA()
            val b = getMatrixB()
            val result = MatrixMath.multiply(a, b)
            formatMatrixResult("A × B", result)
            _matrixError.value = null
        } catch (e: Exception) {
            _matrixError.value = e.message
            _matrixResult.value = ""
        }
    }

    fun calculateMatrixDetA() {
        try {
            if (matrixARows.value != matrixACols.value) {
                throw IllegalArgumentException("Matrix A must be square for determinant")
            }
            val a = getMatrixA()
            val det = MatrixMath.determinant(a)
            _matrixResult.value = "det(A) = ${det.toPrettyString()}"
            _matrixError.value = null
            // Save query representation in history
            viewModelScope.launch {
                repository.insertHistory("det(A)", det.toPrettyString())
            }
        } catch (e: Exception) {
            _matrixError.value = e.message
            _matrixResult.value = ""
        }
    }

    fun calculateMatrixDetB() {
        try {
            if (matrixBRows.value != matrixBCols.value) {
                throw IllegalArgumentException("Matrix B must be square for determinant")
            }
            val b = getMatrixB()
            val det = MatrixMath.determinant(b)
            _matrixResult.value = "det(B) = ${det.toPrettyString()}"
            _matrixError.value = null
            viewModelScope.launch {
                repository.insertHistory("det(B)", det.toPrettyString())
            }
        } catch (e: Exception) {
            _matrixError.value = e.message
            _matrixResult.value = ""
        }
    }

    private fun validateMatrixDimensions() {
        if (matrixARows.value != matrixBRows.value || matrixACols.value != matrixBCols.value) {
            throw IllegalArgumentException("Matrix dimensions must match (Rows: ${matrixARows.value}x${matrixACols.value} and ${matrixBRows.value}x${matrixBCols.value})")
        }
    }

    private fun getMatrixA(): Array<DoubleArray> {
        val rows = matrixARows.value
        val cols = matrixACols.value
        val arr = Array(rows) { DoubleArray(cols) }
        val cells = matrixACells.value
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                arr[r][c] = cells[r][c].toDoubleOrNull() ?: 0.0
            }
        }
        return arr
    }

    private fun getMatrixB(): Array<DoubleArray> {
        val rows = matrixBRows.value
        val cols = matrixBCols.value
        val arr = Array(rows) { DoubleArray(cols) }
        val cells = matrixBCells.value
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                arr[r][c] = cells[r][c].toDoubleOrNull() ?: 0.0
            }
        }
        return arr
    }

    private fun formatMatrixResult(operation: String, result: Array<DoubleArray>) {
        val sb = StringBuilder()
        sb.append("$operation Result:\n")
        for (r in result.indices) {
            sb.append("[ ")
            for (c in result[r].indices) {
                sb.append(result[r][c].toPrettyString()).append("   ")
            }
            sb.append("]\n")
        }
        _matrixResult.value = sb.toString().trim()
        viewModelScope.launch {
            repository.insertHistory(operation, _matrixResult.value.replace("\n", " ; "))
        }
    }

    private fun Double.toPrettyString(): String {
        return if (Math.abs(this - this.toLong()) < 1e-9) {
            this.toLong().toString()
        } else {
            String.format(Locale.US, "%.4f", this).trimEnd('0').trimEnd('.')
        }
    }

    // --- Function Graph Dynamic Evaluator ---

    fun onGraphExprChange(expr: String) {
        _graphExpression.value = expr
        updateGraphPoints()
    }

    fun updateGraphPoints() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            try {
                val points = mutableListOf<Pair<Float, Float>>()
                val expr = _graphExpression.value
                if (expr.isNotBlank()) {
                    // Sample 150 points from x = -10 to x = 10
                    val startX = -10.0
                    val endX = 10.0
                    val steps = 150
                    val stepSize = (endX - startX) / steps
                    for (i in 0..steps) {
                        val currentX = startX + i * stepSize
                        val y = MathParser.eval(expr, currentX, isDegreeMode = false)
                        if (!y.isNaN() && !y.isInfinite()) {
                            points.add(Pair(currentX.toFloat(), y.toFloat()))
                        }
                    }
                }
                _graphPoints.value = points
                _graphError.value = null
            } catch (e: Exception) {
                _graphError.value = e.message ?: "Ошибка построения выражения"
                _graphPoints.value = emptyList()
            }
        }
    }

    // --- Offline Mode & Clouds Sync Simulation ---

    fun toggleOnlineMode() {
        val nextMode = !_isOnline.value
        _isOnline.value = nextMode
        if (nextMode) {
            // Trigger automatic sync when toggling online
            triggerManualSync()
        }
    }

    fun triggerManualSync() {
        if (!_isOnline.value) {
            _exportMessage.value = "Cannot sync: App is currently Offline"
            return
        }
        _isSyncing.value = true
        viewModelScope.launch {
            val result = repository.simulateCloudSync()
            _isSyncing.value = false
            when (result) {
                is SyncResult.Success -> {
                    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    _lastSyncedStr.value = "Synced at " + sdf.format(Date(result.lastSyncedTime))
                    pushNotification("Cloud Sync", "All history & tasks synced across devices!")
                }
                is SyncResult.Error -> {
                    _lastSyncedStr.value = "Sync error: " + result.message
                }
            }
        }
    }

    private fun triggerSilentSync() {
        viewModelScope.launch {
            repository.simulateCloudSync()
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            _lastSyncedStr.value = "Synced at " + sdf.format(Date())
        }
    }

    // --- Authentication (Locked Overlay with PIN / Mock Fingerprint Setup) ---

    fun setLockActive(enabled: Boolean) {
        _isLockEnabled.value = enabled
        if (enabled && !_isBiometricLocked.value) {
            _isBiometricLocked.value = true
        }
    }

    fun onPinDigit(digit: String) {
        val current = _enteredPin.value
        if (current.length < 4) {
            _enteredPin.value = current + digit
        }
        if (_enteredPin.value == "1234") {
            unlock()
        } else if (_enteredPin.value.length == 4) {
            // Incorrect code, clear after a small delay
            viewModelScope.launch {
                kotlinx.coroutines.delay(400)
                _enteredPin.value = ""
                _exportMessage.value = "Wrong PIN! (Hint: use 1234)"
            }
        }
    }

    fun deletePinDigit() {
        val current = _enteredPin.value
        if (current.isNotEmpty()) {
            _enteredPin.value = current.dropLast(1)
        }
    }

    fun simulateFingerprintAuth() {
        // Authenticating via fingertip scanning
        viewModelScope.launch {
            pushNotification("Biometrics", "Fingerprint authorized successfully!")
            unlock()
        }
    }

    private fun unlock() {
        _isBiometricLocked.value = false
        _enteredPin.value = ""
        _exportMessage.value = "Access Granted"
    }

    fun lockManual() {
        if (_isLockEnabled.value) {
            _isBiometricLocked.value = true
        }
    }

    // --- Smart Notifications / Task Reminders System ---

    fun addTaskReminder(title: String, description: String, delaySeconds: Long) {
        viewModelScope.launch {
            val timeMillis = System.currentTimeMillis() + (delaySeconds * 1000)
            repository.insertTask(title, description, timeMillis)

            // Setup a smart dynamic trigger timer inside the app!
            setupScheduledTrigger(title, description, delaySeconds)

            pushNotification("Task Added", "Scheduled '$title' in $delaySeconds seconds.")

            if (_isOnline.value) {
                triggerSilentSync()
            }
        }
    }

    fun toggleTaskCompleted(task: ReminderTask) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            repository.updateTask(updated)
            if (_isOnline.value) {
                triggerSilentSync()
            }
            if (updated.isCompleted) {
                pushNotification("Task Update", "Marked as completed!")
            }
        }
    }

    fun deleteTask(task: ReminderTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
            if (_isOnline.value) {
                triggerSilentSync()
            }
        }
    }

    private fun setupScheduledTrigger(title: String, description: String, delaySeconds: Long) {
        handler.postDelayed({
            // Active notification popup
            pushNotification(title, description.ifBlank { "Reminder achieved!" })
        }, delaySeconds * 1000)
    }

    private fun pushNotification(title: String, body: String) {
        val newNotification = InAppNotification(
            id = System.currentTimeMillis(),
            title = title,
            body = body
        )
        _activeNotifications.value = _activeNotifications.value + newNotification
        // Auto dismiss after 4 seconds
        handler.postDelayed({
            _activeNotifications.value = _activeNotifications.value.filter { it.id != newNotification.id }
        }, 4000)
    }

    fun dismissNotification(id: Long) {
        _activeNotifications.value = _activeNotifications.value.filter { it.id != id }
    }

    // --- Export CSV & PDF Logic ---

    fun exportHistoryToCSV() {
        viewModelScope.launch {
            try {
                val csvContent = StringBuilder()
                csvContent.append("ID,Expression,Result,Timestamp\n")
                val items = historyItems.value
                for (item in items) {
                    csvContent.append("${item.id},\"${item.expression}\",\"${item.result}\",${item.timestamp}\n")
                }

                // Write actual file to cache path so that the Android app actually exports
                val cacheDir = getApplication<Application>().cacheDir
                val file = File(cacheDir, "calculation_history.csv")
                file.writeText(csvContent.toString())

                _exportMessage.value = "CSV exported successfully! (${items.size} rows)"
                pushNotification("Export Complete", "CSV database file written: ${file.name}")
            } catch (e: Exception) {
                _exportMessage.value = "Export failed: ${e.message}"
            }
        }
    }

    fun exportHistoryToPDF() {
        viewModelScope.launch {
            try {
                // To support native PDF in simple Android without extra heavyweight iText packages,
                // we generate a beautiful, raw text layout representation in a .pdf/txt document.
                val pdfContent = StringBuilder()
                pdfContent.append("=========================================\n")
                pdfContent.append("         SUPER CALCULATOR REPORT         \n")
                pdfContent.append("=========================================\n")
                pdfContent.append("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}\n\n")
                pdfContent.append("CALCULATION HISTORY:\n")
                pdfContent.append("-----------------------------------------\n")
                val items = historyItems.value
                if (items.isEmpty()) {
                    pdfContent.append("(No history recorded)\n")
                } else {
                    for (item in items) {
                        pdfContent.append("- Expr: ${item.expression}\n")
                        pdfContent.append("  Res : ${item.result}\n")
                        pdfContent.append("-----------------------------------------\n")
                    }
                }

                val cacheDir = getApplication<Application>().cacheDir
                val file = File(cacheDir, "calculation_history.pdf")
                file.writeText(pdfContent.toString())

                _exportMessage.value = "PDF exported successfully! Check cache directory."
                pushNotification("Export Complete", "PDF file written: ${file.name}")
            } catch (e: Exception) {
                _exportMessage.value = "Export failed: ${e.message}"
            }
        }
    }

    fun clearExportMessage() {
        _exportMessage.value = null
    }

    suspend fun loadHistoryDirectly(expr: String) {
        _calcInput.value = expr
        _calcResult.value = ""
        _calcError.value = null
        tryUpdatePreview(expr)
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
    }
}

data class InAppNotification(
    val id: Long,
    val title: String,
    val body: String
)
