package io.github.bahuauddimitri.bercail.feature.home

import io.github.bahuauddimitri.bercail.core.testing.FakeWorld

/** Screen of the reference phone: 1080 × 2424 px at 420 dpi. */
const val PIXEL_9 = "w411dp-h923dp-420dpi"

/** The home screen fed by every fake source of this world. */
fun FakeWorld.homeViewModel() = HomeViewModel(clock, agenda, weather, messages, home, media, settings)
