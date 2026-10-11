package io.github.bahuauddimitri.bercail

import android.content.Context
import io.github.bahuauddimitri.bercail.core.domain.update.ReleaseFeed
import io.github.bahuauddimitri.bercail.core.domain.update.UpdateJournal
import io.github.bahuauddimitri.bercail.core.domain.update.UpdatePackages
import io.github.bahuauddimitri.bercail.core.domain.update.Version
import io.github.bahuauddimitri.bercail.data.update.GitHubReleases
import io.github.bahuauddimitri.bercail.data.update.PhonePackages
import io.github.bahuauddimitri.bercail.data.update.StoredUpdateNotes
import io.github.bahuauddimitri.bercail.data.update.updateNotesStore
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/** What the update of Bercail is made of. On a phone, [onPhone]; the tests plug their own parts. */
class UpdateParts(
    /** The version running. A build without one, like the debug app, never updates itself. */
    val version: Version?,
    val feed: ReleaseFeed,
    val packages: UpdatePackages,
    val journal: UpdateJournal,
    /** Where the reading, the files and the network run. */
    val background: CoroutineContext = Dispatchers.IO
) {
    companion object {
        private const val NOTES_FILE = "datastore/update.preferences_pb"

        /** The real parts: the releases on GitHub, the package handed to Android, the notes in a file. */
        fun onPhone(context: Context, scope: CoroutineScope) = UpdateParts(
            version = Version.parse(installedVersion(context)),
            feed = GitHubReleases(),
            packages = PhonePackages(context),
            journal = StoredUpdateNotes(updateNotesStore(File(context.filesDir, NOTES_FILE), scope))
        )

        private fun installedVersion(context: Context): String =
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
}
