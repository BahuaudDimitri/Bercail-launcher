package io.github.bahuauddimitri.bercail.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcButton
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcDayEntry
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcDayList
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcDayMark
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcFrost
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSheet
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSheetHeader
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcText
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextColor
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextStyle
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.agenda.DayRow
import io.github.bahuauddimitri.bercail.core.domain.agenda.DayRowState

/**
 * The « Aujourd'hui » sheet: the date, a way to the phone's calendar app, and every appointment of the day along a
 * small line of the day, the past ones dimmed, the next one circled in the accent. Opened on an appointment (from
 * the search or the line), it scrolls to it. Until Bercail may read the calendar, it offers to allow it.
 */
@Composable
internal fun DaySheet(state: HomeUiState, actions: HomeActions, links: HomeLinks, frost: BcFrost?) {
    BcSheet(
        visible = state.dayOpen,
        onDismiss = { actions.onDayOpenChange(false) },
        modifier = Modifier.testTag(DAY_TAG),
        frost = frost
    ) {
        BcSheetHeader("Aujourd'hui", subtitle = state.dayDate, action = "Agenda", onAction = actions::onOpenCalendar)
        when {
            !state.agendaAllowed -> AskForAgenda(onAsk = { links.onAskAccess(Access.Calendar) })

            state.dayRows.isEmpty() -> BcText(NOTHING_TODAY, style = BcTextStyle.Message, color = BcTextColor.Muted)

            else -> {
                val entries = remember(state.dayRows) { state.dayRows.map { it.entry } }
                BcDayList(
                    entries = entries,
                    onOpen = actions::onOpenEvent,
                    modifier = Modifier.weight(1f, fill = false),
                    focus = state.dayFocus
                )
            }
        }
    }
}

@Composable
private fun AskForAgenda(onAsk: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(BcSpacing.m)) {
        BcText(WHY_THE_AGENDA, style = BcTextStyle.Message, color = BcTextColor.Muted)
        BcButton("Autoriser la lecture de l'agenda", onClick = onAsk)
    }
}

private val DayRow.entry: BcDayEntry
    get() = BcDayEntry(
        key = id,
        time = time,
        title = title,
        mark = when {
            allDay -> BcDayMark.AllDay
            state == DayRowState.Past -> BcDayMark.Past
            state == DayRowState.Next -> BcDayMark.Next
            else -> BcDayMark.Coming
        }
    )

private const val NOTHING_TODAY = "Rien de prévu aujourd'hui"
private const val WHY_THE_AGENDA =
    "Bercail n'a pas encore le droit de lire ton agenda. Il ne fait que le lire, et rien ne quitte le téléphone."
