package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "history_items")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "reminder_tasks")
data class ReminderTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val timeMillis: Long,
    val isCompleted: Boolean = false,
    val isSynced: Boolean = false
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_items ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryItem): Long

    @Query("UPDATE history_items SET isSynced = 1")
    suspend fun markAllSynced()

    @Query("DELETE FROM history_items")
    suspend fun clearHistory()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM reminder_tasks ORDER BY timeMillis ASC")
    fun getAllTasks(): Flow<List<ReminderTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ReminderTask): Long

    @Update
    suspend fun updateTask(task: ReminderTask)

    @Delete
    suspend fun deleteTask(task: ReminderTask)

    @Query("UPDATE reminder_tasks SET isSynced = 1")
    suspend fun markAllSynced()
}

@Database(entities = [HistoryItem::class, ReminderTask::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun taskDao(): TaskDao
}
