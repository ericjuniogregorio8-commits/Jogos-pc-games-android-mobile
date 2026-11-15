package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AchievementEntity
import com.example.data.local.GameEntryEntity
import com.example.data.local.PackageArchiveEntity
import com.example.data.local.WebGameSiteEntity
import com.example.data.local.ZipPlayDatabase
import com.example.data.repository.ArcadeFilter
import com.example.data.repository.ArcadeRepository
import com.example.data.repository.GameSortOrder
import com.example.data.zip.InspectedPackage
import com.example.input.GameControlAction
import com.example.input.GlobalKeyboardInputService
import com.example.input.KeyboardControlMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ArcadeUiState(
    val filter: ArcadeFilter = ArcadeFilter.ALL,
    val genreFilter: String = "Todos",
    val sortOrder: GameSortOrder = GameSortOrder.INSTALL_DATE,
    val searchQuery: String = "",
    val displayedGames: List<GameEntryEntity> = emptyList(),
    val allGames: List<GameEntryEntity> = emptyList(),
    val recentlyPlayedGames: List<GameEntryEntity> = emptyList(),
    val archives: List<PackageArchiveEntity> = emptyList(),
    val allWebSites: List<WebGameSiteEntity> = emptyList(),
    val displayedWebSites: List<WebGameSiteEntity> = emptyList(),
    val siteCategoryFilter: String = "Todos",
    val siteSearchQuery: String = "",
    val keyBindings: Map<GameControlAction, Int> = KeyboardControlMapper.DEFAULT_REMAPPABLE_BINDINGS,
    val achievements: List<AchievementEntity> = emptyList(),
    val inspectedPackage: InspectedPackage? = null,
    val isBusy: Boolean = false,
    val statusMessage: String? = null
) {
    val totalGamesCount: Int get() = allGames.size
    val favoriteCount: Int get() = allGames.count { it.isFavorite }
    val totalHighScore: Int get() = allGames.sumOf { it.highScore }
    val totalPlays: Int get() = allGames.sumOf { it.playCount }
    val totalTimePlayedSeconds: Long get() = allGames.sumOf { it.timePlayedSeconds }
    val favoriteSitesCount: Int get() = allWebSites.count { it.isFavorite }
    val unlockedAchievementsCount: Int get() = achievements.count { it.isUnlocked }
}

private data class SiteFilterState(
    val category: String,
    val query: String,
    val sites: List<WebGameSiteEntity>
)

private data class MetaCollectionsState(
    val archives: List<PackageArchiveEntity>,
    val siteState: SiteFilterState,
    val bindings: Map<GameControlAction, Int>,
    val achievements: List<AchievementEntity>
)

