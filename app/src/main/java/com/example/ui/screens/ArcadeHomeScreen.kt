package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.GameEntryEntity
import com.example.data.repository.ArcadeFilter
import com.example.input.GameControlAction
import com.example.input.GlobalKeyboardInputService
import com.example.input.KeyboardInputEffect
import com.example.input.LocalKeyboardInputService
import com.example.ui.theme.ElectricAmber
import com.example.ui.viewmodel.ArcadeUiState

private val LIBRARY_GENRES = listOf(
    "Todos",
    "Ação",
    "Puzzle",
    "Arcade",
    "Memória",
    "Exploração",
    "Emulador",
    "APK",
    "ZIP"
)

@Composable
fun ArcadeHomeScreen(
    uiState: ArcadeUiState,
    onFilterChange: (ArcadeFilter) -> Unit,
    onGenreChange: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleFavorite: (GameEntryEntity) -> Unit,
    onOpenDetails: (String) -> Unit,
    onPlayGame: (String) -> Unit,
    onOpenVaultTab: () -> Unit,
    onDeleteCustomGame: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val inputService = LocalKeyboardInputService.current
    val focusManager = LocalFocusManager.current
    val isKeyboardActive by inputService.isKeyboardActive.collectAsStateWithLifecycle()
    var searchFocused by remember { mutableStateOf(false) }
    var selectedGameIndex by remember { mutableIntStateOf(-1) }
    val gridState = rememberLazyGridState()

    DisposableEffect(inputService) {
        onDispose {
            inputService.setTextInputFocused(false)
        }
    }

    LaunchedEffect(uiState.displayedGames.size) {
        if (selectedGameIndex >= uiState.displayedGames.size) {
            selectedGameIndex = uiState.displayedGames.lastIndex
        }
    }

    LaunchedEffect(selectedGameIndex, isKeyboardActive) {
        if (isKeyboardActive && selectedGameIndex in uiState.displayedGames.indices) {
            // +2 accounts for Hero card and Search/Filter header items
            gridState.animateScrollToItem(selectedGameIndex + 2)
        }
    }

    KeyboardInputEffect(
        priority = GlobalKeyboardInputService.PRIORITY_SCREEN,
        isGameListener = false
    ) { action ->
        if (searchFocused) {
            if (action == GameControlAction.NAV_BACK) {
                focusManager.clearFocus()
                return@KeyboardInputEffect true
            }
            return@KeyboardInputEffect false
        }

        val games = uiState.displayedGames
        when (action) {
            GameControlAction.MOVE_DOWN,
            GameControlAction.MOVE_RIGHT -> {
                if (games.isNotEmpty()) {
                    selectedGameIndex = if (selectedGameIndex !in games.indices) 0
                    else (selectedGameIndex + 1).coerceAtMost(games.lastIndex)
                    true
                } else {
                    false
                }
            }
            GameControlAction.MOVE_UP,
            GameControlAction.MOVE_LEFT -> {
                if (games.isNotEmpty()) {
                    selectedGameIndex = if (selectedGameIndex !in games.indices) 0
                    else (selectedGameIndex - 1).coerceAtLeast(0)
                    true
                } else {
                    false
                }
            }
            GameControlAction.PRIMARY_ACTION -> {
                val target = games.getOrNull(selectedGameIndex)
                if (target != null) {
                    if (target.isBuiltInPlayable) {
                        onPlayGame(target.id)
                    } else {
                        onOpenVaultTab()
                    }
                    true
                } else {
                    false
                }
            }
            GameControlAction.SECONDARY_ACTION -> {
                val target = games.getOrNull(selectedGameIndex)
                if (target != null) {
                    onToggleFavorite(target)
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 620.dp) 2 else 1

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(columns) }) {
                ArcadeHeroCard(
                    totalGames = uiState.totalGamesCount,
                    favoriteCount = uiState.favoriteCount,
                    totalScore = uiState.totalHighScore
                )
            }

            item(span = { GridItemSpan(columns) }) {
                RecentlyPlayedSection(
                    recentGames = uiState.recentlyPlayedGames,
                    onPlayGame = onPlayGame,
                    onOpenDetails = onOpenDetails,
                    onOpenVaultTab = onOpenVaultTab
                )
            }

            item(span = { GridItemSpan(columns) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text(stringResource(R.string.search_games_hint)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Limpar busca"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                searchFocused = focusState.isFocused
                                inputService.setTextInputFocused(focusState.isFocused)
                            }
                            .testTag("search_games_input")
                    )

                    FilterSegmentedRow(
                        activeFilter = uiState.filter,
                        totalCount = uiState.totalGamesCount,
                        favoriteCount = uiState.favoriteCount,
                        onSelectFilter = onFilterChange
                    )

                    GenreFilterRow(
                        selectedGenre = uiState.genreFilter,
                        onSelectGenre = onGenreChange
                    )
                }
            }

            if (uiState.displayedGames.isEmpty()) {
                item(span = { GridItemSpan(columns) }) {
                    EmptyGamesCard(
                        isFavoritesFilter = uiState.filter == ArcadeFilter.FAVORITES,
                        hasQuery = uiState.searchQuery.isNotBlank() || uiState.genreFilter != "Todos",
                        onShowAll = {
                            onSearchQueryChange("")
                            onGenreChange("Todos")
                            onFilterChange(ArcadeFilter.ALL)
                        }
                    )
                }
            } else {
                itemsIndexed(
                    items = uiState.displayedGames,
                    key = { _, item -> item.id }
                ) { index, game ->
                    GameCatalogCard(
                        game = game,
                        isKeyboardSelected = isKeyboardActive && index == selectedGameIndex,
                        onOpenDetails = { onOpenDetails(game.id) },
                        onToggleFavorite = { onToggleFavorite(game) },
                        onPrimaryAction = {
                            if (game.isBuiltInPlayable) {
                                onPlayGame(game.id)
                            } else {
                                onOpenVaultTab()
                            }
                        },
                        onDeleteCustom = if (!game.isBuiltInPlayable) {
                            { onDeleteCustomGame(game.id) }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArcadeHeroCard(
    totalGames: Int,
    favoriteCount: Int,
    totalScore: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_arcade_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.78f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.45f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.hero_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.hero_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.hero_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroStatChip(
                    label = "$totalGames Instalados",
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                HeroStatChip(
                    label = "$favoriteCount Favoritos",
                    accentColor = ElectricAmber,
                    modifier = Modifier.weight(1f)
                )
                HeroStatChip(
                    label = "$totalScore Pts",
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroStatChip(
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FilterSegmentedRow(
    activeFilter: ArcadeFilter,
    totalCount: Int,
    favoriteCount: Int,
    onSelectFilter: (ArcadeFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterPillButton(
            text = "${stringResource(R.string.filter_all)} ($totalCount)",
            selected = activeFilter == ArcadeFilter.ALL,
            leadingStar = false,
            onClick = { onSelectFilter(ArcadeFilter.ALL) },
            modifier = Modifier
                .weight(1f)
                .testTag("filter_all")
        )
        FilterPillButton(
            text = "${stringResource(R.string.filter_favorites)} ($favoriteCount)",
            selected = activeFilter == ArcadeFilter.FAVORITES,
            leadingStar = true,
            onClick = { onSelectFilter(ArcadeFilter.FAVORITES) },
            modifier = Modifier
                .weight(1f)
                .testTag("filter_favorites")
        )
    }
}

@Composable
private fun FilterPillButton(
    text: String,
    selected: Boolean,
    leadingStar: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) {
        if (leadingStar) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    val fg = if (selected) {
        if (leadingStar) ElectricAmber else MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingStar) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GameCatalogCard(
    game: GameEntryEntity,
    isKeyboardSelected: Boolean,
    onOpenDetails: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPrimaryAction: () -> Unit,
    onDeleteCustom: (() -> Unit)?
) {
    val accent = Color(game.accentColorHex)
    val starScale by animateFloatAsState(
        targetValue = if (game.isFavorite) 1.15f else 1f,
        animationSpec = spring(),
        label = "favoriteScale"
    )

    val gameIcon = when (game.genre) {
        "APK" -> Icons.Filled.Android
        "ZIP" -> Icons.Filled.FolderZip
        "Emulador" -> Icons.Filled.Terminal
        else -> Icons.Filled.SportsEsports
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isKeyboardSelected) 6.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onOpenDetails)
            .border(
                width = when {
                    isKeyboardSelected -> 2.dp
                    game.isFavorite -> 1.5.dp
                    else -> 1.dp
                },
                color = when {
                    isKeyboardSelected -> MaterialTheme.colorScheme.primary
                    game.isFavorite -> ElectricAmber.copy(alpha = 0.65f)
                    else -> MaterialTheme.colorScheme.outline
                },
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("game_card_${game.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.16f))
                        .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = gameIcon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = game.packageType,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                        Text(
                            text = "• ${game.versionLabel} • ${game.sizeLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .background(
                            if (game.isFavorite) ElectricAmber.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surfaceContainer
                        )
                        .testTag("favorite_toggle_${game.id}")
                ) {
                    Icon(
                        imageVector = if (game.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = if (game.isFavorite) {
                            "Remover ${game.title} dos favoritos"
                        } else {
                            "Marcar ${game.title} como favorito"
                        },
                        tint = if (game.isFavorite) ElectricAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.scale(starScale)
                    )
                }
            }

            Text(
                text = game.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (game.isBuiltInPlayable) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = ElectricAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Recorde: ${game.highScore}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${game.playCount}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    } else {
                        Text(
                            text = "Arquivo: ${game.sourceArchiveName ?: game.genre}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedVisibility(visible = onDeleteCustom != null) {
                        IconButton(
                            onClick = { onDeleteCustom?.invoke() },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Remover pacote do catálogo",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenDetails,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("details_game_${game.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Ver detalhes de ${game.title}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onPrimaryAction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("play_game_${game.id}")
                    ) {
                        Icon(
                            imageVector = if (game.isBuiltInPlayable) Icons.Filled.PlayArrow else Icons.Filled.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (game.isBuiltInPlayable) {
                                stringResource(R.string.play_now)
                            } else {
                                stringResource(R.string.inspect_package)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGamesCard(
    isFavoritesFilter: Boolean,
    hasQuery: Boolean,
    onShowAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (isFavoritesFilter) Icons.Outlined.StarOutline else Icons.Filled.Search,
            contentDescription = null,
            tint = ElectricAmber,
            modifier = Modifier.size(44.dp)
        )
        Text(
            text = if (isFavoritesFilter && !hasQuery) {
                stringResource(R.string.empty_favorites_title)
            } else {
                "Nenhum jogo encontrado"
            },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (isFavoritesFilter && !hasQuery) {
                stringResource(R.string.empty_favorites_subtitle)
            } else {
                "Tente outro termo de busca ou volte para todos os jogos."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedButton(
            onClick = onShowAll,
            modifier = Modifier.testTag("empty_show_all_button")
        ) {
            Text(stringResource(R.string.show_all_games))
        }
    }
}

@Composable
private fun RecentlyPlayedSection(
    recentGames: List<GameEntryEntity>,
    onPlayGame: (String) -> Unit,
    onOpenDetails: (String) -> Unit,
    onOpenVaultTab: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recently_played_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "JOGADOS RECENTEMENTE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (recentGames.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                Text(
                    text = "Suas partidas recentes aparecerão aqui para acesso rápido assim que você iniciar um jogo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(recentGames, key = { it.id }) { game ->
                    val accent = Color(game.accentColorHex)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .width(210.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onOpenDetails(game.id) }
                            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                            .testTag("recent_game_${game.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(accent.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SportsEsports,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = game.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${game.genre} • ${formatTimePlayed(game.timePlayedSeconds)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (game.isBuiltInPlayable) onPlayGame(game.id) else onOpenVaultTab()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accent,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retomar", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreFilterRow(
    selectedGenre: String,
    onSelectGenre: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LIBRARY_GENRES.forEach { genre ->
            val selected = selectedGenre.equals(genre, ignoreCase = true)
            val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            val fg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .border(
                        width = 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelectGenre(genre) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("genre_filter_$genre"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = genre,
                    style = MaterialTheme.typography.labelLarge,
                    color = fg,
                    maxLines = 1
                )
            }
        }
    }
}
