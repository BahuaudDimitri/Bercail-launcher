package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcWeather
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme

/**
 * The weather in one line at the top of the home screen, opened by the glyph of its sky (the moon on a clear
 * [night]) in the accent of the moment.
 */
@Composable
fun BcWeatherLine(text: String, weather: BcWeather, modifier: Modifier = Modifier, night: Boolean = false) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BcSpacing.xs)
    ) {
        Image(
            painter = rememberVectorPainter(weather.glyph(night).vector),
            contentDescription = null,
            modifier = Modifier.size(GLYPH),
            colorFilter = ColorFilter.tint(BcTheme.moment.accent)
        )
        BcText(text, style = BcTextStyle.Label, maxLines = 1, modifier = Modifier.alpha(TEXT_ALPHA))
    }
}

/** The glyph of a sky: the sun, or the moon on a clear night, the rain, the wind, the snow. */
internal fun BcWeather.glyph(night: Boolean): BcIcons = when (this) {
    BcWeather.Clear -> if (night) BcIcons.Moon else BcIcons.Sun
    BcWeather.Rain -> BcIcons.Rain
    BcWeather.Wind -> BcIcons.Wind
    BcWeather.Snow -> BcIcons.Snow
}

private const val TEXT_ALPHA = 0.85f
private val GLYPH = 15.dp