@OptIn(ExperimentalCoroutinesApi::class)
class ArcadeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ZipPlayDatabase.getInstance(application)
    private val repository = ArcadeRepository(
        gameDao = database.gameDao(),
        archiveDao = database.packageArchiveDao(),
        webGameSiteDao = database.webGameSiteDao(),
        keyBindingDao = database.keyBindingDao(),
        achievementDao = database.achievementDao()
    )

    private val filterFlow = MutableStateFlow(ArcadeFilter.ALL)
    private val genreFilterFlow = MutableStateFlow("Todos")
    private val sortOrderFlow = MutableStateFlow(GameSortOrder.INSTALL_DATE)
    private val searchQueryFlow = MutableStateFlow("")
    private val siteCategoryFilterFlow = MutableStateFlow("Todos")
    private val siteSearchQueryFlow = MutableStateFlow("")
    private val inspectedPackageFlow = MutableStateFlow<InspectedPackage?>(null)
    private val isBusyFlow = MutableStateFlow(false)
    private val statusMessageFlow = MutableStateFlow<String?>(null)

    private val filteredFromRoomFlow = combine(filterFlow, sortOrderFlow) { filter, sortOrder ->
        filter to sortOrder
    }.flatMapLatest { (filter, sortOrder) ->
        repository.observeGames(filter, sortOrder)
    }

    private val siteStateFlow = combine(
        siteCategoryFilterFlow,
        siteSearchQueryFlow,
        repository.observeWebSites()
    ) { category, query, sites ->
        SiteFilterState(category = category, query = query, sites = sites)
    }

    private val keyBindingsMapFlow = repository.observeKeyBindings().combine(
        MutableStateFlow(KeyboardControlMapper.DEFAULT_REMAPPABLE_BINDINGS)
    ) { saved, defaults ->
        val merged = defaults.toMutableMap()
        saved.forEach { entity ->
            runCatching { GameControlAction.valueOf(entity.actionName) }.getOrNull()?.let { action ->
                merged[action] = entity.keyCode
            }
        }
        GlobalKeyboardInputService.instance.updateCustomBindings(merged)
        merged.toMap()
    }

    private val metaCollectionsFlow = combine(
        repository.observeArchives(),
        siteStateFlow,
        keyBindingsMapFlow,
        repository.observeAchievements()
    ) { archives, siteState, bindings, achievements ->
        MetaCollectionsState(
            archives = archives,
            siteState = siteState,
            bindings = bindings,
            achievements = achievements
        )
    }

    private val gameQueryStateFlow = combine(
        filterFlow,
        genreFilterFlow,
        sortOrderFlow,
        searchQueryFlow
    ) { filter, genre, sort, query ->
        listOf(filter, genre, sort, query)
    }

    val uiState: StateFlow<ArcadeUiState> = combine(
        gameQueryStateFlow,
        filteredFromRoomFlow,
        repository.observeAllGames(),
        metaCollectionsFlow,
        inspectedPackageFlow,
        isBusyFlow,
        statusMessageFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val queryParts = values[0] as List<Any>
        val filter = queryParts[0] as ArcadeFilter
        val genreFilter = queryParts[1] as String
        val sortOrder = queryParts[2] as GameSortOrder
        val query = queryParts[3] as String

        @Suppress("UNCHECKED_CAST")
        val roomGames = values[1] as List<GameEntryEntity>
        @Suppress("UNCHECKED_CAST")
        val allGames = values[2] as List<GameEntryEntity>
        val meta = values[3] as MetaCollectionsState
        val inspected = values[4] as InspectedPackage?
        val busy = values[5] as Boolean
        val status = values[6] as String?

        val normalizedSelectedGenre = normalizeGenreAlias(genreFilter)
        val genreFiltered = if (normalizedSelectedGenre == "todos") {
            roomGames
        } else {
            roomGames.filter { game ->
                normalizeGenreAlias(game.genre) == normalizedSelectedGenre
            }
        }

        val searched = if (query.isBlank()) {
            genreFiltered
        } else {
            val q = normalizeGenreAlias(query.trim())
            val rawQ = query.trim().lowercase()
            genreFiltered.filter {
                it.title.lowercase().contains(rawQ) ||
                    it.genre.lowercase().contains(rawQ) ||
                    normalizeGenreAlias(it.genre).contains(q) ||
                    it.packageType.lowercase().contains(rawQ) ||
                    it.subtitle.lowercase().contains(rawQ)
            }
        }

        val recentlyPlayed = allGames
            .filter { it.lastPlayedAt != null }
            .sortedByDescending { it.lastPlayedAt ?: 0L }
            .take(6)

        val filteredSites = meta.siteState.sites.filter { site ->
            val matchesCategory = when (meta.siteState.category) {
                "Todos" -> true
                "Favoritos" -> site.isFavorite
                else -> site.category.equals(meta.siteState.category, ignoreCase = true)
            }
            val q = meta.siteState.query.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                site.name.lowercase().contains(q) ||
                site.url.lowercase().contains(q) ||
                site.description.lowercase().contains(q) ||
                site.category.lowercase().contains(q)
            matchesCategory && matchesQuery
        }

        ArcadeUiState(
            filter = filter,
            genreFilter = genreFilter,
            sortOrder = sortOrder,
            searchQuery = query,
            displayedGames = searched,
            allGames = allGames,
            recentlyPlayedGames = recentlyPlayed,
            archives = meta.archives,
            allWebSites = meta.siteState.sites,
            displayedWebSites = filteredSites,
            siteCategoryFilter = meta.siteState.category,
            siteSearchQuery = meta.siteState.query,
            keyBindings = meta.bindings,
            achievements = meta.achievements,
            inspectedPackage = inspected,
            isBusy = busy,
            statusMessage = status
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ArcadeUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    fun setFilter(filter: ArcadeFilter) {
        filterFlow.value = filter
    }

    fun setGenreFilter(genre: String) {
        genreFilterFlow.value = genre
    }

    fun setSortOrder(order: GameSortOrder) {
        sortOrderFlow.value = order
        statusMessageFlow.value = when (order) {
            GameSortOrder.INSTALL_DATE -> "Ordenado por Data de Instalação"
            GameSortOrder.ALPHABETICAL -> "Ordenado em Ordem Alfabética"
        }
    }

    fun setSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    fun setSiteCategoryFilter(category: String) {
        siteCategoryFilterFlow.value = category
    }

    fun setSiteSearchQuery(query: String) {
        siteSearchQueryFlow.value = query
    }

    fun toggleSiteFavorite(site: WebGameSiteEntity) {
        viewModelScope.launch {
            val next = !site.isFavorite
            repository.setSiteFavorite(site.id, next)
            statusMessageFlow.value = if (next) {
                "${site.name} fixado nos sites favoritos"
            } else {
                "${site.name} removido dos sites favoritos"
            }
        }
    }

    fun recordSiteVisit(siteId: String) {
        viewModelScope.launch {
            repository.recordSiteVisit(siteId)
        }
    }

    fun addCustomSite(name: String, rawUrl: String, category: String, description: String): Boolean {
        val normalized = ArcadeRepository.normalizeSafeWebUrl(rawUrl)
        if (normalized == null) {
            statusMessageFlow.value = "Informe um endereço válido (ex: poki.com)"
            return false
        }
        viewModelScope.launch {
            val saved = repository.addCustomSite(name, normalized, category, description)
            statusMessageFlow.value = "Site ${saved.name} adicionado ao portal"
        }
        return true
    }

    fun deleteCustomSite(siteId: String) {
        viewModelScope.launch {
            repository.deleteCustomSite(siteId)
            statusMessageFlow.value = "Site removido do portal"
        }
    }

    fun validateAndNormalizeSiteUrl(rawUrl: String): String? {
        val normalized = ArcadeRepository.normalizeSafeWebUrl(rawUrl)
        if (normalized == null) {
            statusMessageFlow.value = "Digite um endereço web válido (ex: poki.com)"
        }
        return normalized
    }

    fun reportWebGameScore(gameTitle: String, score: Int) {
        if (score > 0) {
            statusMessageFlow.value = "$gameTitle: $score pontos registrados na sessão!"
        }
    }

    fun toggleFavorite(game: GameEntryEntity) {
        viewModelScope.launch {
            val nextFavorite = !game.isFavorite
            val unlocked = repository.setFavorite(game.id, nextFavorite)
            statusMessageFlow.value = if (unlocked.isNotEmpty()) {
                "Conquista desbloqueada: ${unlocked.first()}!"
            } else if (nextFavorite) {
                "${game.title} adicionado aos Favoritos"
            } else {
                "${game.title} removido dos Favoritos"
            }
        }
    }

    fun markGameLaunched(gameId: String) {
        viewModelScope.launch {
            val unlocked = repository.markGameLaunched(gameId)
            if (unlocked.isNotEmpty()) {
                statusMessageFlow.value = "Conquista desbloqueada: ${unlocked.first()}!"
            }
        }
    }

    fun recordGameResult(gameId: String, score: Int) {
        viewModelScope.launch {
            val unlocked = repository.recordPlaySession(gameId, score)
            if (unlocked.isNotEmpty()) {
                statusMessageFlow.value = "Conquista desbloqueada: ${unlocked.first()}!"
            }
        }
    }

    fun recordPlayDuration(gameId: String, addedSeconds: Long) {
        if (addedSeconds <= 0L) return
        viewModelScope.launch {
            val unlocked = repository.recordPlayDuration(gameId, addedSeconds)
            if (unlocked.isNotEmpty()) {
                statusMessageFlow.value = "Conquista desbloqueada: ${unlocked.first()}!"
            }
        }
    }

    fun updateKeyBinding(action: GameControlAction, keyCode: Int) {
        viewModelScope.launch {
            repository.updateKeyBinding(action, keyCode)
            statusMessageFlow.value =
                "Tecla ${KeyboardControlMapper.formatKeyCodeLabel(keyCode)} atribuída!"
        }
    }

    fun resetKeyBindings() {
        viewModelScope.launch {
            repository.resetKeyBindings()
            statusMessageFlow.value = "Mapeamento de teclado restaurado para o padrão"
        }
    }

    fun inspectPickedUri(uri: Uri) {
        viewModelScope.launch {
            isBusyFlow.value = true
            val result = repository.inspectAndSavePackage(getApplication(), uri)
            isBusyFlow.value = false
            result.onSuccess { pkg ->
                inspectedPackageFlow.value = pkg
                statusMessageFlow.value = "Pacote ${pkg.fileName} analisado (${pkg.totalEntries} entradas)"
            }.onFailure { err ->
                statusMessageFlow.value = err.message ?: "Falha ao ler arquivo ZIP/APK/EXE."
            }
        }
    }

    fun inspectDemoBundle() {
        viewModelScope.launch {
            isBusyFlow.value = true
            val pkg = repository.createAndInspectDemoBundle()
            isBusyFlow.value = false
            inspectedPackageFlow.value = pkg
            statusMessageFlow.value = "Pacote demo .zip gerado e analisado"
        }
    }

    fun openSavedArchivePreview(archive: PackageArchiveEntity) {
        val entries = archive.entriesPreview
            .lines()
            .filter { it.isNotBlank() }
            .map { line ->
                val parts = line.split('|')
                val path = parts.getOrElse(0) { line }
                val bytes = parts.getOrNull(1)?.toLongOrNull() ?: 0L
                com.example.data.zip.ZipEntrySummary(
                    path = path,
                    uncompressedBytes = bytes,
                    compressedBytes = bytes,
                    isDirectory = false
                )
            }
        inspectedPackageFlow.value = InspectedPackage(
            fileName = archive.fileName,
            format = archive.format,
            totalEntries = archive.totalEntries,
            compressedBytes = archive.compressedBytes,
            uncompressedBytes = archive.uncompressedBytes,
            hasManifest = archive.hasManifest,
            dexCount = archive.dexCount,
            nativeAbis = archive.nativeAbis.split(",").map { it.trim() }.filter { it.isNotBlank() },
            entries = entries,
            containsZipPlayBackup = false
        )
    }

    fun dismissInspection() {
        inspectedPackageFlow.value = null
    }

    fun catalogInspectedPackage(pkg: InspectedPackage) {
        viewModelScope.launch {
            repository.catalogInspectedPackage(pkg)
            inspectedPackageFlow.value = null
            statusMessageFlow.value = "${pkg.fileName} catalogado na aba Jogos"
        }
    }

    fun deleteCustomGame(gameId: String) {
        viewModelScope.launch {
            repository.deleteCustomGame(gameId)
            statusMessageFlow.value = "Pacote removido do catálogo"
        }
    }

    fun deleteArchive(archiveId: Long) {
        viewModelScope.launch {
            repository.deleteArchive(archiveId)
            statusMessageFlow.value = "Registro removido do histórico"
        }
    }

    fun exportBackupZip(uri: Uri) {
        viewModelScope.launch {
            isBusyFlow.value = true
            val result = repository.exportBackupZip(getApplication(), uri)
            isBusyFlow.value = false
            result.onSuccess { count ->
                statusMessageFlow.value = "Backup .zip salvo com $count jogos e favoritos"
            }.onFailure { err ->
                statusMessageFlow.value = err.message ?: "Erro ao exportar backup."
            }
        }
    }

    fun restoreBackupZip(uri: Uri) {
        viewModelScope.launch {
            isBusyFlow.value = true
            val result = repository.restoreBackupFromZip(getApplication(), uri)
            isBusyFlow.value = false
            result.onSuccess { count ->
                statusMessageFlow.value = "$count jogos restaurados do backup .zip"
            }.onFailure { err ->
                statusMessageFlow.value = err.message ?: "Erro ao restaurar backup."
            }
        }
    }

    fun clearStatusMessage() {
        statusMessageFlow.value = null
    }

    companion object {
        fun normalizeGenreAlias(raw: String): String {
            return when (raw.trim().lowercase()) {
                "all", "todos" -> "todos"
                "action", "ação", "acao" -> "ação"
                "emulator", "emulador" -> "emulador"
                "memory", "memória", "memoria" -> "memória"
                "exploration", "exploração", "exploracao" -> "exploração"
                else -> raw.trim().lowercase()
            }
        }
    }
}
