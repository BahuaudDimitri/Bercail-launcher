package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcType
import io.github.bahuauddimitri.bercail.core.designsystem.theme.LocalBcContentColor

/** The named text styles a screen can use. */
enum class BcTextStyle(internal val style: TextStyle) {
    AgendaTitle(BcType.agendaTitle),
    Display(BcType.display),
    Headline(BcType.headline),
    TitleLight(BcType.titleLight),
    Title(BcType.title),
    Body(BcType.body),
    BodyStrong(BcType.bodyStrong),
    Message(BcType.message),
    BodySmall(BcType.bodySmall),
    Caption(BcType.caption),
    Label(BcType.label),
    LabelStrong(BcType.labelStrong),
    LabelSmall(BcType.labelSmall)
}

/** The text colors a screen can use. */
enum class BcTextColor(internal val color: Color) {
    Primary(BcColors.text),
    Muted(BcColors.textMuted),
    OnPastel(BcColors.onPastel),
    OnLight(BcColors.onLight)
}

/**
 * Text in Outfit. Without [color], it takes the color of the surface it sits on
 * (light on glass, dark on a pastel tile). Long text ends with an ellipsis.
 */
@Composable
fun BcText(
    text: String,
    modifier: Modifier = Modifier,
    style: BcTextStyle = BcTextStyle.Body,
    color: BcTextColor? = null,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null
) {
    val resolved = color?.color ?: LocalBcContentColor.current
    BasicText(
        text = text,
        modifier = modifier,
        style = style.style.copy(color = resolved, textAlign = textAlign ?: TextAlign.Unspecified),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}
