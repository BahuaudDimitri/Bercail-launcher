package io.github.bahuauddimitri.bercail.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.bahuauddimitri.bercail.core.domain.settings.IdleBackground
import io.github.bahuauddimitri.bercail.core.domain.settings.Settings
import io.github.bahuauddimitri.bercail.core.domain.settings.SettingsSource
import io.github.bahuauddimitri.bercail.core.domain.settings.TimelineOrientation
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
            saved[FAVORITE_APPS] = wanted.favoriteApps.joinToString(SEPARATOR)
            saved[TIMELINE] = wanted.timeline.name
            saved[DRAWER_EXPANDED] = wanted.drawerExpandedAtStart
            saved[IDLE_BACKGROUND] = wanted.idleBackground.name
        }
    }

    private fun Preferences.toSettings(): Settings {
        val defaults = Settings()
        return Settings(
            favoritePeople = this[FAVORITE_PEOPLE]?.toList() ?: defaults.favoritePeople,
            favoriteApps = this[FAVORITE_APPS]?.toList() ?: defaults.favoriteApps,
            timeline = TimelineOrientation.entries.firstOrNull { it.name == this[TIMELINE] } ?: defaults.timeline,
            drawerExpandedAtStart = this[DRAWER_EXPANDED] ?: defaults.drawerExpandedAtStart,
            idleBackground = IdleBackground.entries.firstOrNull { it.name == this[IDLE_BACKGROUND] }
                ?: defaults.idleBackground
        )
    }

    private fun String.toList(): List<String> = split(SEPARATOR).filter { it.isNotEmpty() }

    private companion object {
        val FAVORITE_PEOPLE = stringPreferencesKey("favorite_people")
        val FAVORITE_APPS = stringPreferencesKey("favorite_apps")
        val TIMELINE = stringPreferencesKey("timeline")
        val DRAWER_EXPANDED = booleanPreferencesKey("drawer_expanded_at_start")
        val IDLE_BACKGROUND = stringPreferencesKey("idle_background")

        /** Kept as one ordered line per id: a set would lose the order of the favorites. */
        const val SEPARATOR = "\n"
    }
}
