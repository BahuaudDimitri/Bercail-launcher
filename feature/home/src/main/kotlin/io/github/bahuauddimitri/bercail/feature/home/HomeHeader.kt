package io.github.bahuauddimitri.bercail.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcText
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextColor
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextStyle
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTimeline
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTimelineMark
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTimelineOrientation
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcWeatherLine
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.agenda.DayLine
import io.github.bahuauddimitri.bercail.core.domain.screen.HomePage
import io.github.bahuauddimitri.bercail.core.domain.settings.TimelineOrientation

/** The top of the home screen: weather in one line, the next appointment, the one after. Touching it opens the day. */
@Composable
internal fun HomeHeader(state: HomeUiState, onOpenDay: () -> Unit, modifier: Modifier = Modifier) {
    val vertical = state.timeline == TimelineOrientation.Vertical
    Column(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = SIDE, top = TOP, end = if (vertical) TIMELINE_ROOM else SIDE)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = "Voir la journée", role = Role.Button, onClick = onOpenDay),
            verticalArrangement = Arrangement.spacedBy(LINE_GAP)
        ) {
            state.weatherLine?.let { BcWeatherLine(it, state.weather.look, Modifier.padding(bottom = WEATHER_GAP)) }
            BcText(state.glance.countdown, style = BcTextStyle.LabelStrong, color = BcTextColor.Muted, maxLines = 1)
            BcText(state.glance.title, style = BcTextStyle.AgendaTitle, maxLines = 1)
            state.glance.after?.let { BcText(it, style = BcTextStyle.Label, color = BcTextColor.Muted, maxLines = 1) }
        }
        AnimatedVisibility(visible = !vertical && state.page == HomePage.Home) {
            BcTimeline(
                now = state.dayLine.now,
                marks = state.dayLine.timelineMarks,
                orientation = BcTimelineOrientation.Horizontal,
                description = TIMELINE_DESCRIPTION,
                modifier = Modifier.fillMaxWidth().padding(
                    top = HORIZONTAL_GAP
                ).height(HORIZONTAL_LINE).testTag(TIMELINE_TAG)
            )
        }
    }
}

/**
 * The day along the right edge, from 7 am under the status bar to midnight above the drawer.
 * It fades and retracts on the Listen screen.
 */
@Composable
internal fun VerticalTimeline(state: HomeUiState, modifier: Modifier = Modifier) {
    val motion = BcTheme.motion
    AnimatedVisibility(
        visible = state.timeline == TimelineOrientation.Vertical && state.page == HomePage.Home,
        modifier = modifier,
        enter = fadeIn(tween(motion.fadeMillis)) +
            expandVertically(tween(motion.timelineMillis, easing = motion.easing), expandFrom = Alignment.Top),
        exit = fadeOut(tween(motion.fadeMillis)) +
            shrinkVertically(tween(motion.timelineMillis, easing = motion.easing), shrinkTowards = Alignment.Top)
    ) {
        BcTimeline(
            now = state.dayLine.now,
            marks = state.dayLine.timelineMarks,
            description = TIMELINE_DESCRIPTION,
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .padding(top = VERTICAL_TOP, end = VERTICAL_EDGE, bottom = VERTICAL_BOTTOM)
                .width(VERTICAL_WIDTH)
                .testTag(TIMELINE_TAG)
        )
    }
}

private val DayLine.timelineMarks
    get() = marks.map { BcTimelineMark(position = it.position, label = it.time, next = it.next) }

private const val TIMELINE_DESCRIPTION = "Ligne du temps de la journée"
internal val SIDE = 22.dp
private val TOP = 18.dp
private val TIMELINE_ROOM = 96.dp
private val LINE_GAP = 3.dp
private val WEATHER_GAP = 6.dp
private val HORIZONTAL_GAP = 10.dp
private val HORIZONTAL_LINE = 44.dp
private val VERTICAL_TOP = 34.dp
private val VERTICAL_EDGE = 12.dp
private val VERTICAL_BOTTOM = 24.dp
private val VERTICAL_WIDTH = 78.dp
