package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcType

/**
 * Where an answer is written, at the bottom of a conversation: the microphone when dictation is possible, the
 * field, which grows with the text up to a few lines, and the send button, disabled while there is nothing to send.
 */
@Composable
fun BcReplyField(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Répondre",
    canSend: Boolean = value.isNotBlank(),
    onDictate: (() -> Unit)? = null
) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(GAP)
    ) {
        if (onDictate != null) BcIconButton(BcIcons.Mic, contentDescription = "Dicter", onClick = onDictate)
        Box(
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = FIELD_MIN_HEIGHT)
                .background(BcColors.inset, BcShapes.card)
                .border(1.dp, if (focused) BcColors.glassEdge else BcColors.tile, BcShapes.card)
                .padding(horizontal = FIELD_SIDE, vertical = FIELD_EDGE),
            contentAlignment = Alignment.CenterStart
        ) {
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
                maxLines = MAX_LINES,
                cursorBrush = SolidColor(BcColors.text),
                interactionSource = interactions,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
        }
        BcIconButton(
            BcIcons.Send,
            contentDescription = "Envoyer",
            onClick = onSend,
            variant = BcButtonVariant.Light,
            enabled = canSend
        )
    }
}

private const val MAX_LINES = 4
private val FIELD_MIN_HEIGHT = 44.dp
private val FIELD_SIDE = 16.dp
private val FIELD_EDGE = 10.dp
private val GAP = 8.dp
