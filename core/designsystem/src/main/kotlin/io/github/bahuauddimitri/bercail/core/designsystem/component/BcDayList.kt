package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.designsystem.theme.LocalBcContentColor

/**
 * How an appointment stands on the small line of the day. Past: its dot in the accent, its title dimmed. Next: the
 * next one to begin, circled in the accent, its time in the accent. Coming: a white dot. AllDay: a hollow ring.
 */
enum class BcDayMark { Past, Next, Coming, AllDay }

/** One appointment of the day: [key] names it to [BcDayList]'s `onOpen`; [time] is « 09:30 » or « Journée ». */
@Immutable
data class BcDayEntry(val key: String, val time: String, val title: String, val mark: BcDayMark)

/**
 * The appointments of the day as a small line of the day: a hairline on the left (white 22 %) with a dot for each
 * row, then the time (13 sp, in a 54 dp column) and the title (17 sp). Rows are 48 dp, with no background; touching
 * one calls [onOpen] with its key. The list scrolls by itself; opened on [focus], it scrolls to that appointment,
 * which keeps the look its [BcDayMark] gives it.
 */
@Composable
fun BcDayList(
    entries: List<BcDayEntry>,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    focus: String? = null
) {
    val scroll = rememberScrollState()
    val rowPx = with(LocalDensity.current) { ROW_HEIGHT.roundToPx() }
    val focused = entries.indexOfFirst { it.key == focus }
    LaunchedEffect(focus, focused) { if (focused > 0) scroll.scrollTo(focused * rowPx) }
    val accent = BcTheme.moment.accent
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(scroll)
            .drawBehind { drawDayLine(entries, accent) }
            .padding(start = LINE_ROOM)
    ) {
        entries.forEach { entry -> DayRow(entry, accent) { onOpen(entry.key) } }
    }
}

@Composable
private fun DayRow(entry: BcDayEntry, accent: Color, onClick: () -> Unit) {
    val title = when (entry.mark) {
        BcDayMark.Past -> BcColors.textMuted.copy(alpha = PAST_ALPHA)
        BcDayMark.AllDay -> BcColors.textMuted
        BcDayMark.Next, BcDayMark.Coming -> BcColors.text
    }
    val time = if (entry.mark == BcDayMark.Next) accent else BcColors.textMuted
    Row(
        Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .clip(BcShapes.tile)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ROW_GAP)
    ) {
        CompositionLocalProvider(LocalBcContentColor provides time) {
            BcText(entry.time, Modifier.width(TIME_WIDTH), style = BcTextStyle.Caption, maxLines = 1)
        }
        CompositionLocalProvider(LocalBcContentColor provides title) {
            BcText(entry.title, Modifier.weight(1f), style = BcTextStyle.ListTitle, maxLines = 1)
        }
    }
}

/** The hairline along the rows, then a dot on it in the middle of each row. Still: nothing here animates. */
private fun DrawScope.drawDayLine(entries: List<BcDayEntry>, accent: Color) {
    val x = LINE_X.toPx()
    val inset = LINE_END.toPx()
    drawLine(
        color = BcColors.stroke,
        start = Offset(x, inset),
        end = Offset(x, size.height - inset),
        strokeWidth = LINE_WIDTH.toPx()
    )
    val row = ROW_HEIGHT.toPx()
    val radius = DOT_RADIUS.toPx()
    entries.forEachIndexed { index, entry ->
        val center = Offset(x, row * index + row / 2)
        when (entry.mark) {
            BcDayMark.AllDay -> {
                val ring = HOLLOW_WIDTH.toPx()
                drawCircle(HOLLOW, radius - ring / 2, center, style = Stroke(ring))
            }

            BcDayMark.Past -> drawAppointment(center, radius, accent, next = false, accent = accent)

            BcDayMark.Next, BcDayMark.Coming ->
                drawAppointment(center, radius, FUTURE_DOT, next = entry.mark == BcDayMark.Next, accent = accent)
        }
    }
}

private val ROW_HEIGHT = 48.dp
private val ROW_GAP = 14.dp
private val TIME_WIDTH = 54.dp

/** The room the line takes on the left of the rows, and where it runs within it. */
private val LINE_ROOM = 28.dp
private val LINE_X = 7.5.dp
private val LINE_END = 8.dp
private val LINE_WIDTH = 1.dp
private val DOT_RADIUS = 3.5.dp
private val HOLLOW_WIDTH = 1.dp
private val HOLLOW = BcColors.text.copy(alpha = 0.5f)
private const val PAST_ALPHA = 0.6f
