package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.AchievementEntity
import com.example.data.local.GameEntryEntity
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.EmeraldPulse
import com.example.ui.viewmodel.ArcadeUiState

@Composable
fun StatsScreen(
    uiState: ArcadeUiState,
    onToggleFavorite: (GameEntryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val playableGames = uiState.allGames.filter { it.isBuiltInPlayable }
    val maxScore = (playableGames.maxOfOrNull { it.highScore } ?: 0).coerceAtLeast(100)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Placar & Conquistas",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Acompanhe seus recordes, tempo total jogado e conquistas desbloqueadas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatKpiBox(
                        title = "PONTUAÇÃO TOTAL",
                        value = uiState.totalHighScore.toString(),
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatKpiBox(
                        title = "TEMPO JOGADO",
                        value = formatTimePlayed(uiState.totalTimePlayedSeconds),
                        accent = EmeraldPulse,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatKpiBox(
                        title = "PARTIDAS",
                        value = uiState.totalPlays.toString(),
                        accent = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                    StatKpiBox(
                        title = "CONQUISTAS",
                        value = "${uiState.unlockedAchievementsCount}/${uiState.achievements.size}",
                        accent = ElectricAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Text(
                text = "CONQUISTAS & MARCOS (${uiState.unlockedAchievementsCount}/${uiState.achievements.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(uiState.achievements, key = { it.id }) { achievement ->
            AchievementCard(achievement = achievement)
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "DESEMPENHO POR JOGO",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(playableGames, key = { it.id }) { game ->
            val accent = Color(game.accentColorHex)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SportsEsports,
                                contentDescription = null,
                                tint = accent
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = game.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${game.playCount} partidas • ${formatTimePlayed(game.timePlayedSeconds)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = ElectricAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = " ${game.highScore}",
                                style = MaterialTheme.typography.titleMedium,
                                color = ElectricAmber
                            )
                            IconButton(
                                onClick = { onToggleFavorite(game) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = if (game.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                    contentDescription = "Alternar favorito de ${game.title}",
                                    tint = if (game.isFavorite) ElectricAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (game.highScore.toFloat() / maxScore.toFloat()).coerceIn(0f, 1f) },
                        color = accent,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: AchievementEntity
) {
    val accent = Color(achievement.accentColorHex)
    val ratio = if (achievement.targetValue > 0) {
        (achievement.currentValue.toFloat() / achievement.targetValue.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (achievement.isUnlocked) 1.5.dp else 1.dp,
                color = if (achievement.isUnlocked) accent.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("achievement_${achievement.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (achievement.isUnlocked) accent.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (achievement.isUnlocked) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                        contentDescription = null,
                        tint = if (achievement.isUnlocked) accent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = achievement.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (achievement.isUnlocked) "DESBLOQUEADA" else "${achievement.currentValue}/${achievement.targetValue} ${achievement.unitLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (achievement.isUnlocked) accent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = achievement.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LinearProgressIndicator(
                progress = { ratio },
                color = if (achievement.isUnlocked) accent else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun StatKpiBox(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = accent
        )
    }
}
