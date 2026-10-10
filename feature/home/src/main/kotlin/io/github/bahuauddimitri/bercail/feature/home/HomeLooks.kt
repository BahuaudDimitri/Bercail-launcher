package io.github.bahuauddimitri.bercail.feature.home

import androidx.compose.ui.graphics.Color
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcBrumeColors
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcWeather
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIcons
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.domain.home.HomeControl
import io.github.bahuauddimitri.bercail.core.domain.media.CoverColors
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
internal fun pastelOf(personId: String, favoriteIds: List<String>): BcPastel {
    val rank = favoriteIds.indexOf(personId)
    return if (rank >= 0) BcPastel.entries[rank % BcPastel.entries.size] else BcPastel.forKey(personId)
}

internal val CoverColors.pair: Pair<Color, Color> get() = Color(first) to Color(second)

internal val CoverColors.brume: BcBrumeColors get() = BcBrumeColors.of(Color(first), Color(second))

/** Brume without a cover takes the colors of the moment: peach in the morning, blue-green by day, lavender later. */
internal val DayPart.brume: BcBrumeColors
    get() = when (this) {
        DayPart.Morning -> BcBrumeColors.Morning
        DayPart.Afternoon -> BcBrumeColors.Day
        DayPart.Evening, DayPart.Night -> BcBrumeColors.Evening
    }
