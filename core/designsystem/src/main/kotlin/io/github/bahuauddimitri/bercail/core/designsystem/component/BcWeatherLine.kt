package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcWeather
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing

/** The weather in one line at the top of the home screen, opened by a dot in the color of the sky. */
@Composable
fun BcWeatherLine(text: String, weather: BcWeather, modifier: Modifier = Modifier) {
    val dot = when (weather) {
        BcWeather.Clear -> BcColors.pastelApricot
        BcWeather.Rain -> BcColors.pastelBlue
        BcWeather.Wind -> BcColors.pastelMint
        BcWeather.Snow -> BcColors.text
    }
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BcSpacing.xs)
    ) {
        Box(Modifier.size(DOT).background(dot, BcShapes.pill))
        BcText(text, style = BcTextStyle.Label, maxLines = 1, modifier = Modifier.alpha(TEXT_ALPHA))
    }
}

private const val TEXT_ALPHA = 0.85f
private val DOT = 6.dp
