package io.github.bahuauddimitri.bercail.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/** The single, dark palette of Bercail, taken from the prototype (v14). There is no light theme. */
object BcColors {
    /** Background under Brume. */
    val base = Color(0xFF15161F)

    /** Night gradient behind everything, from top to bottom. */
    val nightTop = Color(0xFF101119)
    val nightBottom = Color(0xFF1B1C2A)

    val text = Color(0xFFECEBF4)
    val textMuted = Color(0xFFB2B0C4)

    /** Bluish black glass of the drawer and sheets, more opaque as it covers more. */
    private val glassTint = Color(0xFF181924)
    val glass = glassTint.copy(alpha = 0.34f)
    val glassOpen = glassTint.copy(alpha = 0.5f)
    val glassSheet = glassTint.copy(alpha = 0.62f)
    val glassSearch = glassTint.copy(alpha = 0.8f)

    /** Light edge on top of glass surfaces. */
    val glassEdge = Color.White.copy(alpha = 0.1f)

    /** Translucent tile: switched-off buttons, chips. */
    val tile = Color.White.copy(alpha = 0.075f)
    val tileEdge = Color.White.copy(alpha = 0.1f)

    /** Sunken field: search bar, segmented control. */
    val inset = Color(0xFF0A0A10).copy(alpha = 0.42f)

    /** One pastel per person and per home control. */
    val pastelCoral = Color(0xFFF2A49A)
    val pastelBlue = Color(0xFF9CC4F0)
    val pastelMint = Color(0xFF9FD8C4)
    val pastelLavender = Color(0xFFB4A7F0)
    val pastelApricot = Color(0xFFF6C28B)
    val pastels = listOf(pastelCoral, pastelBlue, pastelMint, pastelLavender, pastelApricot)

    /** Text and icons drawn on a pastel. */
    val onPastel = Color(0xFF2A2530)

    /** Text and icons drawn on the light primary surface (my bubbles, the send button). */
    val onLight = Color(0xFF1B1B24)

    /** Placeholder tile behind an app or result without its own icon. */
    val appTile = Color(0xFFD9D4E6)

    /** A home control that is switched off. */
    val off = Color(0xFF4A4A5C)

    internal val backgrounds = listOf(base, nightTop, nightBottom)
}

/** WCAG contrast ratio between two colors, from 1 (none) to 21 (black on white). */
internal fun contrastRatio(first: Color, second: Color): Float {
    val a = first.luminance()
    val b = second.luminance()
    return (max(a, b) + LUMINANCE_OFFSET) / (min(a, b) + LUMINANCE_OFFSET)
}

private const val LUMINANCE_OFFSET = 0.05f
