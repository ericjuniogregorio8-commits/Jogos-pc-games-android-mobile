package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "web_game_sites")
data class WebGameSiteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val description: String,
    val category: String,
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false,
    val accentColorHex: Long = 0xFF00E5FF,
    val visitCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
