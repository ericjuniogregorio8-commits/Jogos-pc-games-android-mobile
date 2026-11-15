package com.example.ui.games

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.input.GameControlAction
import com.example.input.GlobalKeyboardInputService
import com.example.input.KeyboardInputEffect
import com.example.ui.theme.ArcadeMagenta
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.NeonCyan
import kotlin.math.abs
import kotlin.random.Random

private enum class MoveDir { UP, DOWN, LEFT, RIGHT }

@Composable
fun Game2048Screen(
    bestScore: Int,
    onScoreRecorded: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    fun emptyBoard(): List<Int> = List(16) { 0 }

    fun spawnTile(board: List<Int>): List<Int> {
        val emptyIndices = board.indices.filter { board[it] == 0 }
        if (emptyIndices.isEmpty()) return board
        val target = emptyIndices[Random.nextInt(emptyIndices.size)]
        val value = if (Random.nextInt(10) == 0) 4 else 2
        return board.toMutableList().apply { this[target] = value }
    }

    fun initialBoard(): List<Int> = spawnTile(spawnTile(emptyBoard()))

    var board by remember { mutableStateOf(initialBoard()) }
    var previousBoard by remember { mutableStateOf<List<Int>?>(null) }
    var previousScore by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var hasReached2048 by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var recordedFinal by remember { mutableStateOf(false) }

    fun slideLine(line: List<Int>): Pair<List<Int>, Int> {
        val nonZero = line.filter { it != 0 }
        val merged = mutableListOf<Int>()
        var gained = 0
        var i = 0
        while (i < nonZero.size) {
            if (i + 1 < nonZero.size && nonZero[i] == nonZero[i + 1]) {
                val sum = nonZero[i] * 2
                merged.add(sum)
                gained += sum
                i += 2
            } else {
                merged.add(nonZero[i])
                i++
            }
        }
        while (merged.size < 4) merged.add(0)
        return merged to gained
    }

    fun canMove(b: List<Int>): Boolean {
        if (b.any { it == 0 }) return true
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                val v = b[r * 4 + c]
                if (c < 3 && v == b[r * 4 + c + 1]) return true
                if (r < 3 && v == b[(r + 1) * 4 + c]) return true
            }
        }
        return false
    }

    fun applyMove(dir: MoveDir) {
        if (isGameOver) return
        val next = MutableList(16) { 0 }
        var gainedTotal = 0

        for (i in 0 until 4) {
            val line = when (dir) {
                MoveDir.LEFT -> List(4) { c -> board[i * 4 + c] }
                MoveDir.RIGHT -> List(4) { c -> board[i * 4 + (3 - c)] }
                MoveDir.UP -> List(4) { r -> board[r * 4 + i] }
                MoveDir.DOWN -> List(4) { r -> board[(3 - r) * 4 + i] }
            }
            val (slid, gained) = slideLine(line)
            gainedTotal += gained
            for (j in 0 until 4) {
                when (dir) {
                    MoveDir.LEFT -> next[i * 4 + j] = slid[j]
                    MoveDir.RIGHT -> next[i * 4 + (3 - j)] = slid[j]
                    MoveDir.UP -> next[j * 4 + i] = slid[j]
                    MoveDir.DOWN -> next[(3 - j) * 4 + i] = slid[j]
                }
            }
        }

        if (next != board) {
            previousBoard = board
            previousScore = score
            val withNewTile = spawnTile(next)
            board = withNewTile
            score += gainedTotal
            if (withNewTile.any { it >= 2048 }) hasReached2048 = true
            if (!canMove(withNewTile)) {
                isGameOver = true
                if (!recordedFinal && score > 0) {
                    recordedFinal = true
                    onScoreRecorded(score)
                }
            }
        }
    }

    fun exitToArcade() {
        if (!recordedFinal && score > 0) {
            recordedFinal = true
            onScoreRecorded(score)
        }
        onBack()
    }

    fun undoMove() {
        previousBoard?.let { prev ->
            board = prev
            score = previousScore
            previousBoard = null
            isGameOver = false
        }
    }

    fun resetGame() {
        board = initialBoard()
        previousBoard = null
        previousScore = 0
        score = 0
        hasReached2048 = false
        isGameOver = false
        recordedFinal = false
    }

    KeyboardInputEffect(
        priority = GlobalKeyboardInputService.PRIORITY_GAME,
        isGameListener = true
    ) { action ->
        when (action) {
            GameControlAction.MOVE_UP -> {
                applyMove(MoveDir.UP)
                true
            }
            GameControlAction.MOVE_DOWN -> {
                applyMove(MoveDir.DOWN)
                true
            }
            GameControlAction.MOVE_LEFT -> {
                applyMove(MoveDir.LEFT)
                true
            }
            GameControlAction.MOVE_RIGHT -> {
                applyMove(MoveDir.RIGHT)
                true
            }
            GameControlAction.PRIMARY_ACTION -> {
                if (isGameOver) resetGame()
                true
            }
            GameControlAction.SECONDARY_ACTION -> {
                if (!isGameOver) undoMove()
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GameHeaderBar(
            title = "2048 Turbo",
            score = score,
            bestScore = bestScore,
            onBack = { exitToArcade() },
            onRestart = { resetGame() }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (hasReached2048) "Meta 2048 atingida!" else "Deslize para fundir blocos",
                style = MaterialTheme.typography.bodyMedium,
                color = if (hasReached2048) EmeraldPulse else MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = { undoMove() },
                enabled = previousBoard != null && !isGameOver
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Desfazer jogada",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text("Desfazer")
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
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                .padding(10.dp)
                .pointerInput(Unit) {
                    var totalX = 0f
                    var totalY = 0f
                    var movedInGesture = false
                    detectDragGestures(
                        onDragStart = {
                            totalX = 0f
                            totalY = 0f
                            movedInGesture = false
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (movedInGesture) return@detectDragGestures
                            totalX += dragAmount.x
                            totalY += dragAmount.y
                            val threshold = 44f
                            if (abs(totalX) > threshold || abs(totalY) > threshold) {
                                movedInGesture = true
                                if (abs(totalX) > abs(totalY)) {
                                    applyMove(if (totalX > 0) MoveDir.RIGHT else MoveDir.LEFT)
                                } else {
                                    applyMove(if (totalY > 0) MoveDir.DOWN else MoveDir.UP)
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (r in 0 until 4) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (c in 0 until 4) {
                            val value = board[r * 4 + c]
                            TileCell(value = value, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (isGameOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SEM MOVIMENTOS",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.tertiary
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
                            modifier = Modifier.testTag("game2048_restart_button")
                        ) {
                            Text("Nova Partida")
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
                    label = "Fundir",
                    activeWhen = setOf(
                        GameControlAction.MOVE_UP,
                        GameControlAction.MOVE_DOWN,
                        GameControlAction.MOVE_LEFT,
                        GameControlAction.MOVE_RIGHT
                    )
                ),
                KeyHintItem(keys = "Q/U", label = "Desfazer"),
                KeyHintItem(keys = "R", label = "Reiniciar"),
                KeyHintItem(keys = "Esc", label = "Sair")
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MoveArrowButton(Icons.Filled.KeyboardArrowLeft, "Mover esquerda") { applyMove(MoveDir.LEFT) }
            MoveArrowButton(Icons.Filled.KeyboardArrowUp, "Mover cima") { applyMove(MoveDir.UP) }
            MoveArrowButton(Icons.Filled.KeyboardArrowDown, "Mover baixo") { applyMove(MoveDir.DOWN) }
            MoveArrowButton(Icons.Filled.KeyboardArrowRight, "Mover direita") { applyMove(MoveDir.RIGHT) }
        }
    }
}

@Composable
private fun TileCell(value: Int, modifier: Modifier = Modifier) {
    val bgColor: Color
    val textColor: Color
    when (value) {
        0 -> {
            bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            textColor = Color.Transparent
        }
        2 -> {
            bgColor = MaterialTheme.colorScheme.surfaceContainerHigh
            textColor = MaterialTheme.colorScheme.onSurface
        }
        4 -> {
            bgColor = MaterialTheme.colorScheme.primaryContainer
            textColor = NeonCyan
        }
        8 -> {
            bgColor = NeonCyan.copy(alpha = 0.25f)
            textColor = NeonCyan
        }
        16 -> {
            bgColor = EmeraldPulse.copy(alpha = 0.25f)
            textColor = EmeraldPulse
        }
        32 -> {
            bgColor = ElectricAmber.copy(alpha = 0.28f)
            textColor = ElectricAmber
        }
        64 -> {
            bgColor = ArcadeMagenta.copy(alpha = 0.28f)
            textColor = ArcadeMagenta
        }
        128, 256 -> {
            bgColor = ElectricAmber.copy(alpha = 0.78f)
            textColor = MaterialTheme.colorScheme.onSecondary
        }
        else -> {
            bgColor = NeonCyan
            textColor = MaterialTheme.colorScheme.onPrimary
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = if (value == 0) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                else textColor.copy(alpha = 0.35f),
                shape = RoundedCornerShape(14.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (value > 0) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = when {
                        value >= 1000 -> 18.sp
                        value >= 100 -> 21.sp
                        else -> 24.sp
                    }
                ),
                color = textColor
            )
        }
    }
}

@Composable
private fun MoveArrowButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(52.dp)
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
