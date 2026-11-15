package com.example

import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.GameSortOrder
import com.example.input.GlobalKeyboardInputService
import com.example.ui.games.BrickBreakerScreen
import com.example.ui.games.CharacterMovementScreen
import com.example.ui.games.Game2048Screen
import com.example.ui.games.MemoryGameScreen
import com.example.ui.games.SnakeGameScreen
import com.example.ui.screens.ArcadeHomeScreen
import com.example.ui.screens.GameDetailScreen
import com.example.ui.screens.GameSitesScreen
import com.example.ui.screens.KeyBindingsScreen
import com.example.ui.screens.PackageVaultScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.WebPortalScreen
import com.example.ui.theme.ZipPlayTheme
import com.example.ui.viewmodel.ArcadeViewModel

private const val ROUTE_ARCADE = "arcade"
private const val ROUTE_SITES = "sites"
private const val ROUTE_VAULT = "vault"
private const val ROUTE_STATS = "stats"
private const val ROUTE_PLAY = "play/{gameId}"
private const val ROUTE_GAME_DETAIL = "game_detail/{gameId}"
private const val ROUTE_KEY_BINDINGS = "key_bindings"
private const val ROUTE_WEB_PORTAL = "web_portal?url={url}&title={title}&asset={asset}"

private data class TopDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalKeyboardInputService.instance.attachToApplication(application)
        enableEdgeToEdge()
        setContent {
            ZipPlayTheme {
                ZipPlayApp()
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (GlobalKeyboardInputService.instance.onKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZipPlayApp(
    viewModel: ArcadeViewModel = viewModel()
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.statusMessage) {
        val message = uiState.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearStatusMessage()
    }

    val topDestinations = remember {
        listOf(
            TopDestination(
                route = ROUTE_ARCADE,
                labelRes = R.string.nav_arcade,
                selectedIcon = Icons.Filled.SportsEsports,
                unselectedIcon = Icons.Outlined.SportsEsports,
                testTag = "nav_arcade"
            ),
            TopDestination(
                route = ROUTE_SITES,
                labelRes = R.string.nav_sites,
                selectedIcon = Icons.Filled.Public,
                unselectedIcon = Icons.Outlined.Public,
                testTag = "nav_sites"
            ),
            TopDestination(
                route = ROUTE_VAULT,
                labelRes = R.string.nav_vault,
                selectedIcon = Icons.Filled.FolderZip,
                unselectedIcon = Icons.Outlined.FolderZip,
                testTag = "nav_vault"
            ),
            TopDestination(
                route = ROUTE_STATS,
                labelRes = R.string.nav_stats,
                selectedIcon = Icons.Filled.EmojiEvents,
                unselectedIcon = Icons.Outlined.EmojiEvents,
                testTag = "nav_stats"
            )
        )
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showNavigationChrome = currentRoute in setOf(ROUTE_ARCADE, ROUTE_SITES, ROUTE_VAULT, ROUTE_STATS)

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 640.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (showNavigationChrome) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                if (currentRoute == ROUTE_ARCADE) {
                                    Text(
                                        text = when (uiState.sortOrder) {
                                            GameSortOrder.INSTALL_DATE -> "Ordem: Data de Instalação"
                                            GameSortOrder.ALPHABETICAL -> "Ordem: Alfabética"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        actions = {
                            if (currentRoute == ROUTE_ARCADE) {
                                IconButton(
                                    onClick = { navController.navigate(ROUTE_KEY_BINDINGS) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("open_key_bindings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Keyboard,
                                        contentDescription = "Configurar controles de teclado",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Box {
                                    IconButton(
                                        onClick = { sortMenuExpanded = true },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("sort_menu_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.MoreVert,
                                            contentDescription = "Opções de ordenação",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = sortMenuExpanded,
                                        onDismissRequest = { sortMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Data de Instalação") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Filled.Schedule,
                                                    contentDescription = null
                                                )
                                            },
                                            trailingIcon = {
                                                if (uiState.sortOrder == GameSortOrder.INSTALL_DATE) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = "Selecionado",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            },
                                            onClick = {
                                                sortMenuExpanded = false
                                                viewModel.setSortOrder(GameSortOrder.INSTALL_DATE)
                                            },
                                            modifier = Modifier.testTag("sort_by_install_date")
                                        )

                                        DropdownMenuItem(
                                            text = { Text("Ordem Alfabética") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Filled.SortByAlpha,
                                                    contentDescription = null
                                                )
                                            },
                                            trailingIcon = {
                                                if (uiState.sortOrder == GameSortOrder.ALPHABETICAL) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = "Selecionado",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            },
                                            onClick = {
                                                sortMenuExpanded = false
                                                viewModel.setSortOrder(GameSortOrder.ALPHABETICAL)
                                            },
                                            modifier = Modifier.testTag("sort_by_alphabetical")
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
            bottomBar = {
                if (showNavigationChrome && !useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        topDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showNavigationChrome && useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        topDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_ARCADE,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_ARCADE) {
                        ArcadeHomeScreen(
                            uiState = uiState,
                            onFilterChange = viewModel::setFilter,
                            onGenreChange = viewModel::setGenreFilter,
                            onSearchQueryChange = viewModel::setSearchQuery,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onOpenDetails = { gameId -> navController.navigate("game_detail/$gameId") },
                            onPlayGame = { gameId -> navController.navigate("play/$gameId") },
                            onOpenVaultTab = { navigateTopLevel(ROUTE_VAULT) },
                            onDeleteCustomGame = viewModel::deleteCustomGame
                        )
                    }

                    composable(ROUTE_SITES) {
                        GameSitesScreen(
                            uiState = uiState,
                            onCategoryChange = viewModel::setSiteCategoryFilter,
                            onSearchQueryChange = viewModel::setSiteSearchQuery,
                            onToggleSiteFavorite = viewModel::toggleSiteFavorite,
                            onOpenSite = { siteId, url, title ->
                                siteId?.let { viewModel.recordSiteVisit(it) }
                                navController.navigate(
                                    "web_portal?url=${Uri.encode(url)}&title=${Uri.encode(title)}&asset="
                                )
                            },
                            onOpenBundledWebGame = { assetFile, title ->
                                navController.navigate(
                                    "web_portal?url=&title=${Uri.encode(title)}&asset=${Uri.encode(assetFile)}"
                                )
                            },
                            onValidateAndOpenRawUrl = { raw ->
                                val normalized = viewModel.validateAndNormalizeSiteUrl(raw)
                                if (normalized != null) {
                                    val hostTitle = Uri.parse(normalized).host ?: "Site de Jogos"
                                    navController.navigate(
                                        "web_portal?url=${Uri.encode(normalized)}&title=${Uri.encode(hostTitle)}&asset="
                                    )
                                }
                            },
                            onAddCustomSite = viewModel::addCustomSite,
                            onDeleteCustomSite = viewModel::deleteCustomSite
                        )
                    }

                    composable(ROUTE_VAULT) {
                        PackageVaultScreen(
                            uiState = uiState,
                            onPickPackageUri = viewModel::inspectPickedUri,
                            onInspectDemoBundle = viewModel::inspectDemoBundle,
                            onOpenArchive = viewModel::openSavedArchivePreview,
                            onDeleteArchive = viewModel::deleteArchive,
                            onExportBackupUri = viewModel::exportBackupZip,
                            onRestoreBackupUri = viewModel::restoreBackupZip,
                            onDismissInspection = viewModel::dismissInspection,
                            onCatalogInspectedPackage = { pkg ->
                                viewModel.catalogInspectedPackage(pkg)
                                navigateTopLevel(ROUTE_ARCADE)
                            }
                        )
                    }

                    composable(ROUTE_STATS) {
                        StatsScreen(
                            uiState = uiState,
                            onToggleFavorite = viewModel::toggleFavorite
                        )
                    }

                    composable(
                        route = ROUTE_PLAY,
                        arguments = listOf(navArgument("gameId") { type = NavType.StringType })
                    ) { entry ->
                        val gameId = entry.arguments?.getString("gameId") ?: "snake_neon"
                        val gameRecord = uiState.allGames.find { it.id == gameId }
                        val best = gameRecord?.highScore ?: 0
                        val lifecycleOwner = LocalLifecycleOwner.current

                        LaunchedEffect(gameId) {
                            viewModel.markGameLaunched(gameId)
                        }

                        DisposableEffect(lifecycleOwner, gameId) {
                            var segmentStartMs = if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                SystemClock.elapsedRealtime()
                            } else {
                                0L
                            }

                            fun flushActiveSegment() {
                                if (segmentStartMs > 0L) {
                                    val elapsedSeconds = ((SystemClock.elapsedRealtime() - segmentStartMs) / 1000L)
                                        .coerceAtLeast(0L)
                                    if (elapsedSeconds > 0L) {
                                        viewModel.recordPlayDuration(gameId, elapsedSeconds)
                                    }
                                    segmentStartMs = 0L
                                }
                            }

                            val observer = LifecycleEventObserver { _, event ->
                                when (event) {
                                    Lifecycle.Event.ON_RESUME -> {
                                        if (segmentStartMs == 0L) {
                                            segmentStartMs = SystemClock.elapsedRealtime()
                                        }
                                    }
                                    Lifecycle.Event.ON_PAUSE -> flushActiveSegment()
                                    else -> Unit
                                }
                            }
                            lifecycleOwner.lifecycle.addObserver(observer)
                            onDispose {
                                lifecycleOwner.lifecycle.removeObserver(observer)
                                flushActiveSegment()
                            }
                        }

                        when (gameId) {
                            "snake_neon" -> SnakeGameScreen(
                                bestScore = best,
                                onScoreRecorded = { score -> viewModel.recordGameResult("snake_neon", score) },
                                onBack = { navController.popBackStack() }
                            )
                            "brick_zip" -> BrickBreakerScreen(
                                bestScore = best,
                                onScoreRecorded = { score -> viewModel.recordGameResult("brick_zip", score) },
                                onBack = { navController.popBackStack() }
                            )
                            "turbo_2048" -> Game2048Screen(
                                bestScore = best,
                                onScoreRecorded = { score -> viewModel.recordGameResult("turbo_2048", score) },
                                onBack = { navController.popBackStack() }
                            )
                            "pixel_memory" -> MemoryGameScreen(
                                bestScore = best,
                                onScoreRecorded = { score -> viewModel.recordGameResult("pixel_memory", score) },
                                onBack = { navController.popBackStack() }
                            )
                            "cyber_explorer" -> CharacterMovementScreen(
                                bestScore = best,
                                onScoreRecorded = { score -> viewModel.recordGameResult("cyber_explorer", score) },
                                onBack = { navController.popBackStack() }
                            )
                            else -> SnakeGameScreen(
                                bestScore = best,
                                onScoreRecorded = { score -> viewModel.recordGameResult("snake_neon", score) },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }

                    composable(
                        route = ROUTE_GAME_DETAIL,
                        arguments = listOf(navArgument("gameId") { type = NavType.StringType })
                    ) { entry ->
                        val gameId = entry.arguments?.getString("gameId") ?: ""
                        val gameRecord = uiState.allGames.find { it.id == gameId }
                        GameDetailScreen(
                            game = gameRecord,
                            onBack = { navController.popBackStack() },
                            onToggleFavorite = viewModel::toggleFavorite,
                            onPlayGame = { id -> navController.navigate("play/$id") },
                            onInspectInVault = {
                                navController.popBackStack()
                                navigateTopLevel(ROUTE_VAULT)
                            },
                            onDeleteCustomGame = viewModel::deleteCustomGame,
                            onOpenKeyBindings = { navController.navigate(ROUTE_KEY_BINDINGS) }
                        )
                    }

                    composable(ROUTE_KEY_BINDINGS) {
                        KeyBindingsScreen(
                            keyBindings = uiState.keyBindings,
                            onUpdateBinding = viewModel::updateKeyBinding,
                            onResetBindings = viewModel::resetKeyBindings,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = ROUTE_WEB_PORTAL,
                        arguments = listOf(
                            navArgument("url") {
                                type = NavType.StringType
                                defaultValue = ""
                            },
                            navArgument("title") {
                                type = NavType.StringType
                                defaultValue = "Jogos Site"
                            },
                            navArgument("asset") {
                                type = NavType.StringType
                                defaultValue = ""
                            }
                        )
                    ) { entry ->
                        val encodedUrl = entry.arguments?.getString("url").orEmpty()
                        val encodedTitle = entry.arguments?.getString("title").orEmpty()
                        val encodedAsset = entry.arguments?.getString("asset").orEmpty()
                        WebPortalScreen(
                            initialTitle = encodedTitle.ifBlank { "Jogos Site" },
                            externalUrl = encodedUrl.ifBlank { null },
                            bundledAssetFile = encodedAsset.ifBlank { null },
                            onWebGameScore = viewModel::reportWebGameScore,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
