package io.github.bahuauddimitri.bercail.data.media

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import io.github.bahuauddimitri.bercail.core.domain.media.MediaAction
import io.github.bahuauddimitri.bercail.core.domain.media.MediaContent
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** One media session at one moment: what its app says it has loaded, and whether it plays. */
internal class Heard(val session: MediaController, val metadata: MediaMetadata?, val playback: PlaybackState?) {
    /** Something is, or is about to be, heard. */
    val isMoving: Boolean get() = playback?.state in MOVING

    /** Where the content is at [now]: an app only says where it was when it last spoke. */
    fun positionAt(now: Long): Duration {
        val playback = playback ?: return Duration.ZERO
        val since = if (isMoving) ((now - playback.lastPositionUpdateTime) * playback.playbackSpeed).toLong() else 0
        val position = (playback.position + since).coerceAtLeast(0).milliseconds
        val duration = metadata?.duration ?: Duration.ZERO
        return if (duration > Duration.ZERO) position.coerceAtMost(duration) else position
    }

    private companion object {
        val MOVING = setOf(
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_BUFFERING,
            PlaybackState.STATE_FAST_FORWARDING,
            PlaybackState.STATE_REWINDING,
            PlaybackState.STATE_SKIPPING_TO_NEXT,
            PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
            PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM
        )
    }
}

/**
 * The sessions Bercail listens to, in Android's order. Each one tells by itself when its content or its state
 * changes; a session that is gone is no longer listened to. Android hands a new object for the same session
 * each time it speaks: the session's token tells which is which.
 */
internal class Followed(private val main: Handler, private val tell: (List<Heard>) -> Unit) {
    private var ears = LinkedHashMap<MediaSession.Token, Ear>()

    @Synchronized
    fun follow(sessions: List<MediaController>) {
        val now = LinkedHashMap<MediaSession.Token, Ear>()
        sessions.forEach { session ->
            val token = session.sessionToken
            now[token] = ears.remove(token) ?: Ear(session).also { session.registerCallback(it, main) }
        }
        ears.values.forEach { it.session.unregisterCallback(it) }
        ears = now
        told()
    }

    @Synchronized
    private fun told() = tell(ears.values.map { Heard(it.session, it.metadata, it.playback) })

    private inner class Ear(val session: MediaController) : MediaController.Callback() {
        var metadata: MediaMetadata? = session.metadata
        var playback: PlaybackState? = session.playbackState

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            this.metadata = metadata
            told()
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            playback = state
            told()
        }
    }
}

/**
 * Turns what a session says into what Bercail shows. A cover is heavy: what was read from each thing an app
 * said it loaded is kept, so that a cover is only read once.
 */
internal class SessionReader(private val context: Context) {
    private val contents = IdentityHashMap<MediaMetadata, MediaContent?>()
    private val names = HashMap<String, String>()
    private val ids = HashMap<String, String>()

    /** Forgets what was read from the sessions that are gone, and their covers with it. */
    fun keepOnly(heard: List<Heard>) {
        val still = Collections.newSetFromMap(IdentityHashMap<MediaMetadata, Boolean>())
        heard.mapNotNullTo(still) { it.metadata }
        contents.keys.retainAll(still)
    }

    /** What the session has loaded, or nothing if it is stopped or its app has not given a title yet. */
    fun loaded(heard: Heard): MediaState.Loaded? {
        val playing = when {
            heard.isMoving -> true
            heard.playback?.state == PlaybackState.STATE_PAUSED -> false
            else -> null
        }
        val content = heard.metadata?.takeIf { playing != null }?.let { said ->
            contents.getOrPut(said) { read(said, heard.session.packageName) }
        }
        return if (playing != null && content != null) {
            MediaState.Loaded(content, playing, heard.playback.accepted())
        } else {
            null
        }
    }

    private fun read(metadata: MediaMetadata, app: String): MediaContent? {
        val title = metadata.text(MediaMetadata.METADATA_KEY_TITLE, MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        val artist = metadata.text(
            MediaMetadata.METADATA_KEY_ARTIST,
            MediaMetadata.METADATA_KEY_ALBUM_ARTIST,
            MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE
        )
        return title?.let {
            val cover = COVER_KEYS.firstNotNullOfOrNull(metadata::getBitmap)?.asCover()
            MediaContent(
                title = title,
                // Spotify adds where the track comes from after the artist: "Kiasmos • À partir de…".
                artist = artist.orEmpty().substringBefore(NOT_THE_ARTIST).trim(),
                app = names.getOrPut(app) { nameOf(app) },
                duration = metadata.duration,
                colors = cover?.colors,
                cover = cover?.picture,
                appId = ids.getOrPut(app) { idOf(app) }
            )
        }
    }

    /** The app as the list of installed apps names it: by what its icon opens. Empty for an app without icon. */
    private fun idOf(app: String): String =
        context.packageManager.getLaunchIntentForPackage(app)?.component?.flattenToShortString().orEmpty()

    /** The name of the app as the phone writes it; its technical name if Android does not let Bercail see it. */
    private fun nameOf(app: String): String = try {
        val packages = context.packageManager
        packages.getApplicationLabel(packages.getApplicationInfo(app, 0)).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        app
    }

    private companion object {
        /** Where apps put their cover, the album's first. */
        val COVER_KEYS = listOf(
            MediaMetadata.METADATA_KEY_ALBUM_ART,
            MediaMetadata.METADATA_KEY_ART,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON
        )

        const val NOT_THE_ARTIST = " • "
    }
}

/** The first of [keys] the app filled in. */
private fun MediaMetadata.text(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { key -> getText(key)?.toString()?.takeIf { it.isNotBlank() } }

/** Zero when the app does not say: a live stream has no end. */
private val MediaMetadata.duration: Duration
    get() = getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0).milliseconds

/** The buttons the app accepts, besides play and pause. */
private fun PlaybackState?.accepted(): Set<MediaAction> = buildSet {
    val actions = this@accepted?.actions ?: 0L
    if (actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L) add(MediaAction.Previous)
    if (actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L) add(MediaAction.Next)
    if (actions and PlaybackState.ACTION_SEEK_TO != 0L) add(MediaAction.Seek)
}
