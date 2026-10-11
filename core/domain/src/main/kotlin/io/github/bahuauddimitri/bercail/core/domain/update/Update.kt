package io.github.bahuauddimitri.bercail.core.domain.update

import java.time.Duration
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

/** A version of Bercail as its releases number them: 0.8.1. */
data class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version> {
    override fun compareTo(other: Version): Int =
        compareValuesBy(this, other, Version::major, Version::minor, Version::patch)

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        private val WRITTEN = Regex("""v?(\d{1,6})\.(\d{1,6})\.(\d{1,6})""")

        /** Reads "0.8.1" or the tag "v0.8.1". Anything else, like the "0.0.0-dev" of a local build, is no version. */
        fun parse(text: String): Version? = WRITTEN.matchEntire(text)?.destructured?.let { (major, minor, patch) ->
            Version(major.toInt(), minor.toInt(), patch.toInt())
        }
    }
}

/** A published version and where its package is. [sha256] is the fingerprint the package must have, if known. */
data class Release(val version: Version, val packageUrl: String, val size: Long, val sha256: String? = null)

/** Where the versions of Bercail are published. */
interface ReleaseFeed {
    /** The latest published version, or nothing when it cannot be read (no network, unexpected answer). */
    suspend fun latest(): Release?
}

/** What Android answers when it is handed a package. */
enum class InstallAnswer {
    /** The new version replaces this one: Android stops Bercail and starts it again. */
    Installed,

    /** Android wants the user's agreement first, and nobody was asked. */
    NeedsUser,

    /** The user was asked and said no. */
    Declined,

    /** Android turned the package down: damaged, older, or not signed as Bercail. */
    Refused
}

/** The packages of the versions to come: fetched, kept on the phone, handed to Android. */
interface UpdatePackages {
    /** Whether the package of this release is on the phone, complete. */
    fun has(release: Release): Boolean

    /** Fetches the package and checks it. False when it could not be fetched or is not what was announced. */
    suspend fun download(release: Release): Boolean

    /**
     * Whether Android lets Bercail install a package without a question. False until the user has allowed it
     * once, on the screen Android shows at the first installation.
     */
    fun mayInstallUnasked(): Boolean

    /** Hands the package to Android. With [askUser], Android may show its confirmation screen. */
    suspend fun install(release: Release, askUser: Boolean): InstallAnswer

    /** Removes every package kept on the phone. */
    fun clear()
}

/** What Bercail remembers between two looks at the releases. */
data class UpdateNotes(
    val checkedAt: LocalDateTime? = null,
    /** Whether the releases could be read that time. */
    val reached: Boolean = true,
    /** The newer version found, until it is installed. */
    val found: Release? = null
)

/** Keeps the [UpdateNotes] on the phone. */
interface UpdateJournal {
    suspend fun read(): UpdateNotes

    suspend fun write(notes: UpdateNotes)
}

/** Why the last attempt did not go through. */
enum class UpdateTrouble { Unreachable, Refused }

/** Where the update of Bercail stands. */
sealed interface UpdateStatus {
    /** A build without a version, like the debug one: it never updates itself. */
    data object Off : UpdateStatus

    data class UpToDate(val trouble: UpdateTrouble? = null) : UpdateStatus

    data object Checking : UpdateStatus

    data class Downloading(val version: Version) : UpdateStatus

    /** A newer version exists, its package is not on the phone yet. */
    data class Waiting(val version: Version) : UpdateStatus

    /** The package is on the phone. With [needsUser], Android wants an agreement: it waits for a touch. */
    data class Ready(val version: Version, val needsUser: Boolean = false) : UpdateStatus

    data class Installing(val version: Version) : UpdateStatus
}

/** The update of Bercail as the settings see it. */
interface Updates {
    /** The version running, or nothing for a build without one. */
    val current: Version?

    val status: Flow<UpdateStatus>

    /** What touching the update row does: look again, fetch, or install with the user's agreement. */
    suspend fun proceed()
}

/**
 * Bercail looks at its releases once a day. After a look that failed, it tries again one hour later, not at each
 * return to the home screen. A clock that went back never blocks the next look.
 */
fun isCheckDue(notes: UpdateNotes, now: LocalDateTime): Boolean {
    val last = notes.checkedAt
    val wait = if (notes.reached) BETWEEN_CHECKS else AFTER_A_FAILURE
    return last == null || now < last || Duration.between(last, now) >= wait
}

internal val BETWEEN_CHECKS: Duration = Duration.ofDays(1)
internal val AFTER_A_FAILURE: Duration = Duration.ofHours(1)

/**
 * What the search says under « Réglages de Bercail »: only a version that waits for the user's agreement. One
 * that installs by itself needs no announcement.
 */
fun updateNotice(status: UpdateStatus): String? =
    (status as? UpdateStatus.Ready)?.takeIf { it.needsUser }?.let { "Version ${it.version} prête à installer" }

/** The update row of the settings: the version running, what is going on, and whether a touch does something. */
data class UpdateLine(val title: String, val detail: String, val touchable: Boolean)

fun updateLine(current: Version?, status: UpdateStatus): UpdateLine {
    val title = if (current == null) "Bercail, version de mise au point" else "Bercail $current"
    return when (status) {
        UpdateStatus.Off -> UpdateLine(title, "Elle ne se met pas à jour toute seule", touchable = false)

        is UpdateStatus.UpToDate -> UpdateLine(
            title,
            when (status.trouble) {
                null -> "À jour · toucher pour vérifier"
                UpdateTrouble.Unreachable -> "Vérification impossible · toucher pour réessayer"
                UpdateTrouble.Refused -> "La dernière version n'a pas pu s'installer · toucher pour réessayer"
            },
            touchable = true
        )

        UpdateStatus.Checking -> UpdateLine(title, "Recherche d'une mise à jour…", touchable = false)

        is UpdateStatus.Downloading ->
            UpdateLine(title, "Téléchargement de la version ${status.version}…", touchable = false)

        is UpdateStatus.Waiting ->
            UpdateLine(title, "Version ${status.version} disponible · toucher pour la télécharger", touchable = true)

        is UpdateStatus.Ready -> UpdateLine(
            title,
            if (status.needsUser) {
                "Version ${status.version} prête · toucher pour l'installer"
            } else {
                "Version ${status.version} prête · elle s'installera quand tu quitteras l'accueil"
            },
            touchable = true
        )

        is UpdateStatus.Installing ->
            UpdateLine(title, "Installation de la version ${status.version}…", touchable = false)
    }
}
