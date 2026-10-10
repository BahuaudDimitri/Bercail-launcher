package io.github.bahuauddimitri.bercail

import android.content.Context
import io.github.bahuauddimitri.bercail.core.testing.FakeAgendaSource
import io.github.bahuauddimitri.bercail.core.testing.FakeMediaSource
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Samples
import io.github.bahuauddimitri.bercail.feature.home.HomeViewModel

/**
 * Plugs the home screen into its sources. Only the clock is real for now: each other source is the prototype's
 * fake until its own wave replaces it (apps and settings in wave 4, agenda and weather in 6, media in 7,
 * messages in 8, house in 9).
 */
internal fun homeViewModel(context: Context): HomeViewModel {
    val prototype = FakeWorld()
    return HomeViewModel(
        clock = DeviceClock(context.applicationContext),
        // The prototype's day, replayed every day.
        agenda = FakeAgendaSource(Samples.eventsOn(Samples.day), everyDay = true),
        weather = prototype.weather,
        messages = prototype.messages,
        home = prototype.home,
        // A player whose time moves, so that the remote can be tried.
        media = FakeMediaSource(Samples.spotify, playing = true, position = Samples.position, ticking = true),
        settings = prototype.settings
    )
}
