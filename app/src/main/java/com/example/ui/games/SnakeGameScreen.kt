package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.input.GameControlAction
import com.example.input.GlobalKeyboardInputService
import com.example.input.KeyboardInputEffect
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

private enum class SnakeDir { UP, DOWN, LEFT, RIGHT }
private data class GridPoint(val x: Int, val y: Int)

@Composable
fun SnakeGameScreen(
    bestScore: Int,
    onScoreRecorded: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cols = 16
    val rows = 16

    var snake by remember { mutableStateOf(listOf(GridPoint(8, 8), GridPoint(7, 8), GridPoint(6, 8))) }
    var direction by remember { mutableStateOf(SnakeDir.RIGHT) }
    var queuedDir by remember { mutableStateOf(SnakeDir.RIGHT) }
    var food by remember { mutableStateOf(GridPoint(12, 8)) }
    var bonusGem by remember { mutableStateOf<GridPoint?>(null) }
    var bonusTicks by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var wrapWalls by remember { mutableStateOf(true) }
    var recordedFinal by remember { mutableStateOf(false) }

    fun spawnEmptyCell(occupied: List<GridPoint>, extra: GridPoint? = null): GridPoint {
        val candidates = buildList {
            for (x in 0 until cols) {
                for (y in 0 until rows) {
                    val p = GridPoint(x, y)
                    if (p !in occupied && p != extra) add(p)
                }
            }
        }
        return if (candidates.isEmpty()) GridPoint(0, 0) else candidates[Random.nextInt(candidates.size)]
    }

    fun exitToArcade() {
        if (!recordedFinal && score > 0) {
            recordedFinal = true
            onScoreRecorded(score)
        }
        onBack()
    }

    fun resetGame() {
        val initial = listOf(GridPoint(8, 8), GridPoint(7, 8), GridPoint(6, 8))
        snake = initial
        direction = SnakeDir.RIGHT
        queuedDir = SnakeDir.RIGHT
        food = spawnEmptyCell(initial)
        bonusGem = null
        bonusTicks = 0
        score = 0
        isPaused = false
        isGameOver = false
        recordedFinal = false
    }

    fun changeDirection(next: SnakeDir) {
        val opposite = when (direction) {
            SnakeDir.UP -> SnakeDir.DOWN
            SnakeDir.DOWN -> SnakeDir.UP
            SnakeDir.LEFT -> SnakeDir.RIGHT
            SnakeDir.RIGHT -> SnakeDir.LEFT
        }
        if (next != opposite) {
            queuedDir = next
        }
    }

    KeyboardInputEffect(
        priority = GlobalKeyboardInputService.PRIORITY_GAME,
        isGameListener = true
    ) { action ->
        when (action) {
            GameControlAction.MOVE_UP -> {
                if (!isPaused && !isGameOver) changeDirection(SnakeDir.UP)
                true
            }
            GameControlAction.MOVE_DOWN -> {
                if (!isPaused && !isGameOver) changeDirection(SnakeDir.DOWN)
                true
            }
            GameControlAction.MOVE_LEFT -> {
                if (!isPaused && !isGameOver) changeDirection(SnakeDir.LEFT)
                true
            }
            GameControlAction.MOVE_RIGHT -> {
                if (!isPaused && !isGameOver) changeDirection(SnakeDir.RIGHT)
                true
            }
            GameControlAction.PRIMARY_ACTION -> {
                if (isGameOver) resetGame() else isPaused = !isPaused
                true
            }
            GameControlAction.SECONDARY_ACTION -> {
                wrapWalls = !wrapWalls
                true
            }
            GameControlAction.PAUSE_TOGGLE -> {
                if (!isGameOver) isPaused = !isPaused
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

    LaunchedEffect(isPaused, isGameOver, wrapWalls) {
        while (!isPaused && !isGameOver) {
            val tickMs = (165L - (score / 25) * 6L).coerceAtLeast(80L)
            delay(tickMs)

            direction = queuedDir
            val head = snake.first()
            var nextX = head.x + when (direction) {
                SnakeDir.LEFT -> -1
                SnakeDir.RIGHT -> 1
                else -> 0
            }
            var nextY = head.y + when (direction) {
                SnakeDir.UP -> -1
                SnakeDir.DOWN -> 1
                else -> 0
            }

            if (wrapWalls) {
                nextX = (nextX + cols) % cols
                nextY = (nextY + rows) % rows
            } else if (nextX !in 0 until cols || nextY !in 0 until rows) {
                isGameOver = true
                if (!recordedFinal && score > 0) {
                    recordedFinal = true
                    onScoreRecorded(score)
                }
                break
            }

            val nextHead = GridPoint(nextX, nextY)
            val willEatFood = nextHead == food
            val willEatBonus = nextHead == bonusGem
            val bodyToCheck = if (willEatFood || willEatBonus) snake else snake.dropLast(1)

            if (nextHead in bodyToCheck) {
                isGameOver = true
                if (!recordedFinal && score > 0) {
                    recordedFinal = true
                    onScoreRecorded(score)
                }
                break
            }

            val newSnake = if (willEatFood || willEatBonus) {
                listOf(nextHead) + snake
            } else {
                listOf(nextHead) + snake.dropLast(1)
            }
            snake = newSnake

            if (willEatFood) {
                score += 10
                food = spawnEmptyCell(newSnake, bonusGem)
                if (bonusGem == null && Random.nextInt(5) == 0) {
                    bonusGem = spawnEmptyCell(newSnake, food)
                    bonusTicks = 28
                }
            }
            if (willEatBonus) {
                score += 30
                bonusGem = null
                bonusTicks = 0
            } else if (bonusGem != null) {
                bonusTicks--
                if (bonusTicks <= 0) bonusGem = null
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val boardBg = MaterialTheme.colorScheme.surface
    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GameHeaderBar(
            title = "Cobra Neon",
            score = score,
            bestScore = bestScore,
            onBack = { exitToArcade() },
            onRestart = { resetGame() },
            isPaused = isPaused,
            onTogglePause = { if (!isGameOver) isPaused = !isPaused }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = wrapWalls,
                onClick = { wrapWalls = true },
                label = { Text("Bordas Livres") }
            )
            FilterChip(
                selected = !wrapWalls,
                onClick = { wrapWalls = false },
                label = { Text("Modo Clássico") }
            )
            Spacer(modifier = Modifier.weight(1f))
            if (bonusGem != null) {
                Text(
                    text = "GEMA +30",
                    style = MaterialTheme.typography.labelMedium,
                    color = tertiaryColor
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(boardBg)
                .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    var accX = 0f
                    var accY = 0f
                    detectDragGestures(
                        onDragStart = {
                            accX = 0f
                            accY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            accX += dragAmount.x
                            accY += dragAmount.y
                            val threshold = 32f
                            if (abs(accX) > threshold || abs(accY) > threshold) {
                                if (abs(accX) > abs(accY)) {
                                    changeDirection(if (accX > 0) SnakeDir.RIGHT else SnakeDir.LEFT)
                                } else {
                                    changeDirection(if (accY > 0) SnakeDir.DOWN else SnakeDir.UP)
                                }
                                accX = 0f
                                accY = 0f
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                val cellW = size.width / cols
                val cellH = size.height / rows

                for (i in 1 until cols) {
                    drawLine(
                        color = gridLineColor,
                        start = Offset(i * cellW, 0f),
                        end = Offset(i * cellW, size.height),
                        strokeWidth = 1f
                    )
                }
                for (j in 1 until rows) {
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, j * cellH),
                        end = Offset(size.width, j * cellH),
                        strokeWidth = 1f
                    )
                }

                // Food
                drawRoundRect(
                    color = secondaryColor,
                    topLeft = Offset(food.x * cellW + cellW * 0.14f, food.y * cellH + cellH * 0.14f),
                    size = Size(cellW * 0.72f, cellH * 0.72f),
                    cornerRadius = CornerRadius(cellW * 0.3f, cellH * 0.3f)
                )

                // Bonus gem
                bonusGem?.let { gem ->
                    drawCircle(
                        color = tertiaryColor,
                        radius = minOf(cellW, cellH) * 0.36f,
                        center = Offset((gem.x + 0.5f) * cellW, (gem.y + 0.5f) * cellH)
                    )
                }

                // Snake
                snake.forEachIndexed { index, part ->
                    val alpha = (1f - index * 0.03f).coerceAtLeast(0.42f)
                    val pad = if (index == 0) 0.06f else 0.11f
                    drawRoundRect(
                        color = primaryColor.copy(alpha = alpha),
                        topLeft = Offset(part.x * cellW + cellW * pad, part.y * cellH + cellH * pad),
                        size = Size(cellW * (1f - pad * 2f), cellH * (1f - pad * 2f)),
                        cornerRadius = CornerRadius(cellW * 0.25f, cellH * 0.25f)
                    )
                }
            }

            if (isPaused && !isGameOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.78f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PAUSADO", style = MaterialTheme.typography.headlineMedium, color = primaryColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { isPaused = false }) {
                            Text("Continuar")
                        }
                    }
                }
            }

            if (isGameOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text("FIM DE JOGO", style = MaterialTheme.typography.headlineMedium, color = tertiaryColor)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Pontuação: $score pts",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { resetGame() },
                            modifier = Modifier.testTag("snake_play_again_button")
                        ) {
                            Text("Jogar Novamente")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        KeyboardControlsHintBar(
            hints = listOf(
                KeyHintItem(
                    keys = "WASD/Setas",
                    label = "Direção",
                    activeWhen = setOf(
                        GameControlAction.MOVE_UP,
                        GameControlAction.MOVE_DOWN,
                        GameControlAction.MOVE_LEFT,
                        GameControlAction.MOVE_RIGHT
                    )
                ),
                KeyHintItem(keys = "Espaço/P", label = "Pausa"),
                KeyHintItem(keys = "Q", label = "Bordas"),
                KeyHintItem(keys = "R", label = "Reiniciar")
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tactile D-Pad controls
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DpadButton(
                icon = Icons.Filled.KeyboardArrowUp,
                description = "Mover para cima",
                onClick = { changeDirection(SnakeDir.UP) }
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DpadButton(
                    icon = Icons.Filled.KeyboardArrowLeft,
                    description = "Mover para esquerda",
                    onClick = { changeDirection(SnakeDir.LEFT) }
                )
                DpadButton(
                    icon = Icons.Filled.KeyboardArrowDown,
                    description = "Mover para baixo",
                    onClick = { changeDirection(SnakeDir.DOWN) }
                )
                DpadButton(
                    icon = Icons.Filled.KeyboardArrowRight,
                    description = "Mover para direita",
                    onClick = { changeDirection(SnakeDir.RIGHT) }
                )
            }
        }
    }
}

@Composable
private fun DpadButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(54.dp)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
    }
}
