package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing

/**
 * The header of a sheet: its [title] large and light (24 sp), the [subtitle] under it in grey (13 sp), and on the
 * right, standing on the same line as the subtitle, a bordered button named [action] (« Beeper », « Agenda »).
 * In a sheet that only its header may pull ([BcSheetGrip.Header]), sliding the header down pulls the sheet.
 */
@Composable
fun BcSheetHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    val grip = LocalSheetGrip.current
    Row(
        modifier.fillMaxWidth().then(grip?.let { Modifier.gripsSheet(it) } ?: Modifier),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(BcSpacing.m)
    ) {
        Column(Modifier.weight(1f)) {
            BcText(title, Modifier.semantics { heading() }, style = BcTextStyle.SheetTitle, maxLines = 1)
            if (subtitle != null) {
                BcText(subtitle, style = BcTextStyle.Caption, color = BcTextColor.Muted, maxLines = 1)
            }
        }
        if (action != null) BcButton(action, onClick = onAction)
    }
}
