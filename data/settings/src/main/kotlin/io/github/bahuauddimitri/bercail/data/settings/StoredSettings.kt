package io.github.bahuauddimitri.bercail.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.bahuauddimitri.bercail.core.domain.apps.MAX_DOCK_APPS
import io.github.bahuauddimitri.bercail.core.domain.settings.IdleBackground
import io.github.bahuauddimitri.bercail.core.domain.settings.Settings
import io.github.bahuauddimitri.bercail.core.domain.settings.SettingsSource
import io.github.bahuauddimitri.bercail.core.domain.weather.City
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Opens the settings file. One store per file and per process; [scope] lives as long as the app. */
fun settingsStore(file: File, scope: CoroutineScope): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(scope = scope) { file }

/**
 * Bercail's settings, kept in a file on the phone. Nothing is read before someone listens, and the file is only
 * written when a setting changes. Defaults live in [Settings]: a missing or unknown value falls back to them.
 */
class StoredSettings(private val store: DataStore<Preferences>) : SettingsSource {
    override val settings: Flow<Settings> = store.data.map { it.toSettings() }.distinctUntilChanged()

    override suspend fun update(transform: (Settings) -> Settings) {
        store.edit { saved ->
            val wanted = transform(saved.toSettings())
            saved[FAVORITE_PEOPLE] = wanted.favoritePeople.joinToString(SEPARATOR)
            saved[FAVORITE_APPS] = wanted.dockApps.joinToString(SEPARATOR)
            // The line of the day could lie down in older versions; it no longer can.
            saved.remove(TIMELINE)
            saved[DRAWER_EXPANDED] = wanted.drawerExpandedAtStart
            saved[IDLE_BACKGROUND] = wanted.idleBackground.name
            saved.keep(wanted.weatherCity)
            wanted.mainMediaApp?.let { saved[MAIN_MEDIA_APP] = it } ?: saved.remove(MAIN_MEDIA_APP)
            wanted.iconPack?.let { saved[ICON_PACK] = it } ?: saved.remove(ICON_PACK)
        }
    }

    private fun Preferences.toSettings(): Settings {
        val defaults = Settings()
        return Settings(
            favoritePeople = this[FAVORITE_PEOPLE]?.toList() ?: defaults.favoritePeople,
            // The favorite apps of the versions before the dock: the five first make it.
            dockApps = this[FAVORITE_APPS]?.toList()?.take(MAX_DOCK_APPS) ?: defaults.dockApps,
            drawerExpandedAtStart = this[DRAWER_EXPANDED] ?: defaults.drawerExpandedAtStart,
            idleBackground = IdleBackground.entries.firstOrNull { it.name == this[IDLE_BACKGROUND] }
                ?: defaults.idleBackground,
            weatherCity = city(),
            mainMediaApp = this[MAIN_MEDIA_APP],
            iconPack = this[ICON_PACK]
        )
    }

    private fun String.toList(): List<String> = split(SEPARATOR).filter { it.isNotEmpty() }

    /** The chosen city, complete or absent: half a city is no city. */
    private fun Preferences.city(): City? {
        val name = this[CITY_NAME]
        val latitude = this[CITY_LATITUDE]
        val longitude = this[CITY_LONGITUDE]
        return if (name != null && latitude != null && longitude != null) {
            City(name, this[CITY_REGION].orEmpty(), latitude, longitude)
        } else {
            null
        }
    }

    private fun MutablePreferences.keep(city: City?) {
        listOf(CITY_NAME, CITY_REGION, CITY_LATITUDE, CITY_LONGITUDE).forEach { remove(it) }
        if (city == null) return
        this[CITY_NAME] = city.name
        this[CITY_REGION] = city.region
        this[CITY_LATITUDE] = city.latitude
        this[CITY_LONGITUDE] = city.longitude
    }

    private companion object {
        val FAVORITE_PEOPLE = stringPreferencesKey("favorite_people")
        val FAVORITE_APPS = stringPreferencesKey("favorite_apps")
        val TIMELINE = stringPreferencesKey("timeline")
        val DRAWER_EXPANDED = booleanPreferencesKey("drawer_expanded_at_start")
        val IDLE_BACKGROUND = stringPreferencesKey("idle_background")
        val CITY_NAME = stringPreferencesKey("weather_city_name")
        val CITY_REGION = stringPreferencesKey("weather_city_region")
        val CITY_LATITUDE = doublePreferencesKey("weather_city_latitude")
        val CITY_LONGITUDE = doublePreferencesKey("weather_city_longitude")
        val MAIN_MEDIA_APP = stringPreferencesKey("main_media_app")
        val ICON_PACK = stringPreferencesKey("icon_pack")

        /** Kept as one ordered line per id: a set would lose the order of the favorites. */
        const val SEPARATOR = "\n"
    }
}
