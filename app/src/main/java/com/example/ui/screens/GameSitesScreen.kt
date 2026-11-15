package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.WebGameSiteEntity
import com.example.ui.theme.ArcadeMagenta
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.NeonCyan
import com.example.ui.viewmodel.ArcadeUiState

data class BundledWebGame(
    val id: String,
    val title: String,
    val genre: String,
    val description: String,
    val assetFile: String,
    val accent: Color,
    val icon: ImageVector
)

private val BUNDLED_WEB_GAMES = listOf(
    BundledWebGame(
        id = "space_blaster",
        title = "Space Blaster Web",
        genre = "HTML5 • Tiro",
        description = "Navegação espacial por toque com ondas neon e disparos automáticos.",
        assetFile = "space_blaster.html",
        accent = NeonCyan,
        icon = Icons.Filled.RocketLaunch
    ),
    BundledWebGame(
        id = "flappy_cyber",
        title = "Flappy Cyber Web",
        genre = "HTML5 • Reflexo",
        description = "Impulse o drone pelos portais neon com física estilo navegador.",
        assetFile = "flappy_cyber.html",
        accent = ElectricAmber,
        icon = Icons.Filled.Flight
    ),
    BundledWebGame(
        id = "tic_tac_neon",
        title = "Jogo da Velha IA",
        genre = "HTML5 • Estratégia",
        description = "Desafie a inteligência artificial minimax direto no player HTML5.",
        assetFile = "tic_tac_neon.html",
        accent = ArcadeMagenta,
        icon = Icons.Filled.Extension
    )
)

private val SITE_CATEGORIES = listOf(
    "Todos",
    "Favoritos",
    "Portais",
    "Indie",
    "Clássicos",
    "Puzzle & Xadrez"
)

