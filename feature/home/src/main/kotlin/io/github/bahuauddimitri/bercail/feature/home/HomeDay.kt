package io.github.bahuauddimitri.bercail.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcButton
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcFrost
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcLeading
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcListRow
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRowEmphasis
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSheet
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcText
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextColor
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextStyle
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.agenda.DayRowState

/**
 * The « Aujourd'hui » sheet: the date, a way to the phone's calendar app, and every appointment of the day,
 * the past ones dimmed, the next one brought forward. Until Bercail may read the calendar, it offers to allow it.
 */
@Composable
internal fun DaySheet(state: HomeUiState, actions: HomeActions, links: HomeLinks, frost: BcFrost?) {
    BcSheet(
        visible = state.dayOpen,
        onDismiss = { actions.onDayOpenChange(false) },
        modifier = Modifier.testTag(DAY_TAG),
        frost = frost
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                BcText("Aujourd'hui", style = BcTextStyle.Headline, maxLines = 1)
                BcText(state.dayDate, style = BcTextStyle.Label, color = BcTextColor.Muted, maxLines = 1)
            }
            BcButton("Agenda", onClick = actions::onOpenCalendar)
        }
        when {
            !state.agendaAllowed -> AskForAgenda(onAsk = { links.onAskAccess(Access.Calendar) })

            state.dayRows.isEmpty() -> BcText(NOTHING_TODAY, style = BcTextStyle.Message, color = BcTextColor.Muted)

            else -> Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                state.dayRows.forEach { row ->
                    BcListRow(
                        title = row.title,
                        onClick = { actions.onOpenEvent(row.id) },
                        leading = BcLeading.Time(row.time),
                        emphasis = row.state.emphasis,
                        titleStyle = BcTextStyle.Body
                    )
                }
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

private val DayRowState.emphasis: BcRowEmphasis
    get() = when (this) {
        DayRowState.Past -> BcRowEmphasis.Dimmed
        DayRowState.Coming -> BcRowEmphasis.Normal
        DayRowState.Highlighted -> BcRowEmphasis.Highlighted
    }

private const val NOTHING_TODAY = "Rien de prévu aujourd'hui"
private const val WHY_THE_AGENDA =
    "Bercail n'a pas encore le droit de lire ton agenda. Il ne fait que le lire, et rien ne quitte le téléphone."
