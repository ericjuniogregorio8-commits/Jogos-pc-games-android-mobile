package com.example.input

import android.view.KeyEvent
import kotlin.math.hypot

/**
 * Standardized control, action, and navigation triggers mapped from physical
 * or virtual keyboard input across ZipPlay Arcade.
 */
enum class GameControlAction {
    // Standard character & grid directional controls (Arrow keys + WASD)
    MOVE_UP,
    MOVE_DOWN,
    MOVE_LEFT,
    MOVE_RIGHT,

    // In-game & UI action triggers
    PRIMARY_ACTION,
    SECONDARY_ACTION,
    PAUSE_TOGGLE,
    RESTART_GAME,

    // Global navigation triggers
    NAV_BACK,
    NAV_TAB_1,
    NAV_TAB_2,
    NAV_TAB_3,
    NAV_NEXT_TAB,
    NAV_PREV_TAB
}

data class MovementVector(
    val x: Float = 0f,
    val y: Float = 0f
) {
    val isMoving: Boolean get() = x != 0f || y != 0f
}

data class KeyboardInputEvent(
    val action: GameControlAction,
    val keyCode: Int,
    val isRepeat: Boolean = false
)

/**
 * Pure key-code mapping utility converting Android [KeyEvent] codes into
 * standardized [GameControlAction] values, supporting user-configured remapping.
 */
object KeyboardControlMapper {

    val DEFAULT_REMAPPABLE_BINDINGS: Map<GameControlAction, Int> = linkedMapOf(
        GameControlAction.MOVE_UP to KeyEvent.KEYCODE_W,
        GameControlAction.MOVE_DOWN to KeyEvent.KEYCODE_S,
        GameControlAction.MOVE_LEFT to KeyEvent.KEYCODE_A,
        GameControlAction.MOVE_RIGHT to KeyEvent.KEYCODE_D,
        GameControlAction.PRIMARY_ACTION to KeyEvent.KEYCODE_SPACE,
        GameControlAction.SECONDARY_ACTION to KeyEvent.KEYCODE_E,
        GameControlAction.PAUSE_TOGGLE to KeyEvent.KEYCODE_P,
        GameControlAction.RESTART_GAME to KeyEvent.KEYCODE_R
    )

    val PRESET_KEY_OPTIONS: List<Int> = listOf(
        KeyEvent.KEYCODE_W,
        KeyEvent.KEYCODE_A,
        KeyEvent.KEYCODE_S,
        KeyEvent.KEYCODE_D,
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_SPACE,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_E,
        KeyEvent.KEYCODE_Q,
        KeyEvent.KEYCODE_J,
        KeyEvent.KEYCODE_K,
        KeyEvent.KEYCODE_L,
        KeyEvent.KEYCODE_P,
        KeyEvent.KEYCODE_R,
        KeyEvent.KEYCODE_Z,
        KeyEvent.KEYCODE_X
    )

    fun mapKeyCodeToAction(
        keyCode: Int,
        isShiftPressed: Boolean = false,
        customBindingsByKeyCode: Map<Int, GameControlAction> = emptyMap()
    ): GameControlAction? {
        customBindingsByKeyCode[keyCode]?.let { return it }

        return when (keyCode) {
            // Directional controls: Arrow keys + WASD
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_W -> GameControlAction.MOVE_UP

            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_S -> GameControlAction.MOVE_DOWN

            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_A -> GameControlAction.MOVE_LEFT

            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_D -> GameControlAction.MOVE_RIGHT

            // Action triggers
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_E,
            KeyEvent.KEYCODE_Z -> GameControlAction.PRIMARY_ACTION

            KeyEvent.KEYCODE_Q,
            KeyEvent.KEYCODE_X,
            KeyEvent.KEYCODE_U -> GameControlAction.SECONDARY_ACTION

            KeyEvent.KEYCODE_P,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> GameControlAction.PAUSE_TOGGLE

            KeyEvent.KEYCODE_R -> GameControlAction.RESTART_GAME

            // Navigation triggers
            KeyEvent.KEYCODE_ESCAPE -> GameControlAction.NAV_BACK

            KeyEvent.KEYCODE_1,
            KeyEvent.KEYCODE_NUMPAD_1 -> GameControlAction.NAV_TAB_1

            KeyEvent.KEYCODE_2,
            KeyEvent.KEYCODE_NUMPAD_2 -> GameControlAction.NAV_TAB_2

            KeyEvent.KEYCODE_3,
            KeyEvent.KEYCODE_NUMPAD_3 -> GameControlAction.NAV_TAB_3

            KeyEvent.KEYCODE_TAB -> if (isShiftPressed) {
                GameControlAction.NAV_PREV_TAB
            } else {
                GameControlAction.NAV_NEXT_TAB
            }

            else -> null
        }
    }

    fun formatKeyCodeLabel(keyCode: Int): String = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> "Seta ↑"
        KeyEvent.KEYCODE_DPAD_DOWN -> "Seta ↓"
        KeyEvent.KEYCODE_DPAD_LEFT -> "Seta ←"
        KeyEvent.KEYCODE_DPAD_RIGHT -> "Seta →"
        KeyEvent.KEYCODE_DPAD_CENTER -> "Centro D-Pad"
        KeyEvent.KEYCODE_SPACE -> "Espaço"
        KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> "Enter"
        KeyEvent.KEYCODE_ESCAPE -> "Esc"
        KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> "Shift"
        KeyEvent.KEYCODE_TAB -> "Tab"
        else -> {
            val raw = KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
            if (raw.length == 1) raw else raw.replace('_', ' ')
        }
    }

    fun isDirectionalAction(action: GameControlAction): Boolean = when (action) {
        GameControlAction.MOVE_UP,
        GameControlAction.MOVE_DOWN,
        GameControlAction.MOVE_LEFT,
        GameControlAction.MOVE_RIGHT -> true
        else -> false
    }

    fun isNavigationAction(action: GameControlAction): Boolean = when (action) {
        GameControlAction.NAV_BACK,
        GameControlAction.NAV_TAB_1,
        GameControlAction.NAV_TAB_2,
        GameControlAction.NAV_TAB_3,
        GameControlAction.NAV_NEXT_TAB,
        GameControlAction.NAV_PREV_TAB -> true
        else -> false
    }

    /**
     * Computes a normalized 2D movement vector from the set of currently held
     * directional actions so diagonal movement (e.g. W+D) maintains unit speed.
     */
    fun computeNormalizedMovement(heldActions: Set<GameControlAction>): MovementVector {
        var dx = 0f
        var dy = 0f
        if (GameControlAction.MOVE_LEFT in heldActions) dx -= 1f
        if (GameControlAction.MOVE_RIGHT in heldActions) dx += 1f
        if (GameControlAction.MOVE_UP in heldActions) dy -= 1f
        if (GameControlAction.MOVE_DOWN in heldActions) dy += 1f

        if (dx == 0f && dy == 0f) return MovementVector(0f, 0f)
        val length = hypot(dx, dy)
        return MovementVector(x = dx / length, y = dy / length)
    }
}
