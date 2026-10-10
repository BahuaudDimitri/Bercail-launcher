package io.github.bahuauddimitri.bercail

import android.app.Application
import android.content.Context
import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaSource
import io.github.bahuauddimitri.bercail.core.domain.apps.AppsSource
import io.github.bahuauddimitri.bercail.core.domain.apps.Phone
import io.github.bahuauddimitri.bercail.core.domain.home.HomeSource
import io.github.bahuauddimitri.bercail.core.domain.media.MediaSource
import io.github.bahuauddimitri.bercail.core.domain.messages.MessagesSource
import io.github.bahuauddimitri.bercail.core.domain.settings.SettingsSource
import io.github.bahuauddimitri.bercail.core.domain.time.Clock
import io.github.bahuauddimitri.bercail.core.domain.weather.WeatherSource
import io.github.bahuauddimitri.bercail.data.apps.DeviceApps
import io.github.bahuauddimitri.bercail.data.apps.DevicePhone
import io.github.bahuauddimitri.bercail.data.settings.StoredSettings
import io.github.bahuauddimitri.bercail.data.settings.settingsStore
import io.github.bahuauddimitri.bercail.feature.home.HomeViewModel
import io.github.bahuauddimitri.bercail.feature.search.SearchViewModel
import io.github.bahuauddimitri.bercail.feature.settings.SettingsViewModel
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Holds the sources for the whole life of the app: the settings file must only be opened once. */
class BercailApplication : Application() {
    val sources: Sources by lazy { Sources(this) }
}

/** The sources whose wave has not come yet. What stands in for them depends on the build: see [waiting]. */
internal class Waiting(
    val agenda: AgendaSource,
    val weather: WeatherSource,
    val messages: MessagesSource,
    val home: HomeSource,
    val media: MediaSource
)

/**
 * Every source of Bercail, plugged once. Real today: the clock, the installed apps, the links to the phone and
 * the stored settings. The others wait for their wave (agenda and weather in 6, media in 7, messages in 8,
 * house in 9).
 */
class Sources(context: Context) {
    private val background = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val waiting = waiting()

    val clock: Clock = DeviceClock(context)
    val apps: AppsSource = DeviceApps(context)
    val phone: Phone = DevicePhone(context)
    val settings: SettingsSource =
        StoredSettings(settingsStore(File(context.filesDir, SETTINGS_FILE), background)).forThisBuild()

    fun homeViewModel() =
        HomeViewModel(clock, waiting.agenda, waiting.weather, waiting.messages, waiting.home, waiting.media, settings)

    fun searchViewModel() =
        SearchViewModel(apps, phone, clock, waiting.agenda, waiting.messages, waiting.home, settings)

    fun settingsViewModel() = SettingsViewModel(settings, apps, phone)

    private companion object {
        const val SETTINGS_FILE = "datastore/settings.preferences_pb"
    }
}
