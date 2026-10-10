package io.github.bahuauddimitri.bercail

import io.github.bahuauddimitri.bercail.core.domain.Unconnected
import io.github.bahuauddimitri.bercail.core.domain.settings.SettingsSource

/** In the released app, a source that is not plugged in yet shows its empty state, never made-up data. */
internal fun waiting() = Waiting(
    agenda = Unconnected.agenda,
    weather = Unconnected.weather,
    messages = Unconnected.messages,
    home = Unconnected.home,
    media = Unconnected.media
)

/** The released app uses the stored settings as they are. */
internal fun SettingsSource.forThisBuild(): SettingsSource = this
