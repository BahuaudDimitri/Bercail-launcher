package io.github.bahuauddimitri.bercail.core.domain.media

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow

/** The two colors of a cover (ARGB), which tint Brume. */
data class CoverColors(val first: Int, val second: Int)

/** What is loaded in a media app: a song, a video, a podcast. */
data class MediaContent(
    val title: String,
    val artist: String,
    val app: String,
    val duration: Duration,
    val colors: CoverColors
)

/** The three media states: nothing, paused, playing. */
sealed interface MediaState {
    val isLoaded: Boolean
    val isPlaying: Boolean

    data object None : MediaState {
        override val isLoaded = false
        override val isPlaying = false
    }

    data class Loaded(val content: MediaContent, val playing: Boolean) : MediaState {
        override val isLoaded get() = true
        override val isPlaying get() = playing
    }
}

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
