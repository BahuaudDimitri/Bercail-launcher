package io.github.bahuauddimitri.bercail.feature.home

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcBrumeColors
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcWeather
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIcons
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcMoment
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.domain.Picture
import io.github.bahuauddimitri.bercail.core.domain.home.HomeControl
import io.github.bahuauddimitri.bercail.core.domain.media.CoverColors
import io.github.bahuauddimitri.bercail.core.domain.screen.HomePage
import io.github.bahuauddimitri.bercail.core.domain.time.DayPart
import io.github.bahuauddimitri.bercail.core.domain.weather.WeatherCondition

// How the things of the domain look: which icon, which pastel, which colors of Brume.

internal val WeatherCondition.look: BcWeather
    get() = when (this) {
        WeatherCondition.Clear -> BcWeather.Clear
        WeatherCondition.Rain -> BcWeather.Rain
        WeatherCondition.Wind -> BcWeather.Wind
        WeatherCondition.Snow -> BcWeather.Snow
    }

internal val HomeControl.icon: BcIcons
    get() = when (this) {
        is HomeControl.Light -> BcIcons.Lamp
        is HomeControl.Scene -> BcIcons.Film
        is HomeControl.Shutters -> BcIcons.Shutters
        is HomeControl.Heating -> BcIcons.Flame
    }

internal val HomeControl.pastel: BcPastel
    get() = when (this) {
        is HomeControl.Light -> BcPastel.Apricot
        is HomeControl.Scene -> BcPastel.Lavender
        is HomeControl.Shutters -> BcPastel.Blue
        is HomeControl.Heating -> BcPastel.Coral
    }

/**
 * The pastel of a person: favorites take the pastels in their order, so two favorites never share one;
 * anyone else always gets the same one.
 */
internal fun pastelOf(personId: String, favoriteIds: List<String>): BcPastel = BcPastel.forKey(personId, favoriteIds)

internal val CoverColors.pair: Pair<Color, Color> get() = Color(first) to Color(second)

internal val CoverColors.brume: BcBrumeColors get() = BcBrumeColors.of(Color(first), Color(second))

/**
 * The cover as a picture ready to draw, made once for each cover. It also carries smaller versions of itself
 * (mipmaps): a cover kept large for the arch of Écoute stays clean when drawn small, in the music pill.
 */
@Composable
internal fun Picture?.asImage(): ImageBitmap? = remember(this) {
    this?.let {
        Bitmap.createBitmap(it.pixels, it.width, it.height, Bitmap.Config.ARGB_8888)
            .apply { setHasMipMap(true) }
            .asImageBitmap()
    }
}

/** Brume without a cover takes the colors of the moment: peach in the morning, blue-green by day, lavender later. */
internal val DayPart.brume: BcBrumeColors
    get() = when (this) {
        DayPart.Morning -> BcBrumeColors.Morning
        DayPart.Afternoon -> BcBrumeColors.Day
        DayPart.Evening, DayPart.Night -> BcBrumeColors.Evening
    }

/** The accent of the moment follows the colors of Brume: peach in the morning, blue-green by day, lavender later. */
internal val DayPart.moment: BcMoment
    get() = when (this) {
        DayPart.Morning -> BcMoment.Morning
        DayPart.Afternoon -> BcMoment.Day
        DayPart.Evening, DayPart.Night -> BcMoment.Evening
    }

/** A sheet covers the home: the people and the house over the band, the day, the media apps or a conversation. */
internal val HomeUiState.underSheet: Boolean
    get() = (drawerExpanded && page == HomePage.Home) || dayOpen || listenOpen || talk != null

/**
 * Brume moves only while music plays and no sheet covers the home, the search included: an open sheet freezes it,
 * so that its glass is frosted at once and nothing is redrawn behind it.
 */
internal fun HomeUiState.brumeMoves(searching: Boolean): Boolean = media.isPlaying && !underSheet && !searching
