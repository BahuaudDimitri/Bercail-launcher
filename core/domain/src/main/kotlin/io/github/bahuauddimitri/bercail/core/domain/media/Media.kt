package io.github.bahuauddimitri.bercail.core.domain.media

import io.github.bahuauddimitri.bercail.core.domain.Picture
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow

/** The two colors of a cover (ARGB), which tint Brume. */
data class CoverColors(val first: Int, val second: Int)

/** What is loaded in a media app: a song, a video, a podcast. */
data class MediaContent(
    val title: String,
    val artist: String,
    /** The name of the app that holds it, as the phone writes it. */
    val app: String,
    /** Zero when the app does not say: a live stream has no end. */
    val duration: Duration,
    /** The two colors of the cover. Without them, Brume takes the colors of the moment. */
    val colors: CoverColors? = null,
    val cover: Picture? = null,
    /** The app that holds it, named as the installed apps are; empty when it cannot be opened from a launcher. */
    val appId: String = "",
    /** The album it comes from; empty when the app does not say (a video, a radio). */
    val album: String = ""
)

/** What stands under the title on the Listen screen: « Artiste · Album », or whichever of the two is known. */
val MediaContent.credits: String
    get() = listOf(artist, album).filter { it.isNotEmpty() }.joinToString(" · ")

/** What a media app accepts from a remote, besides play and pause. YouTube, for one, has no "previous". */
enum class MediaAction { Previous, Next, Seek }

/** The three media states: nothing, paused, playing. */
sealed interface MediaState {
    val isLoaded: Boolean
    val isPlaying: Boolean

    data object None : MediaState {
        override val isLoaded = false
        override val isPlaying = false
    }

    data class Loaded(
        val content: MediaContent,
        val playing: Boolean,
        /** The buttons the remote may show for this app. */
        val actions: Set<MediaAction> = MediaAction.entries.toSet()
    ) : MediaState {
        override val isLoaded get() = true
        override val isPlaying get() = playing
    }
}

/**
 * What Bercail shows when several apps have something loaded. The [sessions] come as Android gives them, the one
 * that played last first: the one that plays wins, otherwise the one that played last.
 */
fun shownAmong(sessions: List<MediaState.Loaded>): MediaState =
    sessions.firstOrNull { it.playing } ?: sessions.firstOrNull() ?: MediaState.None

interface MediaSource {
    /** What is loaded and whether it plays, emitted again on every change. */
    val state: Flow<MediaState>

    /** Where the content is, refreshed about once per second while it plays and someone listens. */
    val position: Flow<Duration>

    fun play()
    fun pause()
    fun next()
    fun previous()
    fun seekTo(position: Duration)

    /** Shows what is loaded in its own app. */
    fun openApp()

    /**
     * Asks the app that played last to play again, as the button of a headset does. Nothing is loaded yet: if
     * that app is gone, nothing happens.
     */
    fun resume()
}

enum class PreviousAction { Restart, PreviousTrack }

/** "Previous" goes back to the start once past 5 seconds, to the previous track before that. */
fun previousAction(position: Duration): PreviousAction =
    if (position > RESTART_THRESHOLD) PreviousAction.Restart else PreviousAction.PreviousTrack

/** A position in a track as the remote writes it: 1:12. */
fun Duration.asTrackTime(): String = "$inWholeMinutes:${"%02d".format(inWholeSeconds % SECONDS_PER_MINUTE)}"

/** The share of the content already played, from 0 to 1. */
fun progress(position: Duration, duration: Duration): Float =
    if (duration <= Duration.ZERO) 0f else (position / duration).toFloat().coerceIn(0f, 1f)

/** The position reached by touching the progress bar at [fraction] of its width. */
fun positionAt(fraction: Float, duration: Duration): Duration =
    (duration.inWholeSeconds * fraction.coerceIn(0f, 1f)).toInt().seconds

private val RESTART_THRESHOLD = 5.seconds
private const val SECONDS_PER_MINUTE = 60
