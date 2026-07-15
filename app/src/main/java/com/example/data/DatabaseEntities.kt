package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pinned_apps")
data class PinnedAppEntity(
    @PrimaryKey val packageName: String,
    val activityName: String,
    val label: String,
    val orderIndex: Int
)

@Entity(tableName = "hidden_apps")
data class HiddenAppEntity(
    @PrimaryKey val packageName: String
)

@Entity(tableName = "quick_notes")
data class QuickNoteEntity(
    @PrimaryKey val id: Int = 1,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "launcher_settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
