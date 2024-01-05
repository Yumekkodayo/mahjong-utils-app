package io.ssttkkl.mahjongutils.app.components.tile

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.coerceIn
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.TextUnit
import dev.icerock.moko.resources.compose.stringResource
import io.ssttkkl.mahjongutils.app.MR
import io.ssttkkl.mahjongutils.app.components.tileime.LocalTileImeHostState
import io.ssttkkl.mahjongutils.app.components.tileime.TileImeHostState
import io.ssttkkl.mahjongutils.app.utils.TileTextSize
import mahjongutils.models.Tile
import mahjongutils.models.toTilesString

private fun TileImeHostState.TileImeConsumer.consume(
    state: CoreTileFieldState,
    valueState: State<List<Tile>>,
    onValueChangeState: State<((List<Tile>) -> Unit)?>
) {
    val value by valueState
    val onValueChange by onValueChangeState

    this.consume(
        handlePendingTile = { tile ->
            state.selection = state.selection.coerceIn(0, value.size)
            val newValue = buildList {
                addAll(value.subList(0, state.selection.start))
                add(tile)

                if (state.selection.end != value.size) {
                    addAll(
                        value.subList(
                            state.selection.end + 1,
                            value.size
                        )
                    )
                }
            }
            onValueChange?.invoke(newValue)
            state.selection = TextRange(state.selection.start + 1)
        },
        handleBackspace = {
            state.selection = state.selection.coerceIn(0, value.size)
            val curCursor = state.selection.start
            if (state.selection.length == 0) {
                if (curCursor - 1 in value.indices) {
                    val newValue = ArrayList(value).apply {
                        removeAt(curCursor - 1)
                    }
                    onValueChange?.invoke(newValue)
                    state.selection = TextRange(curCursor - 1)
                }
            } else {
                val newValue = buildList {
                    addAll(value.subList(0, state.selection.start))

                    if (state.selection.end != value.size) {
                        addAll(
                            value.subList(
                                state.selection.end + 1,
                                value.size
                            )
                        )
                    }
                }
                onValueChange?.invoke(newValue)
                state.selection = TextRange(curCursor)
            }
        }
    )
}

@Composable
fun BaseTileField(
    value: List<Tile>,
    onValueChange: (List<Tile>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fontSize: TextUnit = TileTextSize.Default.bodyLarge,
    isError: Boolean = false,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val state = remember(interactionSource) {
        CoreTileFieldState(interactionSource)
    }

    val currentValueState = rememberUpdatedState(value)
    val currentOnValueChangeState = rememberUpdatedState(onValueChange)

    val tileImeHostState = LocalTileImeHostState.current
    val consumer = remember(state, tileImeHostState) {
        tileImeHostState.TileImeConsumer()
    }

    LaunchedEffect(value) {
        // 限制selection在值范围内
        state.selection = state.selection.coerceIn(0, value.size)
    }

    // 退出时隐藏键盘
    DisposableEffect(consumer) {
        onDispose {
            consumer.release()
        }
    }

    // 用户收起键盘后再点击输入框，重新弹出
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    LaunchedEffect(enabled && focused && pressed, consumer) {
        if (enabled && focused && pressed) {
            consumer.consume(state, currentValueState, currentOnValueChangeState)
        }
    }

    // 绑定键盘到该输入框
    DisposableEffect(enabled && focused, consumer) {
        if (enabled && focused) {
            consumer.consume(state, currentValueState, currentOnValueChangeState)

            onDispose {
                consumer.release()
            }
        } else {
            consumer.release()
            onDispose { }
        }
    }

    val cursorColor: Color = MaterialTheme.colorScheme.primary
    val errorCursorColor: Color = MaterialTheme.colorScheme.error

    CoreTileField(
        value = value,
        modifier = modifier,
        state = state,
        cursorColor = if (isError) errorCursorColor else cursorColor,
        fontSizeInSp = if (fontSize.isSp)
            fontSize.value
        else
            LocalTextStyle.current.fontSize.value
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TileField(
    value: List<Tile>,
    onValueChange: (List<Tile>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fontSize: TextUnit = TileTextSize.Default.bodyLarge,
    label: @Composable (() -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val shape = OutlinedTextFieldDefaults.shape
    val colors = OutlinedTextFieldDefaults.colors()

    val decorationBox = @Composable { innerTextField: @Composable () -> Unit ->
        OutlinedTextFieldDefaults.DecorationBox(
            value = value.toTilesString(),
            visualTransformation = VisualTransformation.None,
            innerTextField = innerTextField,
            placeholder = placeholder,
            label = label,
            trailingIcon = {
                Text(
                    stringResource(MR.strings.text_tiles_num_short, value.size),
                    style = MaterialTheme.typography.labelMedium
                )
            },
            supportingText = supportingText,
            singleLine = true,
            enabled = enabled,
            isError = isError,
            interactionSource = interactionSource,
            colors = colors,
            container = {
                OutlinedTextFieldDefaults.ContainerBox(
                    enabled,
                    isError,
                    interactionSource,
                    colors,
                    shape
                )
            }
        )
    }
    decorationBox {
        BaseTileField(
            value,
            onValueChange,
            modifier,
            enabled = enabled,
            fontSize = fontSize,
            isError = isError,
            interactionSource = interactionSource
        )
    }
}
