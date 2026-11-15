package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.AchievementDao
import com.example.data.local.AchievementEntity
import com.example.data.local.GameDao
import com.example.data.local.GameEntryEntity
import com.example.data.local.KeyBindingDao
import com.example.data.local.KeyBindingEntity
import com.example.data.local.PackageArchiveDao
import com.example.data.local.PackageArchiveEntity
import com.example.data.local.WebGameSiteDao
import com.example.data.local.WebGameSiteEntity
import com.example.data.zip.InspectedPackage
import com.example.data.zip.ZipPackageInspector
import com.example.input.GameControlAction
import com.example.input.KeyboardControlMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

enum class ArcadeFilter {
    ALL,
    FAVORITES
}

enum class GameSortOrder {
    INSTALL_DATE,
    ALPHABETICAL
}

class ArcadeRepository(
    private val gameDao: GameDao,
    private val archiveDao: PackageArchiveDao,
    private val webGameSiteDao: WebGameSiteDao? = null,
    private val keyBindingDao: KeyBindingDao? = null,
    private val achievementDao: AchievementDao? = null
) {
    fun observeGames(
        filter: ArcadeFilter,
        sortOrder: GameSortOrder = GameSortOrder.INSTALL_DATE
    ): Flow<List<GameEntryEntity>> = when (filter to sortOrder) {
        ArcadeFilter.ALL to GameSortOrder.INSTALL_DATE -> gameDao.observeAllGamesByInstallDate()
        ArcadeFilter.ALL to GameSortOrder.ALPHABETICAL -> gameDao.observeAllGamesAlphabetical()
        ArcadeFilter.FAVORITES to GameSortOrder.INSTALL_DATE -> gameDao.observeFavoriteGamesByInstallDate()
        ArcadeFilter.FAVORITES to GameSortOrder.ALPHABETICAL -> gameDao.observeFavoriteGamesAlphabetical()
        else -> gameDao.observeAllGamesByInstallDate()
    }

    fun observeAllGames(): Flow<List<GameEntryEntity>> = gameDao.observeAllGamesByInstallDate()

    fun observeGame(gameId: String): Flow<GameEntryEntity?> = gameDao.observeGameById(gameId)

    fun observeArchives(): Flow<List<PackageArchiveEntity>> = archiveDao.observeArchives()

    fun observeWebSites(): Flow<List<WebGameSiteEntity>> =
        webGameSiteDao?.observeAllSites() ?: flowOf(emptyList())

    fun observeKeyBindings(): Flow<List<KeyBindingEntity>> =
        keyBindingDao?.observeAllBindings() ?: flowOf(emptyList())

    fun observeAchievements(): Flow<List<AchievementEntity>> =
        achievementDao?.observeAchievements() ?: flowOf(emptyList())

    suspend fun ensureSeeded() {
        val now = System.currentTimeMillis()
        val defaultGames = listOf(
            GameEntryEntity(
                id = "snake_neon",
                title = "Cobra Neon",
                subtitle = "Arcade clássico com bônus combo e velocidade progressiva",
                genre = "Arcade",
                packageType = "Nativo",
                versionLabel = "v2.4",
                sizeLabel = "1.8 MB",
                isFavorite = true,
                highScore = 0,
                playCount = 0,
                installedAt = now - 40_000L,
                accentColorHex = 0xFF00E5FF,
                isBuiltInPlayable = true,
                description = "Controle a serpente luminosa em uma arena cyber de alta velocidade. Colete núcleos de energia para aumentar seu multiplicador de combo, desbloqueie velocidade progressiva e evite colidir com as bordas ou com a própria cauda. Suporta controles por gesto de deslize, direcional na tela e teclado."
            ),
            GameEntryEntity(
                id = "brick_zip",
                title = "Quebra-Blocos Zip",
                subtitle = "Destrua camadas compactadas com rebote físico de precisão",
                genre = "Ação",
                packageType = "Nativo",
                versionLabel = "v1.9",
                sizeLabel = "2.1 MB",
                isFavorite = false,
                highScore = 0,
                playCount = 0,
                installedAt = now - 30_000L,
                accentColorHex = 0xFFFFB300,
                isBuiltInPlayable = true,
                description = "Descompacte fileiras de blocos blindados usando ângulos estratégicos de rebote na barra magnética. Cada camada destruída libera pontos extras e acelera a esfera de plasma. Arraste horizontalmente para posicionar a barra com precisão milimétrica."
            ),
            GameEntryEntity(
                id = "turbo_2048",
                title = "2048 Turbo",
                subtitle = "Fusão rápida de blocos numéricos com desfazer jogada",
                genre = "Puzzle",
                packageType = "Nativo",
                versionLabel = "v3.0",
                sizeLabel = "1.2 MB",
                isFavorite = false,
                highScore = 0,
                playCount = 0,
                installedAt = now - 20_000L,
                accentColorHex = 0xFF10E89A,
                isBuiltInPlayable = true,
                description = "Combine blocos de potências iguais deslizando em quatro direções até formar o lendário bloco 2048 e além. Inclui botão de desfazer a última jogada, placar instantâneo e animações fluidas para sessões estratégicas rápidas."
            ),
            GameEntryEntity(
                id = "pixel_memory",
                title = "Memória Pixel",
                subtitle = "Encontre pares digitais com multiplicador de sequência",
                genre = "Memória",
                packageType = "Nativo",
                versionLabel = "v1.5",
                sizeLabel = "1.4 MB",
                isFavorite = false,
                highScore = 0,
                playCount = 0,
                installedAt = now - 10_000L,
                accentColorHex = 0xFFFF4D8D,
                isBuiltInPlayable = true,
                description = "Exercite a memória visual revelando pares de ícones retrô em grade neon. Acertos consecutivos acumulam bônus de sequência enquanto o contador de tentativas testa sua precisão e agilidade."
            ),
            GameEntryEntity(
                id = "cyber_explorer",
                title = "Explorador Cyber",
                subtitle = "Movimente o personagem por toque contínuo e colete orbes",
                genre = "Exploração",
                packageType = "Nativo",
                versionLabel = "v1.0",
                sizeLabel = "1.6 MB",
                isFavorite = false,
                highScore = 0,
                playCount = 0,
                installedAt = now,
                accentColorHex = 0xFF00E5FF,
                isBuiltInPlayable = true,
                description = "Navegue pelo setor digital coletando orbes de dados enquanto desvia de obstáculos em movimento. Use o direcional contínuo por toque ou teclas WASD/setas para explorar a arena com controle total."
            )
        )
        gameDao.insertGamesIfAbsent(defaultGames)

        webGameSiteDao?.let { dao ->
            val defaultSites = listOf(
                WebGameSiteEntity(
                    id = "site_poki",
                    name = "Poki",
                    url = "https://poki.com",
                    description = "Milhares de jogos HTML5 gratuitos direto no navegador, sem downloads.",
                    category = "Portais",
                    isFavorite = true,
                    accentColorHex = 0xFF00E5FF,
                    createdAt = now - 80_000L
                ),
                WebGameSiteEntity(
                    id = "site_crazygames",
                    name = "CrazyGames",
                    url = "https://www.crazygames.com",
                    description = "Arcade, ação 3D WebGL, corrida e multiplayer instantâneo no browser.",
                    category = "Portais",
                    isFavorite = true,
                    accentColorHex = 0xFFFFB300,
                    createdAt = now - 70_000L
                ),
                WebGameSiteEntity(
                    id = "site_itch_html5",
                    name = "itch.io HTML5",
                    url = "https://itch.io/games/html5",
                    description = "Jogos independentes criativos feitos pela comunidade para jogar na web.",
                    category = "Indie",
                    isFavorite = false,
                    accentColorHex = 0xFFFF4D8D,
                    createdAt = now - 60_000L
                ),
                WebGameSiteEntity(
                    id = "site_lichess",
                    name = "Lichess",
                    url = "https://lichess.org",
                    description = "Xadrez online rápido, gratuito e sem anúncios com quebra-cabeças diários.",
                    category = "Puzzle & Xadrez",
                    isFavorite = false,
                    accentColorHex = 0xFF10E89A,
                    createdAt = now - 50_000L
                ),
                WebGameSiteEntity(
                    id = "site_friv",
                    name = "Friv Classic",
                    url = "https://www.friv.com",
                    description = "Coletânea clássica de jogos casuais rápidos em grade colorida.",
                    category = "Clássicos",
                    isFavorite = false,
                    accentColorHex = 0xFF00E5FF,
                    createdAt = now - 40_000L
                ),
                WebGameSiteEntity(
                    id = "site_armorgames",
                    name = "Armor Games",
                    url = "https://armorgames.com",
                    description = "Jogos de estratégia, tower defense e RPG de navegador consagrados.",
                    category = "Clássicos",
                    isFavorite = false,
                    accentColorHex = 0xFFFFB300,
                    createdAt = now - 30_000L
                ),
                WebGameSiteEntity(
                    id = "site_gartic",
                    name = "Gartic.io",
                    url = "https://gartic.io",
                    description = "Jogo multiplayer de desenhar e adivinhar em salas temáticas.",
                    category = "Portais",
                    isFavorite = false,
                    accentColorHex = 0xFFFF4D8D,
                    createdAt = now - 20_000L
                ),
                WebGameSiteEntity(
                    id = "site_2048",
                    name = "2048 Web",
                    url = "https://play2048.co",
                    description = "Versão web oficial do clássico puzzle numérico deslizante.",
                    category = "Puzzle & Xadrez",
                    isFavorite = false,
                    accentColorHex = 0xFF10E89A,
                    createdAt = now - 10_000L
                )
            )
            dao.insertSitesIfAbsent(defaultSites)
        }

        achievementDao?.let { dao ->
            val defaultAchievements = listOf(
                AchievementEntity(
                    id = "first_launch",
                    title = "Primeira Partida",
                    description = "Inicie qualquer jogo da biblioteca ZipPlay pela primeira vez.",
                    category = "Iniciante",
                    targetValue = 1,
                    unitLabel = "partida",
                    accentColorHex = 0xFF00E5FF
                ),
                AchievementEntity(
                    id = "genre_explorer",
                    title = "Explorador de Gêneros",
                    description = "Jogue títulos de pelo menos 3 gêneros diferentes da biblioteca.",
                    category = "Exploração",
                    targetValue = 3,
                    unitLabel = "gêneros",
                    accentColorHex = 0xFFFF4D8D
                ),
                AchievementEntity(
                    id = "hours_1",
                    title = "Aquecimento Arcade",
                    description = "Acumule 60 minutos (1 hora) de tempo total jogado.",
                    category = "Tempo",
                    targetValue = 60,
                    unitLabel = "min",
                    accentColorHex = 0xFF10E89A
                ),
                AchievementEntity(
                    id = "hours_10",
                    title = "Primeiras 10 Horas Jogadas",
                    description = "Complete 10 horas (600 minutos) acumuladas nos jogos da biblioteca.",
                    category = "Tempo",
                    targetValue = 600,
                    unitLabel = "min",
                    accentColorHex = 0xFFFFB300
                ),
                AchievementEntity(
                    id = "high_scorer",
                    title = "Caçador de Recordes",
                    description = "Atinja 1.000 pontos combinados nos recordes dos jogos.",
                    category = "Pontuação",
                    targetValue = 1000,
                    unitLabel = "pts",
                    accentColorHex = 0xFFFFB300
                ),
                AchievementEntity(
                    id = "collector_fav",
                    title = "Curador da Biblioteca",
                    description = "Marque pelo menos 3 jogos como favoritos no seu catálogo.",
                    category = "Coleção",
                    targetValue = 3,
                    unitLabel = "favs",
                    accentColorHex = 0xFF00E5FF
                ),
                AchievementEntity(
                    id = "package_inspector",
                    title = "Inspetor ZIP, APK & EXE",
                    description = "Analise seu primeiro pacote .zip, .apk ou .exe no cofre.",
                    category = "Cofre",
                    targetValue = 1,
                    unitLabel = "pacote",
                    accentColorHex = 0xFF10E89A
                )
            )
            dao.insertIfAbsent(defaultAchievements)
        }

        evaluateAchievements()
    }

    suspend fun setSiteFavorite(siteId: String, isFavorite: Boolean) {
        webGameSiteDao?.updateFavorite(siteId, isFavorite)
    }

    suspend fun recordSiteVisit(siteId: String) {
        webGameSiteDao?.incrementVisit(siteId)
    }

    suspend fun addCustomSite(
        name: String,
        url: String,
        category: String,
        description: String
    ): WebGameSiteEntity {
        val cleanName = name.trim().ifBlank { url.removePrefix("https://").substringBefore("/") }
        val normalizedUrl = normalizeSafeWebUrl(url) ?: "https://poki.com"
        val slug = cleanName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "site" }
        val entity = WebGameSiteEntity(
            id = "custom_site_${slug}_${System.currentTimeMillis()}",
            name = cleanName,
            url = normalizedUrl,
            description = description.trim().ifBlank { "Site de jogos adicionado manualmente ao seu portal." },
            category = category.ifBlank { "Personalizado" },
            isFavorite = true,
            isCustom = true,
            accentColorHex = 0xFF00E5FF,
            visitCount = 0,
            createdAt = System.currentTimeMillis()
        )
        webGameSiteDao?.upsertSite(entity)
        return entity
    }

    suspend fun deleteCustomSite(siteId: String) {
        webGameSiteDao?.deleteCustomSite(siteId)
    }

    companion object {
        fun normalizeSafeWebUrl(raw: String): String? {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return null
            val lower = trimmed.lowercase()
            if (lower.startsWith("javascript:") || lower.startsWith("file:") || lower.startsWith("data:")) {
                return null
            }
            val withScheme = when {
                lower.startsWith("https://") -> trimmed
                lower.startsWith("http://") -> "https://" + trimmed.removePrefix("http://").removePrefix("HTTP://")
                else -> "https://$trimmed"
            }
            val uri = Uri.parse(withScheme)
            val host = uri.host
            return if (uri.scheme == "https" && !host.isNullOrBlank() && host.contains('.')) {
                withScheme
            } else {
                null
            }
        }
    }

    suspend fun setFavorite(gameId: String, isFavorite: Boolean): List<String> {
        gameDao.updateFavorite(gameId, isFavorite)
        return evaluateAchievements()
    }

    suspend fun markGameLaunched(gameId: String): List<String> {
        gameDao.markGameLaunched(gameId, System.currentTimeMillis())
        return evaluateAchievements()
    }

    suspend fun recordPlaySession(gameId: String, score: Int): List<String> {
        gameDao.recordPlaySession(
            gameId = gameId,
            score = score.coerceAtLeast(0),
            playedAt = System.currentTimeMillis()
        )
        return evaluateAchievements()
    }

    suspend fun recordPlayDuration(gameId: String, addedSeconds: Long): List<String> {
        if (addedSeconds > 0L) {
            gameDao.addPlayDuration(
                gameId = gameId,
                addedSeconds = addedSeconds,
                playedAt = System.currentTimeMillis()
            )
        }
        return evaluateAchievements()
    }

    suspend fun updateKeyBinding(action: GameControlAction, keyCode: Int) {
        keyBindingDao?.upsertBinding(
            KeyBindingEntity(
                actionName = action.name,
                keyCode = keyCode,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun resetKeyBindings() {
        keyBindingDao?.let { dao ->
            dao.clearAllBindings()
            val defaults = KeyboardControlMapper.DEFAULT_REMAPPABLE_BINDINGS.map { (action, key) ->
                KeyBindingEntity(actionName = action.name, keyCode = key)
            }
            dao.upsertAll(defaults)
        }
    }

    suspend fun evaluateAchievements(): List<String> {
        val dao = achievementDao ?: return emptyList()
        val currentList = dao.getAchievementsSnapshot()
        if (currentList.isEmpty()) return emptyList()

        val games = gameDao.getAllGamesSnapshot()
        val archives = archiveDao.getArchivesSnapshot()
        val totalPlays = games.sumOf { it.playCount }
        val distinctGenresPlayed = games
            .filter { it.playCount > 0 || it.lastPlayedAt != null }
            .map { it.genre.lowercase() }
            .toSet()
            .size
        val totalMinutesPlayed = (games.sumOf { it.timePlayedSeconds } / 60L).toInt()
        val totalHighScore = games.sumOf { it.highScore }
        val favoriteGamesCount = games.count { it.isFavorite }
        val archivesCount = archives.size
        val now = System.currentTimeMillis()
        val newlyUnlocked = mutableListOf<String>()

        val updated = currentList.map { ach ->
            val progress = when (ach.id) {
                "first_launch" -> if (games.any { it.playCount > 0 || it.lastPlayedAt != null }) 1 else totalPlays
                "genre_explorer" -> distinctGenresPlayed
                "hours_1" -> totalMinutesPlayed
                "hours_10" -> totalMinutesPlayed
                "high_scorer" -> totalHighScore
                "collector_fav" -> favoriteGamesCount
                "package_inspector" -> archivesCount
                else -> ach.currentValue
            }.coerceAtMost(ach.targetValue)

            val shouldUnlock = ach.isUnlocked || progress >= ach.targetValue
            if (shouldUnlock && !ach.isUnlocked) {
                newlyUnlocked.add(ach.title)
            }
            ach.copy(
                currentValue = if (shouldUnlock) ach.targetValue else progress,
                isUnlocked = shouldUnlock,
                unlockedAt = if (shouldUnlock) (ach.unlockedAt ?: now) else null
            )
        }
        dao.upsertAll(updated)
        return newlyUnlocked
    }

    suspend fun inspectAndSavePackage(context: Context, uri: Uri): Result<InspectedPackage> {
        val result = ZipPackageInspector.inspectUri(context, uri)
        result.onSuccess { inspected ->
            archiveDao.insertArchive(inspected.toArchiveEntity())
            evaluateAchievements()
        }
        return result
    }

    suspend fun createAndInspectDemoBundle(): InspectedPackage {
        val games = gameDao.getAllGamesSnapshot()
        val bytes = ZipPackageInspector.createDemoGameBundleBytes(games)
        val inspected = ZipPackageInspector.inspectBytes("zipplay_arcade_demo.zip", bytes)
        archiveDao.insertArchive(inspected.toArchiveEntity())
        evaluateAchievements()
        return inspected
    }

    suspend fun catalogInspectedPackage(pkg: InspectedPackage) {
        val cleanName = pkg.fileName
            .substringBeforeLast('.')
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
            .ifBlank { pkg.fileName }
        val slug = pkg.fileName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        val id = "pkg_${slug}_${pkg.totalEntries}"
        val typeLabel = when (pkg.format) {
            "APK" -> "Pacote APK"
            "EXE" -> "Pacote EXE (PC)"
            else -> "Pacote ZIP"
        }
        val genreLabel = when (pkg.format) {
            "APK" -> "APK"
            "EXE" -> "Emulador"
            else -> "ZIP"
        }
        val accent = when (pkg.format) {
            "APK" -> 0xFFFFB300
            "EXE" -> 0xFF10E89A
            else -> 0xFF00E5FF
        }
        val subtitle = buildString {
            when (pkg.format) {
                "EXE" -> {
                    append("${pkg.totalEntries} seções PE")
                    if (pkg.nativeAbis.isNotEmpty()) {
                        append(" • ${pkg.nativeAbis.joinToString("/")}")
                    }
                }
                else -> {
                    append("${pkg.totalEntries} arquivos")
                    if (pkg.format == "APK" && pkg.dexCount > 0) {
                        append(" • ${pkg.dexCount} DEX")
                    }
                    if (pkg.nativeAbis.isNotEmpty()) {
                        append(" • ${pkg.nativeAbis.joinToString("/")}")
                    }
                }
            }
        }
        val fullDescription = buildString {
            if (pkg.format == "EXE") {
                append("Executável Windows PE (${pkg.nativeAbis.joinToString().ifBlank { "PE32" }}) inspecionado em modo seguro somente leitura no cofre ZipPlay. ")
                append("Contém ${pkg.totalEntries} entradas de seções mapeadas (${ZipPackageInspector.formatBytes(pkg.compressedBytes)}).")
            } else {
                append("Pacote ${pkg.format} catalogado no cofre ZipPlay com ${pkg.totalEntries} entradas internas ")
                append("(${ZipPackageInspector.formatBytes(pkg.uncompressedBytes)} originais e taxa de compressão de ${pkg.compressionRatioPercent}%).")
                if (pkg.format == "APK") {
                    append(
                        if (pkg.hasManifest) " Manifesto AndroidManifest.xml verificado, contendo ${pkg.dexCount} arquivo(s) DEX."
                        else " Nenhum AndroidManifest.xml encontrado na raiz do pacote."
                    )
                }
            }
        }
        gameDao.upsertGame(
            GameEntryEntity(
                id = id,
                title = cleanName,
                subtitle = subtitle,
                genre = genreLabel,
                packageType = typeLabel,
                versionLabel = if (pkg.format == "EXE") "PE Header" else "${pkg.compressionRatioPercent}% comp.",
                sizeLabel = ZipPackageInspector.formatBytes(pkg.compressedBytes),
                isFavorite = false,
                highScore = 0,
                playCount = 0,
                accentColorHex = accent,
                isBuiltInPlayable = false,
                sourceArchiveName = pkg.fileName,
                description = fullDescription
            )
        )
    }

    suspend fun deleteCustomGame(gameId: String) {
        gameDao.deleteCustomPackageGame(gameId)
    }

    suspend fun deleteArchive(archiveId: Long) {
        archiveDao.deleteArchiveById(archiveId)
    }

    suspend fun exportBackupZip(context: Context, uri: Uri): Result<Int> {
        val games = gameDao.getAllGamesSnapshot()
        val archives = archiveDao.getArchivesSnapshot()
        return ZipPackageInspector.exportArcadeBackupToUri(context, uri, games, archives)
    }

    suspend fun restoreBackupFromZip(context: Context, uri: Uri): Result<Int> {
        val parsed = ZipPackageInspector.parseBackupFromZipUri(context, uri)
        return parsed.map { states ->
            val currentMap = gameDao.getAllGamesSnapshot().associateBy { it.id }
            var restored = 0
            states.forEach { backup ->
                val existing = currentMap[backup.id]
                if (existing != null) {
                    gameDao.upsertGame(
                        existing.copy(
                            isFavorite = backup.isFavorite,
                            highScore = maxOf(existing.highScore, backup.highScore),
                            playCount = maxOf(existing.playCount, backup.playCount)
                        )
                    )
                    restored++
                }
            }
            restored
        }
    }
}
