package com.example.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.input.GameControlAction
import com.example.input.KeyboardControlMapper
import com.example.input.LocalKeyboardInputService
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.JetBrainsMonoFontFamily

private data class ControlBindingSpec(
    val action: GameControlAction,
    val title: String,
    val touchHint: String
)

private val REMAPPABLE_CONTROLS = listOf(
    ControlBindingSpec(GameControlAction.MOVE_UP, "Mover para Cima", "Toque / D-Pad ↑"),
    ControlBindingSpec(GameControlAction.MOVE_DOWN, "Mover para Baixo", "Toque / D-Pad ↓"),
    ControlBindingSpec(GameControlAction.MOVE_LEFT, "Mover para Esquerda", "Toque / D-Pad ←"),
    ControlBindingSpec(GameControlAction.MOVE_RIGHT, "Mover para Direita", "Toque / D-Pad →"),
    ControlBindingSpec(GameControlAction.PRIMARY_ACTION, "Ação Principal", "Toque / Botão A / Confirmar"),
    ControlBindingSpec(GameControlAction.SECONDARY_ACTION, "Ação Secundária", "Botão B / Alternar Modo"),
    ControlBindingSpec(GameControlAction.PAUSE_TOGGLE, "Pausar / Retomar", "Botão Pausa na tela"),
    ControlBindingSpec(GameControlAction.RESTART_GAME, "Reiniciar Partida", "Botão Reiniciar na tela")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyBindingsScreen(
    keyBindings: Map<GameControlAction, Int>,
    onUpdateBinding: (GameControlAction, Int) -> Unit,
    onResetBindings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingSpec by remember { mutableStateOf<ControlBindingSpec?>(null) }
    val keyboardService = LocalKeyboardInputService.current

    DisposableEffect(editingSpec) {
        val target = editingSpec
        if (target != null) {
            keyboardService.setRawKeyCaptureListener { keyCode ->
                if (keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
                    onUpdateBinding(target.action, keyCode)
                    editingSpec = null
                    true
                } else {
                    false
                }
            }
        } else {
            keyboardService.setRawKeyCaptureListener(null)
        }
        onDispose {
            keyboardService.setRawKeyCaptureListener(null)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Mapeamento de Teclado") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("key_bindings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 820.dp)
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Keyboard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Controles Toque → Teclado (Ports PC)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Remapeie qualquer comando tátil para uma tecla física ou selecione uma tecla abaixo.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(REMAPPABLE_CONTROLS, key = { it.action.name }) { spec ->
                    val assignedKey = keyBindings[spec.action]
                        ?: KeyboardControlMapper.DEFAULT_REMAPPABLE_BINDINGS[spec.action]
                        ?: KeyEvent.KEYCODE_UNKNOWN
                    val keyLabel = KeyboardControlMapper.formatKeyCodeLabel(assignedKey)

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                            .testTag("binding_row_${spec.action.name}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Gamepad,
                                contentDescription = null,
                                tint = EmeraldPulse,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = spec.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = spec.touchHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElectricAmber.copy(alpha = 0.16f))
                                    .border(1.dp, ElectricAmber.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = keyLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = ElectricAmber
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = { editingSpec = spec },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("remap_${spec.action.name}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Remapear ${spec.title}",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onResetBindings,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("reset_key_bindings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restaurar Padrões (WASD + Espaço)")
                    }
                }
            }
        }
    }

    editingSpec?.let { spec ->
        val currentKey = keyBindings[spec.action]
            ?: KeyboardControlMapper.DEFAULT_REMAPPABLE_BINDINGS[spec.action]
            ?: KeyEvent.KEYCODE_UNKNOWN
        RemapKeyDialog(
            spec = spec,
            currentKeyCode = currentKey,
            onSelectPresetKey = { chosenKey ->
                onUpdateBinding(spec.action, chosenKey)
                editingSpec = null
            },
            onDismiss = { editingSpec = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RemapKeyDialog(
    spec: ControlBindingSpec,
    currentKeyCode: Int,
    onSelectPresetKey: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Remapear: ${spec.title}",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Pressione qualquer tecla no teclado físico agora ou toque em uma tecla abaixo para atribuir ao comando (${spec.touchHint}):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KeyboardControlMapper.PRESET_KEY_OPTIONS.forEach { optionKey ->
                        FilterChip(
                            selected = optionKey == currentKeyCode,
                            onClick = { onSelectPresetKey(optionKey) },
                            label = { Text(KeyboardControlMapper.formatKeyCodeLabel(optionKey)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
