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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcType

/**
 * Where an answer is written, at the bottom of a conversation: the field, a sunken pill like the "Chercher" bar that
 * stays a pill as it grows with the text up to four lines, and the round send button filled with the accent of the
 * moment, disabled while there is nothing to send.
 */
@Composable
fun BcReplyField(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Répondre",
    canSend: Boolean = value.isNotBlank()
) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(GAP)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .testTag(REPLY_FIELD_TAG)
                .defaultMinSize(minHeight = FIELD_MIN_HEIGHT)
                .background(BcColors.inset, BcShapes.pill)
                .border(1.dp, if (focused) BcColors.fieldEdgeFocused else BcColors.fieldEdge, BcShapes.pill)
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
                cursorBrush = SolidColor(BcTheme.moment.accent),
                interactionSource = interactions,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
        }
        BcIconButton(
            BcIcons.Send,
            contentDescription = "Envoyer",
            onClick = onSend,
            variant = BcIconButtonVariant.Accent,
            enabled = canSend,
            size = SEND_SIZE
        )
    }
}

internal const val REPLY_FIELD_TAG = "reply-field"
private const val MAX_LINES = 4
private val FIELD_MIN_HEIGHT = 48.dp
private val SEND_SIZE = 48.dp
private val FIELD_SIDE = 18.dp
private val FIELD_EDGE = 12.dp
private val GAP = 10.dp
