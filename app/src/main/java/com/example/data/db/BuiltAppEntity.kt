package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "built_apps")
data class BuiltAppEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val category: String, // "Game", "Utility", "Math", "Learning", "Tool"
    val iconKey: String, // icon identifier
    val colorHex: String,
    val sourceCode: String,
    val displayMode: String, // "TERMINAL", "GUI_FORM"
    val createdAt: Long = System.currentTimeMillis(),
    val lastRunAt: Long = System.currentTimeMillis(),
    val runCount: Int = 0
)
