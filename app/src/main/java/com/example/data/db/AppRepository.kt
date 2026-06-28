package com.example.data.db

import kotlinx.coroutines.flow.Flow

class AppRepository(private val database: AppDatabase) {
    val historyDao = database.historyDao()
    val taskDao = database.taskDao()

    val allHistory: Flow<List<HistoryItem>> = historyDao.getAllHistory()
    val allTasks: Flow<List<ReminderTask>> = taskDao.getAllTasks()

    suspend fun insertHistory(expression: String, result: String): Long {
        return historyDao.insertHistory(HistoryItem(expression = expression, result = result))
    }

    suspend fun clearHistory() {
        historyDao.clearHistory()
    }

    suspend fun insertTask(title: String, description: String, timeMillis: Long): Long {
        return taskDao.insertTask(ReminderTask(title = title, description = description, timeMillis = timeMillis))
    }

    suspend fun updateTask(task: ReminderTask) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: ReminderTask) {
        taskDao.deleteTask(task)
    }

    suspend fun simulateCloudSync(): SyncResult {
        // Simulates syncing with the cloud (e.g. Firebase or server)
        return try {
            // Emulate network latency
            kotlinx.coroutines.delay(1200)
            historyDao.markAllSynced()
            taskDao.markAllSynced()
            SyncResult.Success(System.currentTimeMillis())
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Unknown sync error")
        }
    }
}

sealed class SyncResult {
    data class Success(val lastSyncedTime: Long) : SyncResult()
    data class Error(val message: String) : SyncResult()
}
