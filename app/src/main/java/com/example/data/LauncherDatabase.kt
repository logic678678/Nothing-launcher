package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface LauncherDao {
    // Pinned Apps queries
    @Query("SELECT * FROM pinned_apps ORDER BY orderIndex ASC")
    fun getPinnedAppsFlow(): Flow<List<PinnedAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPinnedApp(app: PinnedAppEntity)

    @Query("DELETE FROM pinned_apps WHERE packageName = :packageName")
    suspend fun deletePinnedApp(packageName: String)

    @Query("DELETE FROM pinned_apps")
    suspend fun clearPinnedApps()

    // Hidden Apps queries
    @Query("SELECT * FROM hidden_apps")
    fun getHiddenAppsFlow(): Flow<List<HiddenAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHiddenApp(app: HiddenAppEntity)

    @Query("DELETE FROM hidden_apps WHERE packageName = :packageName")
    suspend fun deleteHiddenApp(packageName: String)

    // Quick Notes queries
    @Query("SELECT * FROM quick_notes WHERE id = 1 LIMIT 1")
    fun getQuickNoteFlow(): Flow<QuickNoteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuickNote(note: QuickNoteEntity)

    // Todo queries
    @Query("SELECT * FROM todo_items ORDER BY timestamp DESC")
    fun getTodoItemsFlow(): Flow<List<TodoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodoItem(item: TodoEntity)

    @Query("DELETE FROM todo_items WHERE id = :id")
    suspend fun deleteTodoItem(id: Long)

    @Query("DELETE FROM todo_items WHERE isCompleted = 1")
    suspend fun clearCompletedTodoItems()

    // Settings queries
    @Query("SELECT * FROM launcher_settings")
    fun getAllSettingsFlow(): Flow<List<SettingEntity>>

    @Query("SELECT * FROM launcher_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingByKey(key: String): SettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SettingEntity)
}

@Database(
    entities = [
        PinnedAppEntity::class,
        HiddenAppEntity::class,
        QuickNoteEntity::class,
        SettingEntity::class,
        TodoEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun launcherDao(): LauncherDao

    companion object {
        @Volatile
        private var INSTANCE: LauncherDatabase? = null

        fun getDatabase(context: Context): LauncherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LauncherDatabase::class.java,
                    "nothing_launcher_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
