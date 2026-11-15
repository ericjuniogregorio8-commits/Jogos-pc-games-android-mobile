package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "key_bindings")
data class KeyBindingEntity(
    @PrimaryKey val actionName: String,
    val keyCode: Int,
    val updatedAt: Long = System.currentTimeMillis()
)
