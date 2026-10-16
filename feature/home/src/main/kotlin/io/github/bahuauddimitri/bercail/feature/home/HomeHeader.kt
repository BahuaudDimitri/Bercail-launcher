package io.github.bahuauddimitri.bercail.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRowSwipe
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSwipeBlock
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcText
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextColor
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextStyle
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTimeline
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTimelineMark
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcWeatherLine
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.agenda.DayLine
import io.github.bahuauddimitri.bercail.core.domain.screen.HomePage

/**
 * The top of the home screen: weather in one line, the next appointment, the one after. Touching it opens the day
 * ([onOpenDay] is given nothing: no appointment in particular), or, with nothing left to come today, the phone's
 * calendar app ([onOpenCalendar]). Sliding it to the right always opens the calendar app.
 */
@Composable
internal fun HomeHeader(
    state: HomeUiState,
    onOpenDay: (String?) -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val calendar = state.glance.leadsToCalendar
    Column(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = SIDE, top = TOP, end = TIMELINE_ROOM)
    ) {
        BcSwipeBlock(
            swipe = BcRowSwipe(OPEN_CALENDAR, onOpenCalendar),
            onClick = { if (calendar) onOpenCalendar() else onOpenDay(null) },
            onClickLabel = if (calendar) OPEN_CALENDAR else "Voir la journée",
            verticalArrangement = Arrangement.spacedBy(LINE_GAP)
        ) {
            state.weatherLine?.let { BcWeatherLine(it, state.weather.look, Modifier.padding(bottom = WEATHER_GAP)) }
            BcText(state.glance.countdown, style = BcTextStyle.LabelStrong, color = BcTextColor.Muted, maxLines = 1)
            BcText(state.glance.title, style = BcTextStyle.AgendaTitle, maxLines = 1)
            state.glance.after?.let { BcText(it, style = BcTextStyle.Label, color = BcTextColor.Muted, maxLines = 1) }
        }
    }
}

/**
 * The day along the right edge, rising from the top edge of the screen (7 am under the status bar) to midnight
 * above the drawer, in the accent of the moment.
 * It fades and retracts on the Listen screen. Sliding a finger along it names each appointment, and lifting it opens
 * the day on the one reached.
 */
@Composable
internal fun VerticalTimeline(
    state: HomeUiState,
    onOpenDay: (String?) -> Unit,
    modifier: Modifier = Modifier,
    titlesBelow: Dp = 0.dp
) {
    val motion = BcTheme.motion
    AnimatedVisibility(
        visible = state.page == HomePage.Home,
        modifier = modifier,
        enter = fadeIn(tween(motion.fadeMillis)) +
            expandVertically(tween(motion.timelineMillis, easing = motion.easing), expandFrom = Alignment.Top),
        exit = fadeOut(tween(motion.fadeMillis)) +
            shrinkVertically(tween(motion.timelineMillis, easing = motion.easing), shrinkTowards = Alignment.Top)
    ) {
        BcTimeline(
            now = state.dayLine.now,
            marks = state.dayLine.timelineMarks,
            // The line rises to the top edge; 7 am waits under the status bar, so that nothing of the day hides there.
            headroom = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + UNDER_STATUS_BAR,
            // Beside the top of the screen, where its own title stands, the line only writes the times.
            titlesBelow = titlesBelow,
            description = TIMELINE_DESCRIPTION,
            onOpen = { onOpenDay(it?.key) },
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = VERTICAL_EDGE, bottom = VERTICAL_BOTTOM)
                .width(VERTICAL_WIDTH)
                .testTag(TIMELINE_TAG)
        )
    }
}

private val DayLine.timelineMarks
    get() = marks.map { BcTimelineMark(it.position, it.time, it.next, it.title, key = it.id, brief = it.brief) }

private const val TIMELINE_DESCRIPTION = "Ligne du temps de la journée"
private const val OPEN_CALENDAR = "Ouvrir l'agenda"
internal val SIDE = 22.dp
private val TOP = 18.dp
private val TIMELINE_ROOM = 96.dp
private val LINE_GAP = 3.dp
private val WEATHER_GAP = 6.dp

/** Room between the status bar and 7 am: the ring of now, at 7 am, stays clear of the bar. */
private val UNDER_STATUS_BAR = 14.dp
private val VERTICAL_EDGE = 12.dp
private val VERTICAL_BOTTOM = 24.dp
private val VERTICAL_WIDTH = 78.dp
