package com.example

import android.content.Context
import android.view.KeyEvent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ZipPlayDatabase
import com.example.data.repository.ArcadeFilter
import com.example.data.repository.ArcadeRepository
import com.example.data.zip.ZipPackageInspector
import com.example.input.GameControlAction
import com.example.input.KeyboardControlMapper
import com.example.ui.viewmodel.ArcadeViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: ZipPlayDatabase
    private lateinit var repository: ArcadeRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, ZipPlayDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ArcadeRepository(
            gameDao = db.gameDao(),
            archiveDao = db.packageArchiveDao(),
            webGameSiteDao = db.webGameSiteDao(),
            keyBindingDao = db.keyBindingDao(),
            achievementDao = db.achievementDao()
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `read app name from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("ZipPlay Arcade", appName)
    }

    @Test
    fun `seed games and filter favorites in Room`() = runTest {
        repository.ensureSeeded()

        val allGames = repository.observeGames(ArcadeFilter.ALL).first()
        assertEquals(5, allGames.size)

        val initialFavorites = repository.observeGames(ArcadeFilter.FAVORITES).first()
        assertEquals(1, initialFavorites.size)
        assertEquals("snake_neon", initialFavorites.first().id)

        repository.setFavorite("brick_zip", true)
        val updatedFavorites = repository.observeGames(ArcadeFilter.FAVORITES).first()
        assertEquals(2, updatedFavorites.size)
        assertTrue(updatedFavorites.any { it.id == "brick_zip" })

        repository.setFavorite("snake_neon", false)
        val afterRemoval = repository.observeGames(ArcadeFilter.FAVORITES).first()
        assertEquals(1, afterRemoval.size)
        assertFalse(afterRemoval.any { it.id == "snake_neon" })
    }

    @Test
    fun `demo zip bundle creation and inspection parses valid entries`() = runTest {
        repository.ensureSeeded()
        val games = repository.observeGames(ArcadeFilter.ALL).first()
        val bytes = ZipPackageInspector.createDemoGameBundleBytes(games)
        val inspected = ZipPackageInspector.inspectBytes("demo.zip", bytes)

        assertEquals("ZIP", inspected.format)
        assertTrue(inspected.totalEntries >= 6)
        assertTrue(inspected.entries.any { it.path == "manifest.json" })
        assertTrue(inspected.entries.any { it.path == "README.txt" })
    }

    @Test
    fun `database upgrades cleanly from legacy version 3 without crashing`() = runTest {
        val dbName = "test_upgrade_v3_v4.db"
        context.deleteDatabase(dbName)
        val sqlite = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(
            context.getDatabasePath(dbName).apply { parentFile?.mkdirs() },
            null
        )
        sqlite.execSQL("CREATE TABLE IF NOT EXISTS legacy_table (id INTEGER PRIMARY KEY)")
        sqlite.version = 3
        sqlite.close()

        val upgradedDb = Room.databaseBuilder(context, ZipPlayDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
        val repo = ArcadeRepository(upgradedDb.gameDao(), upgradedDb.packageArchiveDao())
        repo.ensureSeeded()
        val games = repo.observeGames(ArcadeFilter.ALL).first()
        assertEquals(5, games.size)
        upgradedDb.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun `games sort cleanly by install date and alphabetical order in Room`() = runTest {
        repository.ensureSeeded()

        val byInstallDate = repository.observeGames(
            ArcadeFilter.ALL,
            com.example.data.repository.GameSortOrder.INSTALL_DATE
        ).first()
        assertEquals("cyber_explorer", byInstallDate.first().id)
        assertEquals("snake_neon", byInstallDate.last().id)

        val alphabetical = repository.observeGames(
            ArcadeFilter.ALL,
            com.example.data.repository.GameSortOrder.ALPHABETICAL
        ).first()
        assertEquals("2048 Turbo", alphabetical.first().title)
        assertEquals("Quebra-Blocos Zip", alphabetical.last().title)
    }

    @Test
    fun `web game sites seed, favorite toggle, and safe URL normalization work`() = runTest {
        repository.ensureSeeded()

        val sites = repository.observeWebSites().first()
        assertEquals(8, sites.size)
        assertTrue(sites.any { it.id == "site_poki" && it.isFavorite })

        repository.setSiteFavorite("site_lichess", true)
        val updated = repository.observeWebSites().first()
        assertTrue(updated.first { it.id == "site_lichess" }.isFavorite)

        assertEquals("https://poki.com", ArcadeRepository.normalizeSafeWebUrl("poki.com"))
        assertEquals("https://itch.io/games", ArcadeRepository.normalizeSafeWebUrl("http://itch.io/games"))
        assertEquals(null, ArcadeRepository.normalizeSafeWebUrl("javascript:alert(1)"))
        assertEquals(null, ArcadeRepository.normalizeSafeWebUrl("not a url"))
    }

    @Test
    fun `seeded games include metadata size, installation date, and description`() = runTest {
        repository.ensureSeeded()

        val snake = repository.observeGame("snake_neon").first()
        assertTrue(snake != null)
        assertEquals("1.8 MB", snake!!.sizeLabel)
        assertTrue(snake.installedAt > 0L)
        assertTrue(snake.description.isNotBlank())
    }

    @Test
    fun `safe exe PE inspection parses architecture and sections read-only`() {
        val peOffset = 0x80
        val optionalHeaderSize = 0x70
        val sectionOffset = peOffset + 24 + optionalHeaderSize
        val totalSize = sectionOffset + 40
        val buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)

        // MZ header + e_lfanew
        buffer.put(0, 'M'.code.toByte())
        buffer.put(1, 'Z'.code.toByte())
        buffer.putInt(0x3C, peOffset)

        // PE\0\0 + COFF header (x86_64 = 0x8664, 1 section)
        buffer.put(peOffset, 'P'.code.toByte())
        buffer.put(peOffset + 1, 'E'.code.toByte())
        buffer.putShort(peOffset + 4, 0x8664.toShort())
        buffer.putShort(peOffset + 6, 1.toShort())
        buffer.putShort(peOffset + 20, optionalHeaderSize.toShort())

        // Section ".text"
        val secName = ".text".toByteArray(Charsets.US_ASCII)
        secName.forEachIndexed { i, b -> buffer.put(sectionOffset + i, b) }
        buffer.putInt(sectionOffset + 8, 4096)
        buffer.putInt(sectionOffset + 16, 2048)

        val inspected = ZipPackageInspector.inspectBytes("pc_port_game.exe", buffer.array())
        assertEquals("EXE", inspected.format)
        assertEquals(2, inspected.totalEntries)
        assertTrue(inspected.nativeAbis.contains("x86_64 (PE32+)"))
        assertTrue(inspected.entries.any { it.path == ".text" && it.uncompressedBytes == 4096L })
    }

    @Test
    fun `playtime tracking updates Room and unlocks milestone achievements`() = runTest {
        repository.ensureSeeded()

        repository.markGameLaunched("snake_neon")
        repository.recordPlayDuration("snake_neon", 125L)

        val snake = repository.observeGame("snake_neon").first()!!
        assertNotNull(snake.lastPlayedAt)
        assertEquals(125L, snake.timePlayedSeconds)

        // Launch two more distinct genres (Arcade, Ação, Puzzle) -> unlocks genre_explorer
        repository.markGameLaunched("brick_zip")
        repository.markGameLaunched("turbo_2048")

        val achievements = repository.observeAchievements().first()
        val firstLaunch = achievements.first { it.id == "first_launch" }
        val genreExplorer = achievements.first { it.id == "genre_explorer" }
        assertTrue(firstLaunch.isUnlocked)
        assertTrue(genreExplorer.isUnlocked)
    }

    @Test
    fun `custom key bindings persist in Room and remap keyboard actions`() = runTest {
        repository.ensureSeeded()

        repository.updateKeyBinding(GameControlAction.PRIMARY_ACTION, KeyEvent.KEYCODE_J)
        val saved = repository.observeKeyBindings().first()
        val mappedPrimary = saved.first { it.actionName == GameControlAction.PRIMARY_ACTION.name }
        assertEquals(KeyEvent.KEYCODE_J, mappedPrimary.keyCode)

        val customLookup = mapOf(KeyEvent.KEYCODE_J to GameControlAction.PRIMARY_ACTION)
        val action = KeyboardControlMapper.mapKeyCodeToAction(
            keyCode = KeyEvent.KEYCODE_J,
            customBindingsByKeyCode = customLookup
        )
        assertEquals(GameControlAction.PRIMARY_ACTION, action)

        repository.resetKeyBindings()
        val resetBindings = repository.observeKeyBindings().first()
        val defaultPrimary = resetBindings.first { it.actionName == GameControlAction.PRIMARY_ACTION.name }
        assertEquals(KeyEvent.KEYCODE_SPACE, defaultPrimary.keyCode)
    }

    @Test
    fun `genre filter alias normalization handles English and Portuguese labels`() {
        assertEquals("ação", ArcadeViewModel.normalizeGenreAlias("Action"))
        assertEquals("ação", ArcadeViewModel.normalizeGenreAlias("Ação"))
        assertEquals("emulador", ArcadeViewModel.normalizeGenreAlias("Emulator"))
        assertEquals("puzzle", ArcadeViewModel.normalizeGenreAlias("Puzzle"))
        assertEquals("todos", ArcadeViewModel.normalizeGenreAlias("All"))
    }
}
