package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import java.util.Locale

/** The top of a full-screen list: a back arrow, announced by [backDescription], and a quiet title. */
@Composable
fun BcPanelHeader(title: String, onBack: () -> Unit, backDescription: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BcSpacing.s)
    ) {
        BcIconButton(BcIcons.Back, backDescription, onBack, variant = BcIconButtonVariant.Ghost)
        BcText(title, style = BcTextStyle.Caption, color = BcTextColor.Muted, maxLines = 1)
    }
}

/**
 * A full-screen panel over everything (the settings): header with a back arrow, then a scrolling column. It is
 * opaque, so that nothing shows through, and it takes the keyboard down when it opens.
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
    // The panel covers everything: a field left focused underneath gives the keyboard back.
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    LaunchedEffect(Unit) {
        focus.clearFocus(force = true)
        keyboard?.hide()
    }
    Column(
        modifier
            .fillMaxSize()
            .background(BcColors.base)
            .pointerInput(Unit) { detectTapGestures {} }
            // Bars first, then scrolling: content never slides under the status bar.
            .safeDrawingPadding()
            .padding(horizontal = PANEL_SIDE)
    ) {
        BcPanelHeader(title, onBack, backDescription)
        // Rows lie flat one under the other, each with its hairline; section titles keep their own room.
        CompositionLocalProvider(LocalInPanel provides true) {
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(bottom = BcSpacing.xl),
                content = content
            )
        }
    }
}

/**
 * The small title above a group of rows: "Dock", "A", "Agenda". Drawn in spaced capitals; a screen reader reads it
 * as it is written, not letter by letter. Inside a panel, where rows lie flat, it keeps 18 dp above and 6 dp below.
 */
@Composable
fun BcSectionLabel(text: String, modifier: Modifier = Modifier) {
    val inPanel = LocalInPanel.current
    BcText(
        text.inCapitals(),
        modifier = modifier
            .padding(
                top = if (inPanel) PANEL_LABEL_TOP else LABEL_TOP,
                bottom = if (inPanel) PANEL_LABEL_BOTTOM else BcSpacing.xxs
            )
            .clearAndSetSemantics { this.text = AnnotatedString(text) },
        style = BcTextStyle.SectionLabel,
        color = BcTextColor.Muted,
        maxLines = LABEL_LINES
    )
}

/** A section title as it is drawn: in capitals, accents kept. */
internal fun String.inCapitals(): String = uppercase(Locale.FRENCH)

/**
 * One setting, flat, with a hairline under it: title, explanation, and on the right its [trailing] control (a
 * switch, a button, a word saying where it stands). A long choice goes in [control], underneath. With [onClick], the
 * whole row is a link, at least 40 dp high.
 */
@Composable
fun BcSettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    control: (@Composable () -> Unit)? = null
) {
    val link = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Column(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = BcSizes.minTouch)
            .then(link)
            .drawBehind {
                val y = size.height - HAIRLINE.toPx() / 2
                drawLine(BcColors.hairline, Offset(0f, y), Offset(size.width, y), HAIRLINE.toPx())
            }
            .padding(vertical = ROW_VERTICAL),
        verticalArrangement = Arrangement.spacedBy(CONTROL_GAP)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TRAILING_GAP)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TEXT_GAP)) {
                BcText(title, style = BcTextStyle.Title, maxLines = 1)
                if (subtitle !=
                    null
                ) {
                    BcText(subtitle, style = BcTextStyle.Label, color = BcTextColor.Muted, maxLines = 2)
                }
            }
            trailing?.invoke()
        }
        control?.invoke()
    }
}

/** Whether a section title stands among the flat rows of a panel. */
private val LocalInPanel = staticCompositionLocalOf { false }

private val PANEL_SIDE = 20.dp
private val LABEL_TOP = 14.dp
private val PANEL_LABEL_TOP = 18.dp
private val PANEL_LABEL_BOTTOM = 6.dp

// A long hint (the settings explain their lists this way) goes on to a second line rather than being cut.
private const val LABEL_LINES = 2
private val ROW_VERTICAL = 12.dp
private val HAIRLINE = 1.dp
private val CONTROL_GAP = 10.dp
private val TRAILING_GAP = 12.dp
private val TEXT_GAP = 1.dp