@Composable
fun GameSitesScreen(
    uiState: ArcadeUiState,
    onCategoryChange: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSiteFavorite: (WebGameSiteEntity) -> Unit,
    onOpenSite: (siteId: String?, url: String, title: String) -> Unit,
    onOpenBundledWebGame: (assetFile: String, title: String) -> Unit,
    onValidateAndOpenRawUrl: (String) -> Unit,
    onAddCustomSite: (name: String, url: String, category: String, description: String) -> Boolean,
    onDeleteCustomSite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var customUrlInput by rememberSaveable { mutableStateOf("") }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 620.dp) 2 else 1

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(columns) }) {
                WebPortalHeroCard(
                    totalSites = uiState.allWebSites.size,
                    favoriteSites = uiState.favoriteSitesCount,
                    instantWebGames = BUNDLED_WEB_GAMES.size
                )
            }

            item(span = { GridItemSpan(columns) }) {
                DirectUrlLauncherCard(
                    urlText = customUrlInput,
                    onUrlTextChange = { customUrlInput = it },
                    onLaunch = {
                        onValidateAndOpenRawUrl(customUrlInput)
                    }
                )
            }

            item(span = { GridItemSpan(columns) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "JOGOS WEB INSTANTÂNEOS (HTML5)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Rodam direto no player web integrado, mesmo offline",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(BUNDLED_WEB_GAMES, key = { it.id }) { game ->
                            BundledWebGameCard(
                                game = game,
                                onPlay = { onOpenBundledWebGame(game.assetFile, game.title) }
                            )
                        }
                    }
                }
            }

            item(span = { GridItemSpan(columns) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PORTAIS & SITES DE JOGOS (${uiState.displayedWebSites.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("add_custom_site_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Adicionar Site")
                        }
                    }

                    OutlinedTextField(
                        value = uiState.siteSearchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Filtrar sites por nome ou gênero…") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (uiState.siteSearchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Limpar filtro de sites"
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
                            .testTag("search_sites_input")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SITE_CATEGORIES.forEach { category ->
                            val selected = uiState.siteCategoryFilter == category
                            val isFavTab = category == "Favoritos"
                            val chipBg = if (selected) {
                                if (isFavTab) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                            val chipFg = if (selected) {
                                if (isFavTab) ElectricAmber else MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Row(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(chipBg)
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) chipFg.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onCategoryChange(category) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("site_category_$category"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isFavTab) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = chipFg,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = chipFg
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.displayedWebSites.isEmpty()) {
                item(span = { GridItemSpan(columns) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Nenhum site encontrado",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Limpe a busca ou toque em Adicionar Site para salvar qualquer portal.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = {
                            onSearchQueryChange("")
                            onCategoryChange("Todos")
                        }) {
                            Text("Mostrar todos os sites")
                        }
                    }
                }
            } else {
                items(
                    items = uiState.displayedWebSites,
                    key = { it.id }
                ) { site ->
                    WebSiteCatalogCard(
                        site = site,
                        onToggleFavorite = { onToggleSiteFavorite(site) },
                        onOpen = { onOpenSite(site.id, site.url, site.name) },
                        onDeleteCustom = if (site.isCustom) {
                            { onDeleteCustomSite(site.id) }
                        } else null
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddCustomSiteDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, url, category, desc ->
                if (onAddCustomSite(name, url, category, desc)) {
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
private fun WebPortalHeroCard(
    totalSites: Int,
    favoriteSites: Int,
    instantWebGames: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_web_sites_hero),
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
                text = stringResource(R.string.sites_hero_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.sites_hero_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.sites_hero_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SiteHeroChip("$totalSites Portais", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                SiteHeroChip("$favoriteSites Favoritos", ElectricAmber, Modifier.weight(1f))
                SiteHeroChip("$instantWebGames HTML5", EmeraldPulse, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SiteHeroChip(
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
private fun DirectUrlLauncherCard(
    urlText: String,
    onUrlTextChange: (String) -> Unit,
    onLaunch: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = urlText,
                onValueChange = onUrlTextChange,
                placeholder = { Text(stringResource(R.string.sites_url_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(onGo = { onLaunch() }),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("site_url_input")
            )

            Button(
                onClick = onLaunch,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .height(52.dp)
                    .testTag("open_custom_url_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.OpenInBrowser,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ir")
            }
        }
    }
}

@Composable
private fun BundledWebGameCard(
    game: BundledWebGame,
    onPlay: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .width(248.dp)
            .border(1.dp, game.accent.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .testTag("bundled_web_card_${game.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(game.accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = game.icon,
                        contentDescription = null,
                        tint = game.accent
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = game.genre,
                        style = MaterialTheme.typography.labelSmall,
                        color = game.accent
                    )
                }
            }

            Text(
                text = game.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(
                    containerColor = game.accent,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("play_web_game_${game.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.play_web_game))
            }
        }
    }
}

@Composable
private fun WebSiteCatalogCard(
    site: WebGameSiteEntity,
    onToggleFavorite: () -> Unit,
    onOpen: () -> Unit,
    onDeleteCustom: (() -> Unit)?
) {
    val accent = Color(site.accentColorHex)
    val starScale by animateFloatAsState(
        targetValue = if (site.isFavorite) 1.15f else 1f,
        animationSpec = spring(),
        label = "siteFavScale"
    )
    val domain = remember(site.url) {
        site.url.removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore("/")
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (site.isFavorite) 1.5.dp else 1.dp,
                color = if (site.isFavorite) ElectricAmber.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("web_site_card_${site.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.16f))
                        .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Public,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = site.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${site.category} • $domain",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .background(
                            if (site.isFavorite) ElectricAmber.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surfaceContainer
                        )
                        .testTag("site_favorite_${site.id}")
                ) {
                    Icon(
                        imageVector = if (site.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = if (site.isFavorite) {
                            "Remover ${site.name} dos favoritos"
                        } else {
                            "Marcar ${site.name} como favorito"
                        },
                        tint = if (site.isFavorite) ElectricAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.scale(starScale)
                    )
                }
            }

            Text(
                text = site.description,
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
                Text(
                    text = if (site.visitCount > 0) "Acessado ${site.visitCount}x" else "Portal HTTPS seguro",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedVisibility(visible = onDeleteCustom != null) {
                        IconButton(
                            onClick = { onDeleteCustom?.invoke() },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Remover site personalizado",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Button(
                        onClick = onOpen,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("open_site_${site.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.open_site))
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCustomSiteDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, url: String, category: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Portais") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Adicionar Site de Jogos",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do site") },
                    placeholder = { Text("Ex: Coolmath Games") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Endereço (URL)") },
                    placeholder = { Text("Ex: coolmathgames.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoria") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição (opcional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, url, category, description) },
                enabled = url.isNotBlank()
            ) {
                Text("Salvar Site")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
