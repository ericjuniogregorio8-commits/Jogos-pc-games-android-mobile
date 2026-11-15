package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private data class CollectibleOrb(
    val x: Float,
    val y: Float,
    val isBonus: Boolean
)

private data class PatrolDrone(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float
)

@Composable
fun CharacterMovementScreen(
    bestScore: Int,
    onScoreRecorded: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var playerX by remember { mutableFloatStateOf(0.5f) }
    var playerY by remember { mutableFloatStateOf(0.5f) }
    var facingAngle by remember { mutableFloatStateOf(0f) }

    // Normalized touch joystick direction (-1..1)
    var inputDirX by remember { mutableFloatStateOf(0f) }
    var inputDirY by remember { mutableFloatStateOf(0f) }

    // Visual touch joystick overlay in pixel coordinates inside the arena
    var touchOrigin by remember { mutableStateOf<Offset?>(null) }
    var touchCurrent by remember { mutableStateOf<Offset?>(null) }

    // Tap-to-move target (normalized 0..1), used when user taps without dragging
    var moveTarget by remember { mutableStateOf<Offset?>(null) }

    var orbs by remember {
        mutableStateOf(
            listOf(
                CollectibleOrb(0.22f, 0.25f, false),
                CollectibleOrb(0.78f, 0.28f, false),
                CollectibleOrb(0.76f, 0.78f, true)
            )
        )
    }

    var drones by remember {
        mutableStateOf(
            listOf(
                PatrolDrone(0.2f, 0.75f, 0.22f, -0.14f),
                PatrolDrone(0.82f, 0.2f, -0.18f, 0.20f)
            )
        )
    }

    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var invulnerableTimer by remember { mutableFloatStateOf(0f) }
    var dashTimer by remember { mutableFloatStateOf(0f) }
    var dashCooldown by remember { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var recordedFinal by remember { mutableStateOf(false) }

    val inputService = LocalKeyboardInputService.current
    val keyboardMovement by inputService.movementVector.collectAsStateWithLifecycle()

    fun spawnOrb(isBonus: Boolean): CollectibleOrb {
        return CollectibleOrb(
            x = 0.10f + Random.nextFloat() * 0.80f,
            y = 0.10f + Random.nextFloat() * 0.80f,
            isBonus = isBonus
        )
    }

    fun exitToArcade() {
        if (!recordedFinal && score > 0) {
            recordedFinal = true
            onScoreRecorded(score)
        }
        onBack()
    }

    fun resetGame() {
        playerX = 0.5f
        playerY = 0.5f
        facingAngle = 0f
        inputDirX = 0f
        inputDirY = 0f
        touchOrigin = null
        touchCurrent = null
        moveTarget = null
        orbs = listOf(
            spawnOrb(false),
            spawnOrb(false),
            spawnOrb(true)
        )
        drones = listOf(
            PatrolDrone(0.2f, 0.75f, 0.22f, -0.14f),
            PatrolDrone(0.82f, 0.2f, -0.18f, 0.20f)
        )
        score = 0
        lives = 3
        invulnerableTimer = 0f
        dashTimer = 0f
        dashCooldown = 0f
        isPaused = false
        isGameOver = false
        recordedFinal = false
    }

    fun triggerDash() {
        if (isPaused || isGameOver || dashCooldown > 0f) return
        moveTarget = null
        dashTimer = 0.20f
        dashCooldown = 1.0f
        invulnerableTimer = maxOf(invulnerableTimer, 0.24f)
    }

    fun nudgeDirection(dx: Float, dy: Float) {
        if (isPaused || isGameOver) return
        moveTarget = null
        val step = 0.065f
        playerX = (playerX + dx * step).coerceIn(0.06f, 0.94f)
        playerY = (playerY + dy * step).coerceIn(0.06f, 0.94f)
        facingAngle = atan2(dy, dx)
    }

    KeyboardInputEffect(
        priority = GlobalKeyboardInputService.PRIORITY_GAME,
        isGameListener = true
    ) { action ->
        when (action) {
            GameControlAction.MOVE_UP,
            GameControlAction.MOVE_DOWN,
            GameControlAction.MOVE_LEFT,
            GameControlAction.MOVE_RIGHT -> {
                if (!isPaused && !isGameOver) {
                    moveTarget = null
                }
                true
            }
            GameControlAction.PRIMARY_ACTION -> {
                when {
                    isGameOver -> resetGame()
                    isPaused -> isPaused = false
                    else -> triggerDash()
                }
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
        var lastNanos = 0L
        while (!isPaused && !isGameOver) {
            withFrameNanos { frameNanos ->
                if (lastNanos == 0L) {
                    lastNanos = frameNanos
                    return@withFrameNanos
                }
                val dt = ((frameNanos - lastNanos) / 1_000_000_000f).coerceAtMost(0.032f)
                lastNanos = frameNanos

                if (invulnerableTimer > 0f) {
                    invulnerableTimer = (invulnerableTimer - dt).coerceAtLeast(0f)
                }
                if (dashTimer > 0f) {
                    dashTimer = (dashTimer - dt).coerceAtLeast(0f)
                }
                if (dashCooldown > 0f) {
                    dashCooldown = (dashCooldown - dt).coerceAtLeast(0f)
                }

                val moveSpeed = if (dashTimer > 0f) 1.15f else 0.46f
                var vx = if (keyboardMovement.isMoving) keyboardMovement.x else inputDirX
                var vy = if (keyboardMovement.isMoving) keyboardMovement.y else inputDirY

                if (vx == 0f && vy == 0f && dashTimer > 0f) {
                    vx = cos(facingAngle)
                    vy = sin(facingAngle)
                }

                if (vx == 0f && vy == 0f) {
                    moveTarget?.let { target ->
                        val dx = target.x - playerX
                        val dy = target.y - playerY
                        val dist = hypot(dx, dy)
                        if (dist > 0.015f) {
                            vx = dx / dist
                            vy = dy / dist
                        } else {
                            moveTarget = null
                        }
                    }
                }

                if (vx != 0f || vy != 0f) {
                    playerX = (playerX + vx * moveSpeed * dt).coerceIn(0.055f, 0.945f)
                    playerY = (playerY + vy * moveSpeed * dt).coerceIn(0.055f, 0.945f)
                    facingAngle = atan2(vy, vx)
                }

                // Move patrol drones
                drones = drones.map { d ->
                    var nx = d.x + d.vx * dt
                    var ny = d.y + d.vy * dt
                    var nvx = d.vx
                    var nvy = d.vy
                    if (nx < 0.06f || nx > 0.94f) {
                        nvx = -nvx
                        nx = nx.coerceIn(0.06f, 0.94f)
                    }
                    if (ny < 0.06f || ny > 0.94f) {
                        nvy = -nvy
                        ny = ny.coerceIn(0.06f, 0.94f)
                    }
                    PatrolDrone(nx, ny, nvx, nvy)
                }

                // Collect orbs
                var collectedAny = false
                val remainingOrbs = orbs.map { orb ->
                    val dist = hypot(orb.x - playerX, orb.y - playerY)
                    if (dist < 0.068f) {
                        collectedAny = true
                        score += if (orb.isBonus) 30 else 15
                        spawnOrb(Random.nextInt(4) == 0)
                    } else {
                        orb
                    }
                }
                if (collectedAny) {
                    orbs = remainingOrbs
                    if (score >= 120 && drones.size < 3) {
                        drones = drones + PatrolDrone(0.15f, 0.15f, 0.24f, 0.22f)
                    }
                }

                // Drone collision
                if (invulnerableTimer <= 0f) {
                    val hit = drones.any { d -> hypot(d.x - playerX, d.y - playerY) < 0.062f }
                    if (hit) {
                        lives--
                        invulnerableTimer = 1.6f
                        if (lives <= 0) {
                            isGameOver = true
                            if (!recordedFinal && score > 0) {
                                recordedFinal = true
                                onScoreRecorded(score)
                            }
                        }
                    }
                }
            }
        }
    }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val onBgColor = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GameHeaderBar(
            title = "Explorador Cyber",
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
                .widthIn(max = 480.dp)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Arraste ou toque na arena para mover",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(lives.coerceAtLeast(0)) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Vida restante",
                        tint = ArcadeMagenta,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(surfaceColor)
                .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val origin = down.position
                        var movedFar = false
                        touchOrigin = origin
                        touchCurrent = origin

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                if (!movedFar && size.width > 0 && size.height > 0) {
                                    moveTarget = Offset(
                                        x = (change.position.x / size.width.toFloat()).coerceIn(0.06f, 0.94f),
                                        y = (change.position.y / size.height.toFloat()).coerceIn(0.06f, 0.94f)
                                    )
                                }
                                touchOrigin = null
                                touchCurrent = null
                                inputDirX = 0f
                                inputDirY = 0f
                                break
                            }
                            change.consume()
                            val current = change.position
                            touchCurrent = current
                            val dx = current.x - origin.x
                            val dy = current.y - origin.y
                            val dist = hypot(dx, dy)
                            if (dist > 14f) {
                                movedFar = true
                                moveTarget = null
                                val maxRadius = 85f
                                val strength = (dist / maxRadius).coerceIn(0.25f, 1f)
                                inputDirX = (dx / dist) * strength
                                inputDirY = (dy / dist) * strength
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val minDim = minOf(w, h)

                // Arena grid lines
                val lines = 8
                for (i in 1 until lines) {
                    drawLine(
                        color = gridColor,
                        start = Offset(w * i / lines, 0f),
                        end = Offset(w * i / lines, h),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, h * i / lines),
                        end = Offset(w, h * i / lines),
                        strokeWidth = 1f
                    )
                }

                // Tap-to-move destination indicator
                moveTarget?.let { target ->
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.45f),
                        radius = minDim * 0.032f,
                        center = Offset(target.x * w, target.y * h),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Collectible orbs
                orbs.forEach { orb ->
                    val orbCenter = Offset(orb.x * w, orb.y * h)
                    val orbColor = if (orb.isBonus) EmeraldPulse else ElectricAmber
                    drawCircle(
                        color = orbColor.copy(alpha = 0.24f),
                        radius = minDim * 0.042f,
                        center = orbCenter
                    )
                    drawCircle(
                        color = orbColor,
                        radius = minDim * 0.022f,
                        center = orbCenter
                    )
                }

                // Patrol drones
                drones.forEach { drone ->
                    val dc = Offset(drone.x * w, drone.y * h)
                    drawCircle(
                        color = ArcadeMagenta.copy(alpha = 0.24f),
                        radius = minDim * 0.044f,
                        center = dc
                    )
                    drawCircle(
                        color = ArcadeMagenta,
                        radius = minDim * 0.026f,
                        center = dc
                    )
                }

                // Player character (with directional visor)
                val showPlayer = invulnerableTimer <= 0f || ((invulnerableTimer * 10).toInt() % 2 == 0)
                if (showPlayer) {
                    val px = playerX * w
                    val py = playerY * h
                    val radius = minDim * 0.038f
                    val playerCenter = Offset(px, py)

                    // Dash pulse ring when boosting
                    if (dashTimer > 0f) {
                        drawCircle(
                            color = EmeraldPulse.copy(alpha = 0.7f),
                            radius = radius * (2.1f - dashTimer * 3f),
                            center = playerCenter,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    // Outer glow
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.25f),
                        radius = radius * 1.45f,
                        center = playerCenter
                    )
                    // Character body
                    drawCircle(
                        color = NeonCyan,
                        radius = radius,
                        center = playerCenter
                    )
                    // Directional indicator / visor
                    val visorCenter = Offset(
                        x = px + cos(facingAngle) * radius * 0.52f,
                        y = py + sin(facingAngle) * radius * 0.52f
                    )
                    drawCircle(
                        color = onBgColor,
                        radius = radius * 0.34f,
                        center = visorCenter
                    )
                }

                // Floating touch joystick feedback
                val origin = touchOrigin
                val current = touchCurrent
                if (origin != null && current != null) {
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.35f),
                        radius = 44.dp.toPx(),
                        center = origin,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    val dx = current.x - origin.x
                    val dy = current.y - origin.y
                    val dist = hypot(dx, dy)
                    val clampedDist = dist.coerceAtMost(44.dp.toPx())
                    val knobCenter = if (dist > 0.1f) {
                        Offset(
                            origin.x + (dx / dist) * clampedDist,
                            origin.y + (dy / dist) * clampedDist
                        )
                    } else {
                        origin
                    }
                    drawLine(
                        color = NeonCyan.copy(alpha = 0.5f),
                        start = origin,
                        end = knobCenter,
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.78f),
                        radius = 16.dp.toPx(),
                        center = knobCenter
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
                            color = ArcadeMagenta
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Pontuação: $score pts",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { resetGame() },
                            modifier = Modifier.testTag("cyber_play_again_button")
                        ) {
                            Text("Jogar Novamente")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        KeyboardControlsHintBar(
            hints = listOf(
                KeyHintItem(
                    keys = "WASD/Setas",
                    label = "Mover",
                    activeWhen = setOf(
                        GameControlAction.MOVE_UP,
                        GameControlAction.MOVE_DOWN,
                        GameControlAction.MOVE_LEFT,
                        GameControlAction.MOVE_RIGHT
                    )
                ),
                KeyHintItem(keys = "Espaço", label = "Dash"),
                KeyHintItem(keys = "P", label = "Pausa"),
                KeyHintItem(keys = "Esc", label = "Sair")
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Directional touch buttons for tap movement as well
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DirectionalPadButton(Icons.Filled.KeyboardArrowLeft, "Mover esquerda") { nudgeDirection(-1f, 0f) }
            DirectionalPadButton(Icons.Filled.KeyboardArrowUp, "Mover cima") { nudgeDirection(0f, -1f) }
            DirectionalPadButton(Icons.Filled.KeyboardArrowDown, "Mover baixo") { nudgeDirection(0f, 1f) }
            DirectionalPadButton(Icons.Filled.KeyboardArrowRight, "Mover direita") { nudgeDirection(1f, 0f) }
        }
    }
}

@Composable
private fun DirectionalPadButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(50.dp)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
