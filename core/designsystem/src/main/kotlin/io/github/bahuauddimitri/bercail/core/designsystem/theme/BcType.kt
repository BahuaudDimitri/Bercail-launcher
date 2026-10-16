package io.github.bahuauddimitri.bercail.core.designsystem.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.bahuauddimitri.bercail.core.designsystem.R

private val outfitWeights = listOf(200, 300, 400, 500, 600, 700)

/** Outfit (SIL Open Font License), one variable font file for every weight from 200 to 700. */
val Outfit = FontFamily(
    outfitWeights.map { weight ->
        Font(
            resId = R.font.outfit,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight))
        )
    }
)

/** Named text styles, from the prototype's scale. Nothing smaller than 10.5 sp. */
object BcType {
    /** Title of the next appointment. */
    val agendaTitle = style(size = 34.sp, weight = FontWeight.ExtraLight, letterSpacing = (-0.025).em)

    /** Track title on the Listen screen. */
    val display = style(size = 22.sp, weight = FontWeight.Normal, lineHeight = 26.sp)

    /** Sheet header: the person, the day. */
    val headline = style(size = 20.sp, weight = FontWeight.Medium)

    /** The title of a sheet in its header: the person, « Aujourd'hui », « Écouter ». */
    val sheetTitle = style(size = 24.sp, weight = FontWeight.Light, lineHeight = 26.sp, letterSpacing = (-0.01).em)

    /** Weather line and other light headers. */
    val titleLight = style(size = 19.sp, weight = FontWeight.Light)

    /** The title of a list row: an app, a person, an appointment. */
    val listTitle = style(size = 17.sp, weight = FontWeight.Normal)

    val title = style(size = 16.sp, weight = FontWeight.Medium)
    val body = style(size = 15.sp, weight = FontWeight.Normal)
    val bodyStrong = style(size = 15.sp, weight = FontWeight.SemiBold)

    /** Message bubbles. */
    val message = style(size = 14.sp, weight = FontWeight.Normal, lineHeight = 19.sp)

    val bodySmall = style(size = 13.5.sp, weight = FontWeight.Normal)
    val caption = style(size = 13.sp, weight = FontWeight.Normal)

    /** The label of a button. */
    val button = style(size = 13.5.sp, weight = FontWeight.Medium)
    val label = style(size = 12.5.sp, weight = FontWeight.Medium)
    val labelStrong = style(size = 12.sp, weight = FontWeight.SemiBold)
    val labelSmall = style(size = 11.sp, weight = FontWeight.Medium)

    /** Under the title of a list row. */
    val listSubtitle = style(size = 12.sp, weight = FontWeight.Normal)

    /** The small title above a group of rows, written in capitals. */
    val sectionLabel = style(size = 11.sp, weight = FontWeight.SemiBold, letterSpacing = 0.1.em)

    /** Initials on a person tile. */
    val initials = style(size = 17.sp, weight = FontWeight.SemiBold)

    /** The initial in the ring of a list row. */
    val listInitial = style(size = 13.5.sp, weight = FontWeight.SemiBold)

    /** Unread count. The smallest text allowed. */
    val badge = style(size = 10.5.sp, weight = FontWeight.Bold)

    /** State under a home control. The smallest text allowed. */
    val micro = style(size = 10.5.sp, weight = FontWeight.Normal)

    val all: Map<String, TextStyle> = mapOf(
        "agendaTitle" to agendaTitle,
        "display" to display,
        "headline" to headline,
        "sheetTitle" to sheetTitle,
        "titleLight" to titleLight,
        "listTitle" to listTitle,
        "title" to title,
        "body" to body,
        "bodyStrong" to bodyStrong,
        "message" to message,
        "bodySmall" to bodySmall,
        "caption" to caption,
        "button" to button,
        "label" to label,
        "labelStrong" to labelStrong,
        "labelSmall" to labelSmall,
        "listSubtitle" to listSubtitle,
        "sectionLabel" to sectionLabel,
        "initials" to initials,
        "listInitial" to listInitial,
        "badge" to badge,
        "micro" to micro
    )

    private fun style(
        size: TextUnit,
        weight: FontWeight,
        lineHeight: TextUnit = TextUnit.Unspecified,
        letterSpacing: TextUnit = TextUnit.Unspecified
    ) = TextStyle(
        fontFamily = Outfit,
        fontSize = size,
        fontWeight = weight,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing
    )
}
