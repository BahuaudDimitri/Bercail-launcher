package io.github.bahuauddimitri.bercail.core.testing

import io.github.bahuauddimitri.bercail.core.domain.update.InstallAnswer
import io.github.bahuauddimitri.bercail.core.domain.update.Release
import io.github.bahuauddimitri.bercail.core.domain.update.ReleaseFeed
import io.github.bahuauddimitri.bercail.core.domain.update.UpdateJournal
import io.github.bahuauddimitri.bercail.core.domain.update.UpdateNotes
import io.github.bahuauddimitri.bercail.core.domain.update.UpdatePackages
import io.github.bahuauddimitri.bercail.core.domain.update.UpdateStatus
import io.github.bahuauddimitri.bercail.core.domain.update.Updates
import io.github.bahuauddimitri.bercail.core.domain.update.Version
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow

/** A release as GitHub would announce it, for the tests. */
fun release(version: String): Release = Release(
    version = requireNotNull(Version.parse(version)) { "$version is not a version" },
    packageUrl = "https://example.test/bercail-$version.apk",
    size = 2_400_000
)

/** The published releases, set by hand. It counts how many times it was read. */
class FakeReleaseFeed(var published: Release? = null) : ReleaseFeed {
    /** False when the network is down or GitHub does not answer. */
    var reachable = true
    var reads = 0

    override suspend fun latest(): Release? {
        reads++
        return published.takeIf { reachable }
    }
}

/** The packages on a phone that remembers what was fetched and what was handed to Android. */
class FakeUpdatePackages : UpdatePackages {
    /** False when the package cannot be fetched. */
    var reachable = true

    /** False until the user lets Bercail install packages, in the settings of the phone. */
    var allowedToInstall = true

    /** What Android answers to the next installation. */
    var answer = InstallAnswer.Installed

    /** The versions fetched so far, in order. */
    val downloads = mutableListOf<Version>()

    /** The versions handed to Android so far, in order: "0.9.0 without asking", "0.9.0 asking the user". */
    val installs = mutableListOf<String>()

    /** How many times the kept packages were removed. */
    var cleared = 0

    /** When set, a download waits for it to complete: the tests can leave the home screen meanwhile. */
    var slowNetwork: CompletableDeferred<Unit>? = null
    private var kept: Release? = null

    override fun has(release: Release): Boolean = kept == release

    override fun mayInstallUnasked(): Boolean = allowedToInstall

    override suspend fun download(release: Release): Boolean {
        downloads += release.version
        slowNetwork?.await()
        if (reachable) kept = release
        return reachable
    }

    override suspend fun install(release: Release, askUser: Boolean): InstallAnswer {
        installs += "${release.version} " + if (askUser) "asking the user" else "without asking"
        return answer
    }

    override fun clear() {
        cleared++
        kept = null
    }
}

/** Notes kept in memory. */
class FakeUpdateJournal(var notes: UpdateNotes = UpdateNotes()) : UpdateJournal {
    override suspend fun read(): UpdateNotes = notes

    override suspend fun write(notes: UpdateNotes) {
        this.notes = notes
    }
}

/** An update whose status the tests set by hand. It counts the touches on its row. */
class FakeUpdates(
    override val current: Version? = Version.parse("0.8.1"),
    status: UpdateStatus = UpdateStatus.UpToDate()
) : Updates {
    override val status = MutableStateFlow(status)
    var proceeded = 0

    override suspend fun proceed() {
        proceeded++
    }
}
