package io.github.bahuauddimitri.bercail

import android.app.Application
import android.content.Context
import android.util.Log
import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaSource
import io.github.bahuauddimitri.bercail.core.domain.apps.AppsSource
import io.github.bahuauddimitri.bercail.core.domain.apps.Phone
import io.github.bahuauddimitri.bercail.core.domain.home.HomeSource
import io.github.bahuauddimitri.bercail.core.domain.media.MediaAppsSource
import io.github.bahuauddimitri.bercail.core.domain.media.MediaSource
import io.github.bahuauddimitri.bercail.core.domain.messages.MessagesSource
import io.github.bahuauddimitri.bercail.core.domain.settings.SettingsSource
import io.github.bahuauddimitri.bercail.core.domain.time.Clock
import io.github.bahuauddimitri.bercail.core.domain.update.Updater
import io.github.bahuauddimitri.bercail.core.domain.weather.WeatherSource
import io.github.bahuauddimitri.bercail.core.domain.weather.WeatherWatch
import io.github.bahuauddimitri.bercail.data.agenda.DeviceAgenda
import io.github.bahuauddimitri.bercail.data.apps.DeviceApps
import io.github.bahuauddimitri.bercail.data.apps.DevicePhone
import io.github.bahuauddimitri.bercail.data.media.DeviceMedia
import io.github.bahuauddimitri.bercail.data.media.DeviceMediaApps
import io.github.bahuauddimitri.bercail.data.media.listeningStore
import io.github.bahuauddimitri.bercail.data.messages.BeeperMessages
import io.github.bahuauddimitri.bercail.data.settings.StoredSettings
import io.github.bahuauddimitri.bercail.data.settings.settingsStore
import io.github.bahuauddimitri.bercail.data.weather.OpenMeteo
import io.github.bahuauddimitri.bercail.data.weather.OpenMeteoCities
import io.github.bahuauddimitri.bercail.data.weather.PhonePosition
import io.github.bahuauddimitri.bercail.data.weather.StoredForecast
import io.github.bahuauddimitri.bercail.data.weather.forecastStore
import io.github.bahuauddimitri.bercail.feature.home.HomeViewModel
import io.github.bahuauddimitri.bercail.feature.home.ListenSources
import io.github.bahuauddimitri.bercail.feature.search.SearchViewModel
import io.github.bahuauddimitri.bercail.feature.settings.SettingsViewModel
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Holds the sources for the whole life of the app: the settings file must only be opened once. They are plugged at
 * first use; the tests may plug their own before the home screen starts.
 */
class BercailApplication : Application() {
    private var plugged: Sources? = null

    var sources: Sources
        get() = plugged ?: Sources(this).also { plugged = it }
        set(value) {
            plugged = value
        }
}

/** The source whose wave has not come yet. What stands in for it depends on the build: see [waiting]. */
internal class Waiting(val home: HomeSource)

/**
 * Every source of Bercail, plugged once. Real today: the clock, the installed apps, the links to the phone, the
 * stored settings, the update of Bercail itself, what the user lets it read, the calendar, the weather, what plays
 * in the media apps and the conversations Beeper keeps. The house waits for its wave (9). The tests plug their
 * own parts.
 */
class Sources(
    context: Context,
    update: UpdateParts? = null,
    agenda: AgendaSource? = null,
    clock: Clock? = null,
    weather: WeatherSource? = null,
    media: MediaSource? = null,
    messages: MessagesSource? = null
) {
    private val background = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val waiting = waiting()

    val clock: Clock = clock ?: DeviceClock(context)
    val apps: AppsSource = DeviceApps(context)
    val phone: Phone = DevicePhone(context)
    val settings: SettingsSource =
        StoredSettings(settingsStore(File(context.filesDir, SETTINGS_FILE), background))

    /** What the user has let Bercail read so far. */
    val access = DeviceAccess(context)
    val agenda: AgendaSource = agenda ?: DeviceAgenda(context, access)

    /** The forecast of the place where the phone is, asked for only while the home screen is shown. */
    val weather: WeatherSource = weather ?: WeatherWatch(
        clock = this.clock,
        access = access,
        settings = settings,
        position = PhonePosition(context),
        service = OpenMeteo(onFailure = { Log.w(LOG_TAG, "The forecast could not be read", it) }),
        journal = StoredForecast(forecastStore(File(context.filesDir, WEATHER_FILE), background)),
        background = Dispatchers.IO,
        onFailure = { Log.w(LOG_TAG, "The weather of Bercail stumbled", it) }
    )

    /** What plays in the media apps, followed only while the home screen is shown and once the user has agreed. */
    val media: MediaSource = media ?: DeviceMedia(context, access)

    /** The apps that can play, looked for only while the « Écouter » sheet or the settings show them. */
    val mediaApps: MediaAppsSource =
        DeviceMediaApps(context, apps, listeningStore(File(context.filesDir, LISTENING_FILE), background))

    /** The conversations of every network, read in Beeper only while the home screen or the search shows them. */
    val messages: MessagesSource = messages ?: BeeperMessages(context, access)

    private val updateParts = update ?: UpdateParts.onPhone(context, background)

    /** Bercail updating itself as the home screen comes and goes. */
    val updater: Updater = updateParts.let {
        Updater(it.version, it.feed, it.packages, it.journal, this.clock, it.background, ::logged)
    }

    fun homeViewModel() = HomeViewModel(
        clock = clock,
        agenda = agenda,
        weather = weather,
        messages = messages,
        home = waiting.home,
        listen = ListenSources(media, mediaApps, apps),
        settings = settings,
        access = access
    )

    fun searchViewModel() = SearchViewModel(apps, phone, clock, agenda, messages, waiting.home, settings, updater)

    fun settingsViewModel() =
        SettingsViewModel(settings, apps, phone, updater, access, OpenMeteoCities(), mediaApps, messages)

    /** The home screen just went away: a version that is ready may take over. */
    fun onHomeHidden() {
        background.launch(updateParts.background) { updater.onHomeHidden() }
    }

    /** What broke unexpectedly in the update only leaves a trace: the home screen goes on. */
    private fun logged(failure: Exception) {
        Log.w(LOG_TAG, "The update of Bercail stumbled", failure)
    }

    private companion object {
        const val SETTINGS_FILE = "datastore/settings.preferences_pb"
        const val WEATHER_FILE = "datastore/weather.preferences_pb"
        const val LISTENING_FILE = "datastore/listening.preferences_pb"
        const val LOG_TAG = "Bercail"
    }
}
