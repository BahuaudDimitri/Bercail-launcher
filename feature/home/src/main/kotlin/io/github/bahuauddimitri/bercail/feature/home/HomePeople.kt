package io.github.bahuauddimitri.bercail.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcButton
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIcons
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcPersonTile
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSummaryLeading
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSummaryRow
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.messages.LINK_MESSAGES
import io.github.bahuauddimitri.bercail.core.domain.messages.MESSAGES_NOT_LINKED
import io.github.bahuauddimitri.bercail.core.domain.messages.allMessagesDescription

/** The messages line of the folded drawer: who wrote, or that Beeper is not linked yet. */
@Composable
internal fun MessagesLine(state: HomeUiState, onClick: () -> Unit, onClickLabel: String) {
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
        onClickLabel = onClickLabel
    )
}

/**
 * The people of the open drawer: the favorites and "Tous", which shows the messaging app. Until the user lets
 * Bercail read Beeper, the button that asks for it stands in their place.
 */
@Composable
internal fun PeopleRow(state: HomeUiState, links: HomeLinks) {
    if (!state.messagesAllowed) {
        Row(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically) {
            BcButton(LINK_MESSAGES, onClick = { links.onAskAccess(Access.Messages) }, icon = BcIcons.Chat)
        }
        return
    }
    val favoriteIds = state.favorites.map { it.id }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        state.favorites.forEach { person ->
            BcPersonTile(
                name = person.name,
                initials = person.initials,
                pastel = pastelOf(person.id, favoriteIds),
                unread = person.unread,
                onClick = { links.onOpenConversation(person.id) },
                description = person.description
            )
        }
        BcPersonTile(
            name = "Tous",
            initials = "",
            pastel = null,
            unread = state.otherUnread,
            onClick = links::onOpenMessagesApp,
            description = allMessagesDescription(state.otherUnread),
            icon = BcIcons.Chat
        )
    }
}
