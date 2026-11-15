package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyBindingDao {
    @Query("SELECT * FROM key_bindings ORDER BY actionName ASC")
    fun observeAllBindings(): Flow<List<KeyBindingEntity>>

    @Query("SELECT * FROM key_bindings")
    suspend fun getAllBindingsSnapshot(): List<KeyBindingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBinding(binding: KeyBindingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(bindings: List<KeyBindingEntity>)

    @Query("DELETE FROM key_bindings")
    suspend fun clearAllBindings()
}
