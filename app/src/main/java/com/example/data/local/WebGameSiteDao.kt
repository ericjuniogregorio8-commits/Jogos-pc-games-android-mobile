package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WebGameSiteDao {
    @Query("SELECT * FROM web_game_sites ORDER BY isFavorite DESC, createdAt ASC, name COLLATE NOCASE ASC")
    fun observeAllSites(): Flow<List<WebGameSiteEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSitesIfAbsent(sites: List<WebGameSiteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSite(site: WebGameSiteEntity)

    @Query("UPDATE web_game_sites SET isFavorite = :isFavorite WHERE id = :siteId")
    suspend fun updateFavorite(siteId: String, isFavorite: Boolean)

    @Query("UPDATE web_game_sites SET visitCount = visitCount + 1 WHERE id = :siteId")
    suspend fun incrementVisit(siteId: String)

    @Query("DELETE FROM web_game_sites WHERE id = :siteId AND isCustom = 1")
    suspend fun deleteCustomSite(siteId: String)
}
