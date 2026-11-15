package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PackageArchiveDao {
    @Query("SELECT * FROM package_archives ORDER BY importedAt DESC")
    fun observeArchives(): Flow<List<PackageArchiveEntity>>

    @Query("SELECT * FROM package_archives ORDER BY importedAt DESC")
    suspend fun getArchivesSnapshot(): List<PackageArchiveEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArchive(archive: PackageArchiveEntity): Long

    @Query("DELETE FROM package_archives WHERE id = :id")
    suspend fun deleteArchiveById(id: Long)
}
