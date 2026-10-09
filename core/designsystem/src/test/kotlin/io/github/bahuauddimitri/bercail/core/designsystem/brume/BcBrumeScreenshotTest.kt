package io.github.bahuauddimitri.bercail.core.designsystem.brume

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.bahuauddimitri.bercail.core.designsystem.PIXEL_9
import io.github.bahuauddimitri.bercail.core.designsystem.captureComponent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class BcBrumeScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun capture(
        name: String,
        colors: BcBrumeColors,
        weather: BcWeather = BcWeather.Clear,
        night: Boolean = false
    ) = compose.captureComponent(name) {
        BcBrume(
            colors = colors,
            playing = false,
            weather = weather,
            night = night,
            modifier = Modifier.size(260.dp, 520.dp)
        )
    }

    @Test
    fun `Brume du matin correspond à sa capture`() = capture("brume_morning", BcBrumeColors.Morning)

    @Test
    fun `Brume du soir avec ses étoiles correspond à sa capture`() =
        capture("brume_evening_night", BcBrumeColors.Evening, night = true)

    @Test
    fun `Brume sous la pluie correspond à sa capture`() = capture("brume_rain", BcBrumeColors.Day, BcWeather.Rain)

    @Test
    fun `Brume dans le vent correspond à sa capture`() = capture("brume_wind", BcBrumeColors.Day, BcWeather.Wind)

    @Test
    fun `Brume sous la neige correspond à sa capture`() = capture("brume_snow", BcBrumeColors.Morning, BcWeather.Snow)
}
