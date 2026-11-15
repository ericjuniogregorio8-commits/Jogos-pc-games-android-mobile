package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.abs

private data class BrickBlock(
    val row: Int,
    val col: Int,
    val hitsLeft: Int,
    val color: Color
)

@Composable
fun BrickBreakerScreen(
    bestScore: Int,
    onScoreRecorded: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cols = 7
    val rows = 5
    val rowColors = listOf(ArcadeMagenta, ElectricAmber, NeonCyan, EmeraldPulse, NeonCyan)

    fun buildBricks(wave: Int): List<BrickBlock> = buildList {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val hits = if (r == 0 || (wave > 1 && r == 1)) 2 else 1
                add(BrickBlock(row = r, col = c, hitsLeft = hits, color = rowColors[r % rowColors.size]))
            }
        }
    }

    var wave by remember { mutableIntStateOf(1) }
    var bricks by remember { mutableStateOf(buildBricks(1)) }
    var paddleX by remember { mutableFloatStateOf(0.5f) }
    var ballX by remember { mutableFloatStateOf(0.5f) }
    var ballY by remember { mutableFloatStateOf(0.82f) }
    var velX by remember { mutableFloatStateOf(0.36f) }
    var velY by remember { mutableFloatStateOf(-0.56f) }
    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var isPaused by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var recordedFinal by remember { mutableStateOf(false) }

    val paddleWidth = 0.24f
    val paddleY = 0.90f
    val ballRadius = 0.02f

    val inputService = LocalKeyboardInputService.current
    val keyboardMovement by inputService.movementVector.collectAsStateWithLifecycle()

    fun exitToArcade() {
        if (!recordedFinal && score > 0) {
            recordedFinal = true
            onScoreRecorded(score)
        }
        onBack()
    }

    fun resetGame() {
        wave = 1
        bricks = buildBricks(1)
        paddleX = 0.5f
        ballX = 0.5f
        ballY = 0.82f
        velX = 0.36f
        velY = -0.56f
        score = 0
        lives = 3
        isPaused = false
        isGameOver = false
        recordedFinal = false
    }

    KeyboardInputEffect(
        priority = GlobalKeyboardInputService.PRIORITY_GAME,
        isGameListener = true
    ) { action ->
        when (action) {
            GameControlAction.MOVE_LEFT,
            GameControlAction.MOVE_RIGHT,
            GameControlAction.MOVE_UP,
            GameControlAction.MOVE_DOWN -> true
            GameControlAction.PRIMARY_ACTION -> {
                if (isGameOver) resetGame() else isPaused = !isPaused
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

    LaunchedEffect(isPaused, isGameOver) {
        var lastTimeNanos = 0L
        while (!isPaused && !isGameOver) {
            withFrameNanos { frameTime ->
                if (lastTimeNanos == 0L) {
                    lastTimeNanos = frameTime
                    return@withFrameNanos
                }
                val dt = ((frameTime - lastTimeNanos) / 1_000_000_000f).coerceAtMost(0.032f)
                lastTimeNanos = frameTime

                if (keyboardMovement.x != 0f) {
                    val paddleSpeed = 0.95f
                    paddleX = (paddleX + keyboardMovement.x * paddleSpeed * dt)
                        .coerceIn(paddleWidth / 2f, 1f - paddleWidth / 2f)
                }

                var nx = ballX + velX * dt
                var ny = ballY + velY * dt

                if (nx < ballRadius) {
                    nx = ballRadius
                    velX = abs(velX)
                } else if (nx > 1f - ballRadius) {
                    nx = 1f - ballRadius
                    velX = -abs(velX)
                }

                if (ny < ballRadius) {
                    ny = ballRadius
                    velY = abs(velY)
                }

                // Paddle collision
                val paddleLeft = paddleX - paddleWidth / 2f
                val paddleRight = paddleX + paddleWidth / 2f
                if (velY > 0f && ny + ballRadius >= paddleY && ny - ballRadius <= paddleY + 0.03f &&
                    nx in (paddleLeft - ballRadius)..(paddleRight + ballRadius)
                ) {
                    ny = paddleY - ballRadius
                    val hitOffset = ((nx - paddleX) / (paddleWidth / 2f)).coerceIn(-1f, 1f)
                    val speedBoost = 1f + (wave - 1) * 0.08f
                    velX = hitOffset * 0.62f * speedBoost
                    velY = -abs(velY)
                }

                // Brick collision
                val brickW = 1f / cols
                val brickH = 0.065f
                val topOffset = 0.08f
                var hitBrickIndex = -1
                for (idx in bricks.indices) {
                    val b = bricks[idx]
                    val left = b.col * brickW + 0.008f
                    val right = (b.col + 1) * brickW - 0.008f
                    val top = topOffset + b.row * brickH + 0.006f
                    val bottom = topOffset + (b.row + 1) * brickH - 0.006f
                    if (nx + ballRadius >= left && nx - ballRadius <= right &&
                        ny + ballRadius >= top && ny - ballRadius <= bottom
                    ) {
                        hitBrickIndex = idx
                        val overlapX = minOf(nx + ballRadius - left, right - (nx - ballRadius))
                        val overlapY = minOf(ny + ballRadius - top, bottom - (ny - ballRadius))
                        if (overlapX < overlapY) {
                            velX = -velX
                        } else {
                            velY = -velY
                        }
                        break
                    }
                }

                if (hitBrickIndex >= 0) {
                    val target = bricks[hitBrickIndex]
                    val updated = bricks.toMutableList()
                    if (target.hitsLeft > 1) {
                        updated[hitBrickIndex] = target.copy(hitsLeft = target.hitsLeft - 1)
                        score += 10
                    } else {
                        updated.removeAt(hitBrickIndex)
                        score += 20 * wave
                    }
                    bricks = updated
                    if (updated.isEmpty()) {
                        wave++
                        bricks = buildBricks(wave)
                        nx = paddleX
                        ny = 0.82f
                        velY = -abs(velY) * 1.05f
                    }
                }

                // Bottom miss
                if (ny > 1f + ballRadius) {
                    lives--
                    if (lives <= 0) {
                        isGameOver = true
                        if (!recordedFinal && score > 0) {
                            recordedFinal = true
                            onScoreRecorded(score)
                        }
                    } else {
                        nx = paddleX
                        ny = 0.82f
                        velY = -abs(velY)
                    }
                }

                ballX = nx
                ballY = ny
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onBgColor = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GameHeaderBar(
            title = "Quebra-Blocos Zip",
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
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FASE $wave",
                style = MaterialTheme.typography.labelMedium,
                color = primaryColor
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(lives.coerceAtLeast(0)) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Vida restante",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.height(18.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(surfaceColor)
                .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        val delta = dragAmount / size.width.toFloat()
                        paddleX = (paddleX + delta).coerceIn(paddleWidth / 2f, 1f - paddleWidth / 2f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val brickW = w / cols
                val brickH = h * 0.065f
                val topOffset = h * 0.08f

                bricks.forEach { b ->
                    val alpha = if (b.hitsLeft > 1) 1f else 0.72f
                    drawRoundRect(
                        color = b.color.copy(alpha = alpha),
                        topLeft = Offset(b.col * brickW + 4.dp.toPx(), topOffset + b.row * brickH + 3.dp.toPx()),
                        size = Size(brickW - 8.dp.toPx(), brickH - 6.dp.toPx()),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }

                // Paddle
                val pW = w * paddleWidth
                val pH = 14.dp.toPx()
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(paddleX * w - pW / 2f, paddleY * h),
                    size = Size(pW, pH),
                    cornerRadius = CornerRadius(pH / 2f, pH / 2f)
                )

                // Ball
                drawCircle(
                    color = onBgColor,
                    radius = ballRadius * minOf(w, h),
                    center = Offset(ballX * w, ballY * h)
                )
            }

            if (isPaused && !isGameOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.78f)),
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = { isPaused = false }) {
                        Text("Continuar")
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "FIM DE JOGO",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Pontuação: $score pts",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { resetGame() },
                            modifier = Modifier.testTag("brick_play_again_button")
                        ) {
                            Text("Jogar Novamente")
                        }
                    }
                }
            }
        }

        KeyboardControlsHintBar(
            hints = listOf(
                KeyHintItem(
                    keys = "A/D ou ◄/►",
                    label = "Raquete",
                    activeWhen = setOf(GameControlAction.MOVE_LEFT, GameControlAction.MOVE_RIGHT)
                ),
                KeyHintItem(keys = "Espaço/P", label = "Pausa"),
                KeyHintItem(keys = "R", label = "Reiniciar"),
                KeyHintItem(keys = "Esc", label = "Sair")
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    paddleX = (paddleX - 0.14f).coerceIn(paddleWidth / 2f, 1f - paddleWidth / 2f)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("◄ Esquerda")
            }
            OutlinedButton(
                onClick = {
                    paddleX = (paddleX + 0.14f).coerceIn(paddleWidth / 2f, 1f - paddleWidth / 2f)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Direita ►")
            }
        }
    }
}
