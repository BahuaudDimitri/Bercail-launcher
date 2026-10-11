package io.github.bahuauddimitri.bercail.data.update

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.bahuauddimitri.bercail.core.domain.update.Release
import io.github.bahuauddimitri.bercail.core.domain.update.UpdateJournal
import io.github.bahuauddimitri.bercail.core.domain.update.UpdateNotes
import io.github.bahuauddimitri.bercail.core.domain.update.Version
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first

/**
 * Opens the file of the update notes. One store per file and per process; [scope] lives as long as the app.
 * A damaged file starts again from nothing: the only loss is one more look at the releases.
 */
fun updateNotesStore(file: File, scope: CoroutineScope): DataStore<Preferences> = PreferenceDataStoreFactory.create(
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    scope = scope
) { file }

/** What Bercail remembers about its updates, kept in a file: read at each look, written when something changes. */
class StoredUpdateNotes(private val store: DataStore<Preferences>) : UpdateJournal {
    override suspend fun read(): UpdateNotes = store.data.first().toNotes()

    override suspend fun write(notes: UpdateNotes) {
        store.edit { saved ->
            saved.clear()
            notes.checkedAt?.let { saved[CHECKED_AT] = it.toString() }
            saved[REACHED] = notes.reached
            notes.found?.let { found ->
                saved[FOUND_VERSION] = found.version.toString()
                saved[FOUND_URL] = found.packageUrl
                saved[FOUND_SIZE] = found.size
                found.sha256?.let { saved[FOUND_SHA_256] = it }
            }
        }
    }

    private fun Preferences.toNotes(): UpdateNotes = UpdateNotes(
        checkedAt = this[CHECKED_AT]?.let(::dateOrNull),
        reached = this[REACHED] ?: true,
        found = found()
    )

    private fun Preferences.found(): Release? {
        val version = this[FOUND_VERSION]?.let(Version::parse)
        val url = this[FOUND_URL]
        val size = this[FOUND_SIZE]
        return if (version != null && url != null && size != null) {
            Release(version, url, size, this[FOUND_SHA_256])
        } else {
            null
        }
    }

    private fun dateOrNull(text: String): LocalDateTime? = try {
        LocalDateTime.parse(text)
    } catch (_: DateTimeParseException) {
        null
    }

    private companion object {
        val CHECKED_AT = stringPreferencesKey("checked_at")
        val REACHED = booleanPreferencesKey("reached")
        val FOUND_VERSION = stringPreferencesKey("found_version")
        val FOUND_URL = stringPreferencesKey("found_url")
        val FOUND_SIZE = longPreferencesKey("found_size")
        val FOUND_SHA_256 = stringPreferencesKey("found_sha256")
    }
}
