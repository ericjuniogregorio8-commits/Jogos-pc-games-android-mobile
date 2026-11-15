package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "installed_games")
data class GameEntryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    val genre: String,
    val packageType: String,
    val versionLabel: String,
    val sizeLabel: String,
    val isFavorite: Boolean = false,
    val highScore: Int = 0,
    val playCount: Int = 0,
    val installedAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long? = null,
    val accentColorHex: Long = 0xFF00E5FF,
    val isBuiltInPlayable: Boolean = true,
    val sourceArchiveName: String? = null,
    val description: String = "",
    val timePlayedSeconds: Long = 0L
)
