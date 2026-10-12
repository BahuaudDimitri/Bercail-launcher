package io.github.bahuauddimitri.bercail.core.testing

import io.github.bahuauddimitri.bercail.core.domain.apps.App
import io.github.bahuauddimitri.bercail.core.domain.media.LastHeard
import io.github.bahuauddimitri.bercail.core.domain.media.MediaAppsSource
import kotlinx.coroutines.flow.MutableStateFlow

/** The media apps of a phone, set by hand, and what was last heard, remembered as long as the test lasts. */
class FakeMediaAppsSource(apps: List<App> = emptyList(), lastHeard: LastHeard? = null) : MediaAppsSource {
    override val apps = MutableStateFlow(apps)
    override val lastHeard = MutableStateFlow(lastHeard)

    override suspend fun remember(heard: LastHeard) {
        lastHeard.value = heard
    }
}
