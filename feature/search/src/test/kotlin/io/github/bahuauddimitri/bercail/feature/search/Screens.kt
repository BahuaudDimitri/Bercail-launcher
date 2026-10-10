package io.github.bahuauddimitri.bercail.feature.search

/** Screen of the reference phone: 1080 × 2424 px at 420 dpi. */
const val PIXEL_9 = "w411dp-h923dp-420dpi"

/** The search fed by every fake source of this world. */
fun io.github.bahuauddimitri.bercail.core.testing.FakeWorld.searchViewModel() =
    SearchViewModel(apps, phone, clock, agenda, messages, home, settings)
