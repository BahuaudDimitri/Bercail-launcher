package io.github.bahuauddimitri.bercail.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcButton
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIcons
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRing
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRingMark
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRingTone
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRowSwipe
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSummaryLeading
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSummaryRow
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.messages.LINK_MESSAGES
import io.github.bahuauddimitri.bercail.core.domain.messages.MESSAGES_NOT_LINKED
import io.github.bahuauddimitri.bercail.core.domain.messages.allMessagesDescription

/**
 * The messages line of the folded drawer: who wrote, or that Beeper is not linked yet. Once it is, sliding the line
 * to the right shows the messaging app ([onOpenApp]).
 */
@Composable
internal fun MessagesLine(state: HomeUiState, onClick: () -> Unit, onClickLabel: String, onOpenApp: () -> Unit) {
    if (!state.messagesAllowed) {
        BcSummaryRow(MESSAGES_NOT_LINKED, BcSummaryLeading.Icon(BcIcons.Chat), onClick, onClickLabel = onClickLabel)
        return
    }
    val favoriteIds = state.favorites.map { it.id }
    BcSummaryRow(
        text = state.messages.detail,
        strong = state.messages.headline,
        leading = BcSummaryLeading.Dots(state.messages.senders.map { pastelOf(it.id, favoriteIds) }),
        onClick = onClick,
        onClickLabel = onClickLabel,
        swipe = BcRowSwipe(OPEN_MESSAGES_APP, onOpenApp)
    )
}

private const val OPEN_MESSAGES_APP = "Ouvrir Beeper"

/**
 * The people of the open drawer: the favorites and "Tous", which shows the messaging app. Until the user lets
 * Bercail read Beeper, the button that asks for it stands in their place.
 */
@Composable
internal fun PeopleRow(state: HomeUiState, actions: HomeActions, links: HomeLinks) {
    if (!state.messagesAllowed) {
        Row(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically) {
            BcButton(LINK_MESSAGES, onClick = { links.onAskAccess(Access.Messages) }, icon = BcIcons.Chat)
        }
        return
    }
    val favoriteIds = state.favorites.map { it.id }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        state.favorites.forEach { person ->
            BcRing(
                label = person.name,
                mark = BcRingMark.Initial(person.initials),
                onClick = { actions.onOpenConversation(person.id) },
                tone = BcRingTone.Pastel(pastelOf(person.id, favoriteIds)),
                unread = person.unread,
                description = person.description
            )
        }
        BcRing(
            label = "Tous",
            mark = BcRingMark.Icon(BcIcons.Chat),
            onClick = links::onOpenMessagesApp,
            unread = state.otherUnread,
            description = allMessagesDescription(state.otherUnread)
        )
    }
}
