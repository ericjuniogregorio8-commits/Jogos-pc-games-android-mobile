package com.example.input

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global keyboard input listener service that intercepts hardware/emulator key events
 * at the application/activity level, maps arrow keys and WASD to standard character
 * controls, tracks held directional keys for smooth 60fps movement, and dispatches
 * discrete action and navigation triggers to prioritized listeners.
 */
class GlobalKeyboardInputService {

    interface RegistrationHandle {
        fun unregister()
    }

    private data class RegisteredListener(
        val id: Long,
        val priority: Int,
        val isGameListener: Boolean,
        val onAction: (GameControlAction) -> Boolean
    )

    private var nextListenerId = 1L
    private val listeners = mutableListOf<RegisteredListener>()
    private val heldKeyToAction = LinkedHashMap<Int, GameControlAction>()
    private var attachedApplication: Application? = null

    private val _heldActions = MutableStateFlow<Set<GameControlAction>>(emptySet())
    val heldActions: StateFlow<Set<GameControlAction>> = _heldActions.asStateFlow()

    private val _movementVector = MutableStateFlow(MovementVector())
    val movementVector: StateFlow<MovementVector> = _movementVector.asStateFlow()

    private val _isKeyboardActive = MutableStateFlow(false)
    val isKeyboardActive: StateFlow<Boolean> = _isKeyboardActive.asStateFlow()

    private val _lastTriggeredAction = MutableStateFlow<GameControlAction?>(null)
    val lastTriggeredAction: StateFlow<GameControlAction?> = _lastTriggeredAction.asStateFlow()

    private val _events = MutableSharedFlow<KeyboardInputEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<KeyboardInputEvent> = _events.asSharedFlow()

    @Volatile
    var isTextInputFocused: Boolean = false
        private set

    @Volatile
    private var customBindingsByKeyCode: Map<Int, GameControlAction> = emptyMap()

