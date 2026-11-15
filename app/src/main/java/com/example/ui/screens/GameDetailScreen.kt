package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.GameEntryEntity
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GameDetailScreen(
    game: GameEntryEntity?,
    onBack: () -> Unit,
    onToggleFavorite: (GameEntryEntity) -> Unit,
    onPlayGame: (String) -> Unit,
    onInspectInVault: () -> Unit,
    onDeleteCustomGame: (String) -> Unit,
    onOpenKeyBindings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = game?.title ?: "Detalhes do Jogo",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar para a biblioteca"
                        )
                    }
                },
                actions = {
                    if (game != null) {
                        IconButton(
                            onClick = { onToggleFavorite(game) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("detail_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (game.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = if (game.isFavorite) {
                                    "Remover ${game.title} dos favoritos"
                                } else {
                                    "Adicionar ${game.title} aos favoritos"
                                },
                                tint = if (game.isFavorite) ElectricAmber else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (game == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "Jogo não encontrado na biblioteca",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    OutlinedButton(onClick = onBack) {
                        Text("Voltar à biblioteca")
                    }
                }
            }
            return@Scaffold
        }

        val accent = Color(game.accentColorHex)
        val installedDateText = remember(game.installedAt) {
            formatTimestamp(game.installedAt)
        }
        val lastPlayedText = remember(game.lastPlayedAt) {
            game.lastPlayedAt?.let(::formatTimestamp) ?: "Ainda não jogado"
        }
        val timePlayedText = remember(game.timePlayedSeconds) {
            formatTimePlayed(game.timePlayedSeconds)
        }
        val fullDescription = remember(game.description, game.subtitle) {
            game.description.ifBlank { game.subtitle }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            val isWide = maxWidth >= 680.dp

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 920.dp)
            ) {
                item {
                    DetailHeroSection(
                        game = game,
                        accent = accent,
                        onPrimaryAction = {
                            if (game.isBuiltInPlayable) {
                                onPlayGame(game.id)
                            } else {
                                onInspectInVault()
                            }
                        }
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "METADADOS DO ARQUIVO & INSTALAÇÃO",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (isWide) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MetadataTile(
                                    icon = Icons.Filled.Storage,
                                    label = "TAMANHO DO ARQUIVO",
                                    value = game.sizeLabel,
                                    accent = accent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("detail_size_value")
                                )
                                MetadataTile(
                                    icon = Icons.Filled.CalendarToday,
                                    label = "DATA DE INSTALAÇÃO",
                                    value = installedDateText,
                                    accent = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("detail_install_date_value")
                                )
                                MetadataTile(
                                    icon = Icons.Filled.Timer,
                                    label = "TEMPO JOGADO",
                                    value = timePlayedText,
                                    accent = EmeraldPulse,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("detail_time_played_value")
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MetadataTile(
                                    icon = Icons.Filled.History,
                                    label = "ÚLTIMA SESSÃO",
                                    value = lastPlayedText,
                                    accent = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                MetadataTile(
                                    icon = Icons.Filled.Inventory2,
                                    label = "TIPO DE PACOTE",
                                    value = "${game.packageType} • ${game.versionLabel}",
                                    accent = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MetadataTile(
                                    icon = Icons.Filled.Storage,
                                    label = "TAMANHO DO ARQUIVO",
                                    value = game.sizeLabel,
                                    accent = accent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("detail_size_value")
                                )
                                MetadataTile(
                                    icon = Icons.Filled.Timer,
                                    label = "TEMPO JOGADO",
                                    value = timePlayedText,
                                    accent = EmeraldPulse,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("detail_time_played_value")
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MetadataTile(
                                    icon = Icons.Filled.CalendarToday,
                                    label = "DATA DE INSTALAÇÃO",
                                    value = installedDateText,
                                    accent = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("detail_install_date_value")
                                )
                                MetadataTile(
                                    icon = Icons.Filled.History,
                                    label = "ÚLTIMA SESSÃO",
                                    value = lastPlayedText,
                                    accent = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tag,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "IDENTIFICADOR & ORIGEM",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = game.sourceArchiveName?.let { "${game.id} • $it" }
                                            ?: "${game.id} • Biblioteca Nativa ZipPlay",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                            .testTag("detail_description_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "DESCRIÇÃO COMPLETA",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = fullDescription,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CapabilityTag(
                                    icon = Icons.Filled.SportsEsports,
                                    text = "Gênero: ${game.genre}",
                                    accent = accent
                                )
                                if (game.isBuiltInPlayable) {
                                    CapabilityTag(
                                        icon = Icons.Filled.TouchApp,
                                        text = "Controles por Toque",
                                        accent = EmeraldPulse
                                    )
                                    CapabilityTag(
                                        icon = Icons.Filled.Keyboard,
                                        text = "Suporte a Teclado",
                                        accent = MaterialTheme.colorScheme.secondary
                                    )
                                } else {
                                    CapabilityTag(
                                        icon = Icons.Filled.FolderZip,
                                        text = "Somente Leitura Segura",
                                        accent = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = onOpenKeyBindings,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("detail_key_bindings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Keyboard,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Configurar Mapeamento de Teclado")
                            }
                        }
                    }
                }

                if (!game.isBuiltInPlayable) {
                    item {
                        OutlinedButton(
                            onClick = {
                                onDeleteCustomGame(game.id)
                                onBack()
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("detail_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Remover pacote da biblioteca",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailHeroSection(
    game: GameEntryEntity,
    accent: Color,
    onPrimaryAction: () -> Unit
) {
    val gameIcon = when (game.genre) {
        "APK" -> Icons.Filled.Android
        "ZIP" -> Icons.Filled.FolderZip
        else -> Icons.Filled.SportsEsports
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(accent.copy(alpha = 0.20f))
                            .border(1.5.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = gameIcon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${game.genre.uppercase()} • ${game.packageType.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                        Text(
                            text = game.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = game.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (game.isBuiltInPlayable) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HeroMetricPill(
                            icon = Icons.Filled.EmojiEvents,
                            label = "Recorde",
                            value = game.highScore.toString(),
                            accent = ElectricAmber,
                            modifier = Modifier.weight(1f)
                        )
                        HeroMetricPill(
                            icon = Icons.Filled.PlayArrow,
                            label = "Partidas",
                            value = "${game.playCount}x",
                            accent = accent,
                            modifier = Modifier.weight(1f)
                        )
                        HeroMetricPill(
                            icon = Icons.Filled.Star,
                            label = "Status",
                            value = if (game.isFavorite) "Favorito" else "Biblioteca",
                            accent = if (game.isFavorite) ElectricAmber else EmeraldPulse,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Button(
                    onClick = onPrimaryAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("detail_primary_action_button")
                ) {
                    Icon(
                        imageVector = if (game.isBuiltInPlayable) Icons.Filled.PlayArrow else Icons.Filled.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (game.isBuiltInPlayable) {
                            "${stringResource(R.string.play_now)} ${game.title}"
                        } else {
                            stringResource(R.string.inspect_package)
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroMetricPill(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MetadataTile(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CapabilityTag(
    icon: ImageVector,
    text: String,
    accent: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = accent
        )
    }
}

private fun formatTimestamp(epochMillis: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"))
    return formatter.format(Date(epochMillis))
}

internal fun formatTimePlayed(totalSeconds: Long): String {
    if (totalSeconds <= 0L) return "0m 00s"
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0L -> "${hours}h ${minutes.toString().padStart(2, '0')}m"
        else -> "${minutes}m ${seconds.toString().padStart(2, '0')}s"
    }
}
