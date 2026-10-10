package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing

/** The top of a full-screen list: a back arrow, announced by [backDescription], and a quiet title. */
@Composable
fun BcPanelHeader(title: String, onBack: () -> Unit, backDescription: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BcSpacing.s)
    ) {
        BcIconButton(BcIcons.Back, backDescription, onBack, variant = BcButtonVariant.Ghost)
        BcText(title, style = BcTextStyle.Caption, color = BcTextColor.Muted, maxLines = 1)
    }
}

/**
 * A full-screen glass panel over everything (the settings): header with a back arrow, then a scrolling column.
 * The system back gesture closes it; touches do not reach what is underneath.
 */
@Composable
fun BcPanel(
    title: String,
    onBack: () -> Unit,
    backDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    BackHandler(onBack = onBack)
    Column(
        modifier
            .fillMaxSize()
            .background(BcColors.glassSearch)
            .pointerInput(Unit) { detectTapGestures {} }
            // Bars first, then scrolling: content never slides under the status bar.
            .safeDrawingPadding()
            .padding(horizontal = PANEL_SIDE)
    ) {
        BcPanelHeader(title, onBack, backDescription)
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(bottom = BcSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(BcSpacing.s),
            content = content
        )
    }
}

/** The small title above a group of rows: "Apps favorites", "A", "Agenda". */
@Composable
fun BcSectionLabel(text: String, modifier: Modifier = Modifier) {
    BcText(
        text,
        modifier = modifier.padding(top = LABEL_TOP, bottom = BcSpacing.xxs),
        style = BcTextStyle.LabelStrong,
        color = BcTextColor.Muted,
        maxLines = 1
    )
}

/**
 * One setting on its tile: title, explanation, and its [control] underneath (a segmented choice, a button).
 * With [onClick], the whole tile is a link, at least 40 dp high.
 */
@Composable
fun BcSettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    control: (@Composable () -> Unit)? = null
) {
    val link = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Column(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = BcSizes.minTouch)
            .clip(ROW_SHAPE)
            .background(BcColors.tile)
            .border(1.dp, BcColors.tileEdge, ROW_SHAPE)
            .then(link)
            .padding(horizontal = ROW_SIDE, vertical = ROW_VERTICAL),
        verticalArrangement = Arrangement.spacedBy(ROW_GAP)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TEXT_GAP)) {
            BcText(title, style = BcTextStyle.Message, maxLines = 1)
            if (subtitle != null) BcText(subtitle, style = BcTextStyle.Caption, color = BcTextColor.Muted, maxLines = 2)
        }
        control?.invoke()
    }
}

private val PANEL_SIDE = 20.dp
private val LABEL_TOP = 14.dp
private val ROW_SHAPE = RoundedCornerShape(18.dp)
private val ROW_SIDE = 14.dp
private val ROW_VERTICAL = 13.dp
private val ROW_GAP = 10.dp
private val TEXT_GAP = 2.dp
