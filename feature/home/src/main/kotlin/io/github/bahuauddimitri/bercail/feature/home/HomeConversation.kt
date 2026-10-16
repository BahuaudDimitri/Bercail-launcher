package io.github.bahuauddimitri.bercail.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcBubble
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcButton
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcFrost
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcReplyField
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSheet
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSheetHeader
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcText
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextColor
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextStyle
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcUndoCountdown
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import io.github.bahuauddimitri.bercail.core.domain.messages.via

/**
 * The sheet of a conversation: who and through which network, a way to the messaging app, the last messages (the
 * latest always in view), and the field to answer, by hand or by voice. Nothing is written for the user.
 */
@Composable
internal fun TalkSheet(state: HomeUiState, actions: HomeActions, links: HomeLinks, frost: BcFrost?) {
    // While the sheet slides away, it keeps showing the conversation it was closed on.
    val kept = remember { arrayOfNulls<TalkUiState>(1) }
    state.talk?.let { kept[0] = it }
    BcSheet(
        visible = state.talk != null,
        onDismiss = actions::onCloseConversation,
        modifier = Modifier.testTag(TALK_TAG),
        frost = frost
    ) {
        val talk = state.talk ?: kept[0] ?: return@BcSheet
        BcSheetHeader(
            title = talk.person.name,
            subtitle = talk.person.via,
            action = MESSAGING_APP,
            onAction = links::onOpenMessagesApp
        )
        Messages(talk, pastelOf(talk.person.id, state.favorites.map { it.id }), actions::onUndoSend)
        if (talk.notSent) BcText(NOT_SENT, style = BcTextStyle.Label, color = BcTextColor.Muted)
        BcReplyField(
            value = talk.draft,
            onValueChange = actions::onDraftChange,
            onSend = actions::onSend,
            canSend = talk.canSend
        )
    }
}

/**
 * The messages, the oldest at the top. The list starts at its end and stays there: the last message is always in
 * view, and the older ones scroll inside the sheet.
 */
@Composable
private fun ColumnScope.Messages(talk: TalkUiState, pastel: BcPastel, onUndo: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState(), reverseScrolling = true),
        verticalArrangement = Arrangement.spacedBy(BcSpacing.xs)
    ) {
        when {
            talk.loading -> BcText(READING, style = BcTextStyle.Message, color = BcTextColor.Muted)

            talk.bubbles.isEmpty() -> BcText(NO_MESSAGE, style = BcTextStyle.Message, color = BcTextColor.Muted)

            else -> talk.bubbles.forEach { bubble ->
                val side = Modifier.align(if (bubble.mine) Alignment.End else Alignment.Start)
                BcBubble(bubble.text, bubble.author, bubble.mine, pastel, side, header = bubble.header)
                if (bubble.id == talk.undoable) {
                    key(bubble.id) { BcUndoCountdown(UNDO_MILLIS, onUndo, Modifier.align(Alignment.End)) }
                }
            }
        }
    }
}

internal const val TALK_TAG = "home-talk"
private const val MESSAGING_APP = "Beeper"
private const val READING = "Lecture de la conversation…"
private const val NO_MESSAGE = "Pas encore de message."
private const val NOT_SENT = "Le message n'est pas parti : il est revenu dans le champ."

/** The time « Annuler l'envoi » stays, as its ring shows it. */
private const val UNDO_MILLIS = 6_000