    @Volatile
    private var rawKeyCaptureListener: ((Int) -> Boolean)? = null

    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) {
            clearHeldKeys()
        }
        override fun onActivityStopped(activity: Activity) {
            clearHeldKeys()
        }
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) {
            clearHeldKeys()
        }
    }

    fun attachToApplication(application: Application) {
        if (attachedApplication === application) return
        attachedApplication?.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        attachedApplication = application
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    fun setTextInputFocused(focused: Boolean) {
        isTextInputFocused = focused
        if (focused) {
            clearHeldKeys()
        }
    }

    fun markPointerInteraction() {
        _isKeyboardActive.value = false
    }

    fun updateCustomBindings(bindingsByAction: Map<GameControlAction, Int>) {
        val inverted = linkedMapOf<Int, GameControlAction>()
        bindingsByAction.forEach { (action, keyCode) ->
            inverted[keyCode] = action
        }
        customBindingsByKeyCode = inverted
    }

    fun setRawKeyCaptureListener(listener: ((Int) -> Boolean)?) {
        rawKeyCaptureListener = listener
    }

    fun registerListener(
        priority: Int = PRIORITY_GAME,
        isGameListener: Boolean = true,
        onAction: (GameControlAction) -> Boolean
    ): RegistrationHandle {
        val id = nextListenerId++
        val entry = RegisteredListener(
            id = id,
            priority = priority,
            isGameListener = isGameListener,
            onAction = onAction
        )
        listeners.add(entry)
        listeners.sortWith(compareByDescending<RegisteredListener> { it.priority }.thenByDescending { it.id })
        return object : RegistrationHandle {
            override fun unregister() {
                listeners.removeAll { it.id == id }
                if (isGameListener) {
                    clearHeldKeys()
                }
            }
        }
    }

    fun hasActiveGameListener(): Boolean = listeners.any { it.isGameListener }

    /**
     * Processes a raw Android [KeyEvent]. Returns `true` if a listener or active
     * game control consumed the event so default view focus traversal does not steal it.
     */
    fun onKeyEvent(event: KeyEvent): Boolean {
        val capture = rawKeyCaptureListener
        if (capture != null && event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            if (capture(event.keyCode)) {
                return true
            }
        }

        val action = KeyboardControlMapper.mapKeyCodeToAction(
            keyCode = event.keyCode,
            isShiftPressed = event.isShiftPressed,
            customBindingsByKeyCode = customBindingsByKeyCode
        ) ?: return false

        // While typing into a text input (e.g., search field), let the IME/TextField
        // consume normal keys, except Escape which can dismiss/unfocus.
        if (isTextInputFocused && action != GameControlAction.NAV_BACK) {
            return false
        }

        return when (event.action) {
            KeyEvent.ACTION_DOWN -> handleKeyDown(event, action)
            KeyEvent.ACTION_UP -> handleKeyUp(event, action)
            else -> false
        }
    }

    private fun handleKeyDown(event: KeyEvent, action: GameControlAction): Boolean {
        _isKeyboardActive.value = true
        _lastTriggeredAction.value = action

        if (KeyboardControlMapper.isDirectionalAction(action)) {
            val previousSize = heldKeyToAction.size
            heldKeyToAction[event.keyCode] = action
            if (heldKeyToAction.size != previousSize || _heldActions.value != heldKeyToAction.values.toSet()) {
                syncHeldMovement()
            }
        }

        val isRepeat = event.repeatCount > 0
        if (!isRepeat) {
            _events.tryEmit(KeyboardInputEvent(action = action, keyCode = event.keyCode, isRepeat = false))
            for (listener in listeners.toList()) {
                if (listener.onAction(action)) {
                    return true
                }
            }
        }

        // Keep directional/game keys consumed during key repeat when a game is active
        // so Android focus navigation doesn't steal focus out of the game arena.
        if (hasActiveGameListener() && !KeyboardControlMapper.isNavigationAction(action)) {
            return true
        }
        return false
    }

    private fun handleKeyUp(event: KeyEvent, action: GameControlAction): Boolean {
        if (heldKeyToAction.remove(event.keyCode) != null) {
            syncHeldMovement()
        }
        if (hasActiveGameListener() && !KeyboardControlMapper.isNavigationAction(action)) {
            return true
        }
        return false
    }

    fun clearHeldKeys() {
        if (heldKeyToAction.isNotEmpty()) {
            heldKeyToAction.clear()
            syncHeldMovement()
        }
    }

    fun resetForTest() {
        listeners.clear()
        heldKeyToAction.clear()
        isTextInputFocused = false
        _heldActions.value = emptySet()
        _movementVector.value = MovementVector()
        _isKeyboardActive.value = false
        _lastTriggeredAction.value = null
    }

    private fun syncHeldMovement() {
        val actions = heldKeyToAction.values.toSet()
        _heldActions.value = actions
        _movementVector.value = KeyboardControlMapper.computeNormalizedMovement(actions)
    }

    companion object {
        const val PRIORITY_GLOBAL_NAV = 0
        const val PRIORITY_SCREEN = 5
        const val PRIORITY_GAME = 10
        const val PRIORITY_OVERLAY = 20

        val instance: GlobalKeyboardInputService by lazy { GlobalKeyboardInputService() }
    }
}

val LocalKeyboardInputService = staticCompositionLocalOf { GlobalKeyboardInputService.instance }

/**
 * Registers a scoped keyboard action handler while this composable is active in the composition.
 */
@Composable
fun KeyboardInputEffect(
    priority: Int = GlobalKeyboardInputService.PRIORITY_GAME,
    isGameListener: Boolean = true,
    enabled: Boolean = true,
    onAction: (GameControlAction) -> Boolean
) {
    val inputService = LocalKeyboardInputService.current
    val currentOnAction by rememberUpdatedState(onAction)

    DisposableEffect(inputService, priority, isGameListener, enabled) {
        if (!enabled) {
            onDispose {}
        } else {
            val handle = inputService.registerListener(
                priority = priority,
                isGameListener = isGameListener,
                onAction = { action -> currentOnAction(action) }
            )
            onDispose {
                handle.unregister()
            }
        }
    }
}
