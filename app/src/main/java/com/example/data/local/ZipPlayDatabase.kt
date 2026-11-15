package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        GameEntryEntity::class,
        PackageArchiveEntity::class,
        WebGameSiteEntity::class,
        KeyBindingEntity::class,
        AchievementEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class ZipPlayDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun packageArchiveDao(): PackageArchiveDao
    abstract fun webGameSiteDao(): WebGameSiteDao
    abstract fun keyBindingDao(): KeyBindingDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile
        private var instance: ZipPlayDatabase? = null

        fun getInstance(context: Context): ZipPlayDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ZipPlayDatabase::class.java,
                    "zipplay_arcade.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
