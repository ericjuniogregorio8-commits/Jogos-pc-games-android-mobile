package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String,
    val targetValue: Int,
    val currentValue: Int = 0,
    val unitLabel: String = "",
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val accentColorHex: Long = 0xFF00E5FF
)
