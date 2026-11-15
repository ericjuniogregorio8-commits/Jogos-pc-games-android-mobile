package com.example.ui.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.input.GameControlAction
import com.example.input.GlobalKeyboardInputService
import com.example.input.KeyboardInputEffect
import com.example.input.LocalKeyboardInputService
import com.example.ui.theme.ArcadeMagenta
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.delay

private data class MemoryCard(
    val uid: Int,
    val pairId: Int,
    val icon: ImageVector,
    val tint: Color,
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false
)

@Composable
fun MemoryGameScreen(
    bestScore: Int,
    onScoreRecorded: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val definitions = remember {
        listOf(
            Icons.Filled.SportsEsports to NeonCyan,
            Icons.Filled.RocketLaunch to ElectricAmber,
            Icons.Filled.Diamond to ArcadeMagenta,
            Icons.Filled.Bolt to EmeraldPulse,
            Icons.Filled.Star to ElectricAmber,
            Icons.Filled.Favorite to ArcadeMagenta,
            Icons.Filled.BugReport to NeonCyan,
            Icons.Filled.Extension to EmeraldPulse
        )
    }

    fun buildDeck(): List<MemoryCard> {
        val raw = definitions.flatMapIndexed { pairId, (icon, color) ->
            listOf(
                MemoryCard(uid = pairId * 2, pairId = pairId, icon = icon, tint = color),
                MemoryCard(uid = pairId * 2 + 1, pairId = pairId, icon = icon, tint = color)
            )
        }
        return raw.shuffled()
    }

    var cards by remember { mutableStateOf(buildDeck()) }
    var selectedIndices by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isCheckingMismatch by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var moves by remember { mutableIntStateOf(0) }
    var isVictory by remember { mutableStateOf(false) }
    var recordedFinal by remember { mutableStateOf(false) }
    var focusedIndex by remember { mutableIntStateOf(0) }

    val inputService = LocalKeyboardInputService.current
    val isKeyboardActive by inputService.isKeyboardActive.collectAsStateWithLifecycle()

    fun exitToArcade() {
        if (!recordedFinal && score > 0) {
            recordedFinal = true
            onScoreRecorded(score)
        }
        onBack()
    }

    fun flipCard(idx: Int) {
        val card = cards.getOrNull(idx) ?: return
        if (!isCheckingMismatch && !card.isFaceUp && !card.isMatched && selectedIndices.size < 2) {
            cards = cards.mapIndexed { i, item ->
                if (i == idx) item.copy(isFaceUp = true) else item
            }
            val nextSelected = selectedIndices + idx
            if (nextSelected.size == 2) moves++
            selectedIndices = nextSelected
        }
    }

    fun resetGame() {
        cards = buildDeck()
        selectedIndices = emptyList()
        isCheckingMismatch = false
        score = 0
        streak = 0
        moves = 0
        isVictory = false
        recordedFinal = false
        focusedIndex = 0
    }

    KeyboardInputEffect(
        priority = GlobalKeyboardInputService.PRIORITY_GAME,
        isGameListener = true
    ) { action ->
        when (action) {
            GameControlAction.MOVE_UP -> {
                focusedIndex = ((focusedIndex / 4 + 3) % 4) * 4 + (focusedIndex % 4)
                true
            }
            GameControlAction.MOVE_DOWN -> {
                focusedIndex = ((focusedIndex / 4 + 1) % 4) * 4 + (focusedIndex % 4)
                true
            }
            GameControlAction.MOVE_LEFT -> {
                focusedIndex = (focusedIndex / 4) * 4 + ((focusedIndex % 4 + 3) % 4)
                true
            }
            GameControlAction.MOVE_RIGHT -> {
                focusedIndex = (focusedIndex / 4) * 4 + ((focusedIndex % 4 + 1) % 4)
                true
            }
            GameControlAction.PRIMARY_ACTION -> {
                if (isVictory) resetGame() else flipCard(focusedIndex)
                true
            }
            GameControlAction.RESTART_GAME -> {
                resetGame()
                true
            }
            GameControlAction.NAV_BACK -> {
                exitToArcade()
                true
            }
            else -> false
        }
    }

    LaunchedEffect(selectedIndices) {
        if (selectedIndices.size == 2) {
            isCheckingMismatch = true
            val firstIdx = selectedIndices[0]
            val secondIdx = selectedIndices[1]
            val first = cards[firstIdx]
            val second = cards[secondIdx]

            if (first.pairId == second.pairId) {
                delay(240)
                streak++
                score += 50 + (streak - 1) * 25
                cards = cards.mapIndexed { idx, card ->
                    if (idx == firstIdx || idx == secondIdx) card.copy(isMatched = true) else card
                }
                selectedIndices = emptyList()
                isCheckingMismatch = false
                if (cards.all { it.isMatched }) {
                    val efficiencyBonus = (24 - moves).coerceAtLeast(0) * 15
                    score += efficiencyBonus
                    isVictory = true
                    if (!recordedFinal) {
                        recordedFinal = true
                        onScoreRecorded(score)
                    }
                }
            } else {
                delay(650)
                streak = 0
                cards = cards.mapIndexed { idx, card ->
                    if (idx == firstIdx || idx == secondIdx) card.copy(isFaceUp = false) else card
                }
                selectedIndices = emptyList()
                isCheckingMismatch = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GameHeaderBar(
            title = "Memória Pixel",
            score = score,
            bestScore = bestScore,
            onBack = { exitToArcade() },
            onRestart = { resetGame() }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Jogadas: $moves",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (streak > 1) "Sequência x$streak" else "Pares: ${cards.count { it.isMatched } / 2}/8",
                style = MaterialTheme.typography.labelMedium,
                color = if (streak > 1) ElectricAmber else MaterialTheme.colorScheme.primary
            )
        }

        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (r in 0 until 4) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (c in 0 until 4) {
                            val idx = r * 4 + c
                            val card = cards[idx]
                            MemoryCardTile(
                                card = card,
                                isKeyboardFocused = isKeyboardActive && idx == focusedIndex,
                                onClick = {
                                    focusedIndex = idx
                                    flipCard(idx)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (isVictory) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.88f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TABULEIRO COMPLETO!",
                            style = MaterialTheme.typography.headlineSmall,
                            color = EmeraldPulse
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Pontuação final: $score pts ($moves jogadas)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { resetGame() },
                            modifier = Modifier.testTag("memory_play_again_button")
                        ) {
                            Text("Jogar Novamente")
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        KeyboardControlsHintBar(
            hints = listOf(
                KeyHintItem(
                    keys = "WASD/Setas",
                    label = "Selecionar",
                    activeWhen = setOf(
                        GameControlAction.MOVE_UP,
                        GameControlAction.MOVE_DOWN,
                        GameControlAction.MOVE_LEFT,
                        GameControlAction.MOVE_RIGHT
                    )
                ),
                KeyHintItem(keys = "Espaço/Enter", label = "Virar"),
                KeyHintItem(keys = "R", label = "Reiniciar"),
                KeyHintItem(keys = "Esc", label = "Sair")
            )
        )
    }
}

@Composable
private fun MemoryCardTile(
    card: MemoryCard,
    isKeyboardFocused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val revealed = card.isFaceUp || card.isMatched
    val scale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.96f,
        animationSpec = spring(),
        label = "cardScale"
    )
    val bgColor = when {
        card.isMatched -> card.tint.copy(alpha = 0.22f)
        card.isFaceUp -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        isKeyboardFocused -> NeonCyan
        card.isMatched -> card.tint
        card.isFaceUp -> card.tint.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(if (isKeyboardFocused) 2.5.dp else 1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = !revealed, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (revealed) {
            Icon(
                imageVector = card.icon,
                contentDescription = "Símbolo revelado",
                tint = card.tint,
                modifier = Modifier.size(34.dp)
            )
        } else {
            Text(
                text = "?",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
