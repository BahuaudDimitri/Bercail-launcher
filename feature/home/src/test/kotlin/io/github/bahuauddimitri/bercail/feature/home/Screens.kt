package io.github.bahuauddimitri.bercail.feature.home

import io.github.bahuauddimitri.bercail.core.domain.Unconnected
import io.github.bahuauddimitri.bercail.core.testing.FakeClock
import io.github.bahuauddimitri.bercail.core.testing.FakeSettingsSource
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Samples

/** Screen of the reference phone: 1080 × 2424 px at 420 dpi. */
const val PIXEL_9 = "w411dp-h923dp-420dpi"

/** The home screen fed by every fake source of this world. */
fun FakeWorld.homeViewModel() = HomeViewModel(clock, agenda, weather, messages, home, media, settings)

/** The home screen as the released app shows it before any source is plugged in: only the time is known. */
fun unconnectedHomeViewModel(hour: Int = 9) = HomeViewModel(
    clock = FakeClock(Samples.day.atTime(hour, 0)),
    agenda = Unconnected.agenda,
    weather = Unconnected.weather,
    messages = Unconnected.messages,
    home = Unconnected.home,
    media = Unconnected.media,
    settings = FakeSettingsSource()
)
