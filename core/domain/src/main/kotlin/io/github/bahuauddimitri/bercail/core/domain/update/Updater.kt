package io.github.bahuauddimitri.bercail.core.domain.update

import io.github.bahuauddimitri.bercail.core.domain.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/**
 * Bercail updates itself, and only works for that when the home screen comes and goes: no timer, no background
 * job. When the home screen shows, the releases are read if the daily look is due and the package of a newer
 * version is fetched. When it hides, the package is handed to Android: nobody is watching the home screen while
 * Bercail restarts. Whoever runs [onHomeShown] cancels it when the home screen goes away.
 */
class Updater(
    override val current: Version?,
    private val feed: ReleaseFeed,
    private val packages: UpdatePackages,
    private val journal: UpdateJournal,
    private val clock: Clock,
    /** Where the reading, the files and the network run: off the main thread on a phone. */
    private val background: CoroutineContext = EmptyCoroutineContext
) : Updates {
    private val state = MutableStateFlow(if (current == null) UpdateStatus.Off else UpdateStatus.UpToDate())
    override val status: StateFlow<UpdateStatus> = state.asStateFlow()

    private val busy = Mutex()

    // Forgotten when Bercail restarts: one more quiet attempt then costs nothing.
    private var needsUser = false
    private var refused = false
    private var downloadFailedAt: LocalDateTime? = null

    /** The home screen just appeared. */
    suspend fun onHomeShown() = oneAtATime {
        val notes = journal.read()
        settle(if (isCheckDue(notes, clock.now())) check(notes) else notes, patient = true)
    }

    /** The home screen just went away: a ready version can take over, unless Android wants the user's word. */
    suspend fun onHomeHidden() = oneAtATime {
        val ready = state.value as? UpdateStatus.Ready
        if (ready != null && !ready.needsUser) install(askUser = false)
    }

    override suspend fun proceed() = oneAtATime {
        if (state.value is UpdateStatus.Ready) {
            install(
                askUser = true
            )
        } else {
            settle(check(journal.read()), patient = false)
        }
    }

    /** A build without a version does nothing, and a request that arrives during another one is dropped. */
    private suspend fun oneAtATime(work: suspend () -> Unit) {
        if (current == null || !busy.tryLock()) return
        try {
            withContext(background) { work() }
        } finally {
            busy.unlock()
        }
    }

    private suspend fun check(notes: UpdateNotes): UpdateNotes {
        val before = state.value
        state.value = UpdateStatus.Checking
        val latest = try {
            feed.latest()
        } finally {
            state.value = before
        }
        if (latest != null) refused = false
        // An unreadable answer says nothing about the version already found.
        val found = if (latest == null) notes.found else latest
        return UpdateNotes(clock.now(), reached = latest != null, found = found?.takeIf(::isNewer))
            .also { journal.write(it) }
    }

    /** Brings the phone as far as it can go without the user: package fetched, ready to install. */
    private suspend fun settle(notes: UpdateNotes, patient: Boolean) {
        val found = notes.found?.takeIf(::isNewer)
        if (found == null) {
            if (notes.found != null) forget(notes)
            state.value = UpdateStatus.UpToDate(
                when {
                    refused -> UpdateTrouble.Refused
                    !notes.reached -> UpdateTrouble.Unreachable
                    else -> null
                }
            )
            return
        }
        // A download that just failed is not tried again at each return to the home screen, unless asked.
        val mayFetch = !patient || !failedRecently()
        if (!packages.has(found) && mayFetch) download(found)
        state.value = if (packages.has(found)) {
            // Without Android's permission, a quiet attempt would only be turned down: wait for a touch.
            UpdateStatus.Ready(found.version, needsUser || !packages.mayInstallUnasked())
        } else {
            UpdateStatus.Waiting(found.version)
        }
    }

    private suspend fun download(found: Release) {
        state.value = UpdateStatus.Downloading(found.version)
        val fetched = try {
            packages.download(found)
        } finally {
            // Also when the home screen goes away in the middle: the download starts again at the next return.
            state.value = UpdateStatus.Waiting(found.version)
        }
        downloadFailedAt = if (fetched) null else clock.now()
    }

    private suspend fun install(askUser: Boolean) {
        val notes = journal.read()
        val found = notes.found ?: return
        val ready = state.value
        state.value = UpdateStatus.Installing(found.version)
        val answer = try {
            packages.install(found, askUser)
        } finally {
            state.value = ready
        }
        when (answer) {
            // Android is about to stop Bercail and start the new version.
            InstallAnswer.Installed -> Unit

            InstallAnswer.NeedsUser, InstallAnswer.Declined -> {
                needsUser = true
                state.value = UpdateStatus.Ready(found.version, needsUser = true)
            }

            InstallAnswer.Refused -> {
                refused = true
                forget(notes)
                state.value = UpdateStatus.UpToDate(UpdateTrouble.Refused)
            }
        }
    }

    private suspend fun forget(notes: UpdateNotes) {
        packages.clear()
        journal.write(notes.copy(found = null))
    }

    private fun isNewer(release: Release) = current != null && release.version > current

    private fun failedRecently(): Boolean {
        val failedAt = downloadFailedAt ?: return false
        val now = clock.now()
        return now >= failedAt && Duration.between(failedAt, now) < AFTER_A_FAILURE
    }
}
