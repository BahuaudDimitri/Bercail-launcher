package io.github.bahuauddimitri.bercail.data.messages

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * What the user has already seen in Bercail: for each conversation, when it had last moved at that moment. No
 * name and no message are kept, only the identifier Beeper gives the conversation and that time.
 */
interface SeenJournal {
    val seen: Flow<Map<String, Long>>

    suspend fun note(conversationId: String, stamp: Long)
}

/**
 * Opens the file of what was seen. One store per file and per process; [scope] lives as long as the app.
 * A damaged file starts again from nothing: the only loss is a few badges that come back.
 */
fun seenStore(file: File, scope: CoroutineScope): DataStore<Preferences> = PreferenceDataStoreFactory.create(
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    scope = scope
) { file }

/** What was seen, kept in a small file from one launch to the next. Only the latest conversations stay in it. */
class StoredSeen(private val store: DataStore<Preferences>) : SeenJournal {
    override val seen: Flow<Map<String, Long>> = store.data
        .map { saved ->
            saved.asMap().entries
                .filter { it.key.name.startsWith(PREFIX) }
                .associate { it.key.name.removePrefix(PREFIX) to it.value as Long }
        }
        .distinctUntilChanged()

    override suspend fun note(conversationId: String, stamp: Long) {
        store.edit { saved ->
            saved[longPreferencesKey(PREFIX + conversationId)] = stamp
            val kept = saved.asMap().entries.filter { it.key.name.startsWith(PREFIX) }
            kept.sortedByDescending { it.value as Long }.drop(MOST_KEPT).forEach { saved.remove(it.key) }
        }
    }

    private companion object {
        const val PREFIX = "seen:"

        /** Twice what the home screen reads of Beeper: an older conversation that moves again is unread anyway. */
        const val MOST_KEPT = 200
    }
}
