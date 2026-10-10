package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcType

/**
 * The "Chercher" bar, which is the field itself: sunken pill, magnifier, clear button once something is typed.
 * Disabled, it keeps its place and its look but takes no focus and no text.
 */
@Composable
fun BcSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Chercher",
    onFocusChange: (Boolean) -> Unit = {},
    onSearch: () -> Unit = {},
    enabled: Boolean = true
) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    LaunchedEffect(focused) { onFocusChange(focused) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(FIELD_HEIGHT)
            .background(BcColors.inset, BcShapes.pill)
            .border(1.dp, if (focused) BcColors.glassEdge else BcColors.tile, BcShapes.pill)
            .padding(start = START_PADDING, end = END_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GAP)
    ) {
        BcIcon(BcIcons.Search, contentDescription = null, size = ICON, tint = BcTextColor.Muted)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                BcText(
                    placeholder,
                    modifier = Modifier.clearAndSetSemantics {},
                    style = BcTextStyle.Message,
                    color = BcTextColor.Muted,
                    maxLines = 1
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = placeholder },
                textStyle = BcType.message.copy(color = BcColors.text),
                enabled = enabled,
                singleLine = true,
                cursorBrush = SolidColor(BcColors.text),
                interactionSource = interactions,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() })
            )
        }
        if (value.isNotEmpty()) {
            BcIconButton(BcIcons.Close, contentDescription = "Effacer", onClick = {
                onValueChange("")
            }, variant = BcButtonVariant.Ghost)
        }
    }
}

private val FIELD_HEIGHT = 46.dp
private val START_PADDING = 18.dp
private val END_PADDING = 2.dp
private val GAP = 10.dp
private val ICON = 16.dp
