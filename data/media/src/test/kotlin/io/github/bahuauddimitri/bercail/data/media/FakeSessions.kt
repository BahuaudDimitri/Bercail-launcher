package io.github.bahuauddimitri.bercail.data.media

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Looper
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowMediaSessionManager
import org.robolectric.util.ReflectionHelpers

/**
 * The media apps of a phone, played by the tests: each one opens a session and says what it has loaded, exactly
 * as Android hands it over to an app that may see the sessions.
 */
internal class FakeSessions(private val phone: Application) {
    private val android = phone.getSystemService(MediaSessionManager::class.java)
    private val open = mutableListOf<MediaController>()

    /** An app is installed under [name]. */
    fun install(app: String, name: String) {
        val info = ApplicationInfo().apply {
            packageName = app
            nonLocalizedLabel = name
        }
        shadowOf(phone.packageManager).installPackage(
            PackageInfo().apply {
                packageName = app
                applicationInfo = info
            }
        )
    }

    /** [app] opens a session. It becomes the first of the list, as an app that has just played. */
    fun open(
        app: String,
        title: String? = "Looped",
        artist: String? = "Kiasmos",
        state: Int = PlaybackState.STATE_PLAYING,
        cover: Bitmap? = null
    ): MediaController {
        val session = MediaController(phone, MediaSession(phone, app).sessionToken)
        shadowOf(session).setPackageName(app)
        shadowOf(session).setMetadata(metadata(title, artist, cover = cover))
        shadowOf(session).setPlaybackState(playback(state))
        open.add(0, session)
        told()
        return session
    }

    /** The app closes its session. */
    fun close(session: MediaController) {
        open -= session
        told()
    }

    /** [session] has just played: Android now gives it first. */
    fun bringFirst(session: MediaController) {
        open -= session
        open.add(0, session)
        told()
    }

    /** The app of [session] says what it now has loaded. */
    fun load(
        session: MediaController,
        title: String?,
        artist: String? = "Kiasmos",
        seconds: Long = 245,
        cover: Bitmap? = null
    ) {
        val loaded = metadata(title, artist, seconds, cover)
        shadowOf(session).setMetadata(loaded)
        shadowOf(session).executeOnMetadataChanged(loaded)
        idle()
    }

    /** The app of [session] says whether it plays, where it is, and which buttons it accepts. */
    fun say(
        session: MediaController,
        state: Int,
        positionSeconds: Long = 0,
        at: Long = 0,
        actions: Long = EVERY_BUTTON
    ) {
        val said = playback(state, positionSeconds, at, actions)
        shadowOf(session).setPlaybackState(said)
        shadowOf(session).executeOnPlaybackStateChanged(said)
        idle()
    }

    /** How many listeners Bercail left on [session]. */
    fun watchersOf(session: MediaController): Int = shadowOf(session).callbacks.size

    /** How many times Bercail still asks Android to be told when the sessions change. */
    val listeners: Int get() = android("listeners").size

    /** Android now gives the sessions in their new order, and tells it once to whoever asked. */
    private fun told() {
        // Makes sure the manager exists before its list is filled.
        checkNotNull(android)
        val sessions = android("controllers")
        sessions.clear()
        sessions.addAll(open)
        android("listeners").toList().forEach {
            (it as MediaSessionManager.OnActiveSessionsChangedListener).onActiveSessionsChanged(open.toList())
        }
        idle()
    }

    @Suppress("UNCHECKED_CAST")
    private fun android(field: String): MutableCollection<Any> =
        ReflectionHelpers.getStaticField(ShadowMediaSessionManager::class.java, field)

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    companion object {
        const val SPOTIFY = "com.spotify.music"
        const val YOUTUBE = "com.google.android.youtube"
        const val YOUTUBE_MUSIC = "com.google.android.apps.youtube.music"

        const val EVERY_BUTTON = PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_SKIP_TO_PREVIOUS or
            PlaybackState.ACTION_SKIP_TO_NEXT or
            PlaybackState.ACTION_SEEK_TO

        fun metadata(title: String?, artist: String?, seconds: Long = 245, cover: Bitmap? = null): MediaMetadata =
            MediaMetadata.Builder().apply {
                if (title != null) putString(MediaMetadata.METADATA_KEY_TITLE, title)
                if (artist != null) putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                putLong(MediaMetadata.METADATA_KEY_DURATION, seconds * MILLIS)
                if (cover != null) putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, cover)
            }.build()

        fun playback(state: Int, positionSeconds: Long = 0, at: Long = 0, actions: Long = EVERY_BUTTON): PlaybackState =
            PlaybackState.Builder()
                .setState(state, positionSeconds * MILLIS, 1f, at)
                .setActions(actions)
                .build()

        private const val MILLIS = 1000L
    }
}
