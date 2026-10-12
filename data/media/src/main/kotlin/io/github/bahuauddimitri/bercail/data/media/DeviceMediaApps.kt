package io.github.bahuauddimitri.bercail.data.media

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.bahuauddimitri.bercail.core.domain.apps.App
import io.github.bahuauddimitri.bercail.core.domain.apps.AppsSource
import io.github.bahuauddimitri.bercail.core.domain.media.LastHeard
import io.github.bahuauddimitri.bercail.core.domain.media.MediaAppsSource
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Opens the file of what was last heard. One store per file and per process; [scope] lives as long as the app.
 * A damaged file starts again from nothing: the only loss is one "Reprendre".
 */
fun listeningStore(file: File, scope: CoroutineScope): DataStore<Preferences> = PreferenceDataStoreFactory.create(
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    scope = scope
) { file }

/**
 * The installed apps that can play sound or video: those that answer the play button of a headset, as every
 * player does, and those declared to Android as music apps. They are found among [installed], so Android only
 * tells of a new one while someone listens. What was last heard is kept in a small file.
 */
class DeviceMediaApps(
    context: Context,
    installed: AppsSource,
    private val store: DataStore<Preferences>,
    background: CoroutineDispatcher = Dispatchers.IO
) : MediaAppsSource {
    private val packages = context.applicationContext.packageManager

    override val apps: Flow<List<App>> = installed.apps
        .map { all ->
            val players = players()
            all.filter { it.id.substringBefore('/') in players }.sortedBy { it.name.lowercase() }
        }
        .distinctUntilChanged()
        .flowOn(background)

    override val lastHeard: Flow<LastHeard?> = store.data
        .map { saved -> saved[APP]?.let { app -> saved[TITLE]?.let { LastHeard(app, it) } } }
        .distinctUntilChanged()

    override suspend fun remember(heard: LastHeard) {
        store.edit { saved ->
            saved[APP] = heard.appId
            saved[TITLE] = heard.title
        }
    }

    /** The packages that play, whatever their screen. */
    private fun players(): Set<String> {
        val none = PackageManager.ResolveInfoFlags.of(0)
        val answering = packages.queryBroadcastReceivers(MEDIA_BUTTON, none).map { it.activityInfo.packageName }
        val declared = packages.queryIntentActivities(MUSIC_APP, none).map { it.activityInfo.packageName }
        return (answering + declared).toSet()
    }

    private companion object {
        val MEDIA_BUTTON = Intent(Intent.ACTION_MEDIA_BUTTON)
        val MUSIC_APP: Intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC)
        val APP = stringPreferencesKey("last_app")
        val TITLE = stringPreferencesKey("last_title")
    }
}
