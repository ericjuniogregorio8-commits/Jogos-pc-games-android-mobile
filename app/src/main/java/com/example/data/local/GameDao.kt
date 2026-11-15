package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM installed_games ORDER BY installedAt DESC, title COLLATE NOCASE ASC")
    fun observeAllGamesByInstallDate(): Flow<List<GameEntryEntity>>

    @Query("SELECT * FROM installed_games ORDER BY title COLLATE NOCASE ASC")
    fun observeAllGamesAlphabetical(): Flow<List<GameEntryEntity>>

    @Query("SELECT * FROM installed_games WHERE isFavorite = 1 ORDER BY installedAt DESC, title COLLATE NOCASE ASC")
    fun observeFavoriteGamesByInstallDate(): Flow<List<GameEntryEntity>>

    @Query("SELECT * FROM installed_games WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeFavoriteGamesAlphabetical(): Flow<List<GameEntryEntity>>

    @Query("SELECT * FROM installed_games WHERE id = :gameId LIMIT 1")
    fun observeGameById(gameId: String): Flow<GameEntryEntity?>

    @Query("SELECT * FROM installed_games ORDER BY title COLLATE NOCASE ASC")
    suspend fun getAllGamesSnapshot(): List<GameEntryEntity>

    @Query("SELECT COUNT(*) FROM installed_games")
    suspend fun countGames(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGamesIfAbsent(games: List<GameEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGame(game: GameEntryEntity)

    @Query("UPDATE installed_games SET isFavorite = :isFavorite WHERE id = :gameId")
    suspend fun updateFavorite(gameId: String, isFavorite: Boolean)

    @Query(
        """
        UPDATE installed_games
        SET highScore = CASE WHEN :score > highScore THEN :score ELSE highScore END,
            playCount = playCount + 1,
            lastPlayedAt = :playedAt
        WHERE id = :gameId
        """
    )
    suspend fun recordPlaySession(gameId: String, score: Int, playedAt: Long)

    @Query("UPDATE installed_games SET lastPlayedAt = :playedAt WHERE id = :gameId")
    suspend fun markGameLaunched(gameId: String, playedAt: Long)

    @Query(
        """
        UPDATE installed_games
        SET timePlayedSeconds = timePlayedSeconds + :addedSeconds,
            lastPlayedAt = :playedAt
        WHERE id = :gameId
        """
    )
    suspend fun addPlayDuration(gameId: String, addedSeconds: Long, playedAt: Long)

    @Query("DELETE FROM installed_games WHERE id = :gameId AND isBuiltInPlayable = 0")
    suspend fun deleteCustomPackageGame(gameId: String)
}
