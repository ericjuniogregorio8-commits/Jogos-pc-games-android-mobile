package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "package_archives")
data class PackageArchiveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val format: String,
    val totalEntries: Int,
    val compressedBytes: Long,
    val uncompressedBytes: Long,
    val hasManifest: Boolean,
    val dexCount: Int,
    val nativeAbis: String,
    val entriesPreview: String,
    val importedAt: Long = System.currentTimeMillis()
)
