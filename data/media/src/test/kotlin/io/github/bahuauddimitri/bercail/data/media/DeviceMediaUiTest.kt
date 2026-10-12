package io.github.bahuauddimitri.bercail.data.media

import android.app.Application
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.media.session.PlaybackState
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.media.MediaAction
import io.github.bahuauddimitri.bercail.core.domain.media.MediaContent
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import io.github.bahuauddimitri.bercail.core.testing.FakeAccess
import io.github.bahuauddimitri.bercail.data.media.FakeSessions.Companion.SPOTIFY
import io.github.bahuauddimitri.bercail.data.media.FakeSessions.Companion.YOUTUBE
import io.github.bahuauddimitri.bercail.data.media.FakeSessions.Companion.YOUTUBE_MUSIC
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** What plays on the phone, read from Android's media sessions. The media apps are played by [FakeSessions]. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class DeviceMediaUiTest {
    private val phone = ApplicationProvider.getApplicationContext<Application>()
    private val apps = FakeSessions(phone).apply {
        install(SPOTIFY, "Spotify")
        install(YOUTUBE, "YouTube")
        install(YOUTUBE_MUSIC, "YouTube Music")
    }
    private val access = FakeAccess()
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = UnconfinedTestDispatcher(scheduler)

    /** The buttons pressed so far, with the app they went to: "com.spotify.music: pause". */
    private val pressed = mutableListOf<String>()

    private val media = DeviceMedia(
        context = phone,
        access = access,
        background = dispatcher,
        elapsed = { scheduler.currentTime },
        buttons = { session ->
            object : MediaButtons {
                override fun play() = press("play")
                override fun pause() = press("pause")
                override fun next() = press("next")
                override fun previous() = press("previous")
                override fun seekTo(millis: Long) = press("seek to $millis")

                private fun press(button: String) {
                    pressed += "${session.packageName}: $button"
                }
            }
        }
    )

    /** What the home screen would show now. Reading it means someone listens, as when the home screen is shown. */
    private class Watching(val job: Job) {
        var state: MediaState? = null
        var position: Duration? = null
        val loaded: MediaState.Loaded get() = state as MediaState.Loaded
        val content: MediaContent get() = loaded.content
    }

    private fun TestScope.watch(): Watching {
        // A child of the test: whatever still listens when the test ends is stopped with it.
        val job = Job(backgroundScope.coroutineContext[Job])
        val watching = Watching(job)
        backgroundScope.launch(dispatcher + job) { media.state.collect { watching.state = it } }
        backgroundScope.launch(dispatcher + job) { media.position.collect { watching.position = it } }
        return watching
    }

    private fun cover(width: Int = 300, height: Int = 300): Bitmap {
        // Two thirds of blue above one third of orange.
        val pixels = IntArray(width * height) { if (it / width < height * 2 / 3) BLUE else ORANGE }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    // ---------- what is loaded ----------

    @Test
    fun `sans session média, rien n'est chargé`() = runTest(dispatcher) {
        assertThat(watch().state).isEqualTo(MediaState.None)
    }

    @Test
    fun `une session en lecture donne son titre, son artiste, sa durée et le nom de son app`() = runTest(dispatcher) {
        apps.open(SPOTIFY, "Looped", "Kiasmos")

        val shown = watch()

        assertThat(shown.loaded.playing).isTrue()
        assertThat(shown.content.title).isEqualTo("Looped")
        assertThat(shown.content.artist).isEqualTo("Kiasmos")
        assertThat(shown.content.duration).isEqualTo(245.seconds)
        assertThat(shown.content.app).isEqualTo("Spotify")
    }

    @Test
    fun `en pause, le contenu reste chargé`() = runTest(dispatcher) {
        apps.open(SPOTIFY, "Looped", state = PlaybackState.STATE_PAUSED)

        val shown = watch()

        assertThat(shown.loaded.playing).isFalse()
        assertThat(shown.content.title).isEqualTo("Looped")
    }

    @Test
    fun `pendant le chargement d'un morceau, la session compte comme en lecture`() = runTest(dispatcher) {
        apps.open(SPOTIFY, state = PlaybackState.STATE_BUFFERING)

        assertThat(watch().loaded.playing).isTrue()
    }

    @Test
    fun `une session arrêtée n'est pas montrée`() = runTest(dispatcher) {
        apps.open(SPOTIFY, state = PlaybackState.STATE_STOPPED)

        assertThat(watch().state).isEqualTo(MediaState.None)
    }

    @Test
    fun `une session sans titre n'est pas montrée`() = runTest(dispatcher) {
        apps.open(SPOTIFY, title = null)

        assertThat(watch().state).isEqualTo(MediaState.None)
    }

    @Test
    fun `une vidéo sans artiste connu s'affiche avec son seul titre`() = runTest(dispatcher) {
        apps.open(YOUTUBE, "Tiny Desk Concert", artist = null)

        val shown = watch()

        assertThat(shown.content.title).isEqualTo("Tiny Desk Concert")
        assertThat(shown.content.artist).isEqualTo("")
        assertThat(shown.content.app).isEqualTo("YouTube")
    }

    // ---------- several apps ----------

    @Test
    fun `plusieurs sessions en pause, c'est celle qui a joué en dernier qui est montrée`() = runTest(dispatcher) {
        apps.open(SPOTIFY, "Looped", state = PlaybackState.STATE_PAUSED)
        apps.open(YOUTUBE_MUSIC, "Says", state = PlaybackState.STATE_PAUSED)

        assertThat(watch().content.title).isEqualTo("Says")
    }

    @Test
    fun `l'app qui se met à jouer prend la place de celle qui était en pause`() = runTest(dispatcher) {
        val spotify = apps.open(SPOTIFY, "Looped", state = PlaybackState.STATE_PAUSED)
        apps.open(YOUTUBE_MUSIC, "Says", state = PlaybackState.STATE_PAUSED)
        val shown = watch()

        apps.say(spotify, PlaybackState.STATE_PLAYING)

        assertThat(shown.content.title).isEqualTo("Looped")
        assertThat(shown.loaded.playing).isTrue()
    }

    @Test
    fun `quand Android change l'ordre des sessions, l'accueil suit`() = runTest(dispatcher) {
        val spotify = apps.open(SPOTIFY, "Looped", state = PlaybackState.STATE_PAUSED)
        apps.open(YOUTUBE_MUSIC, "Says", state = PlaybackState.STATE_PAUSED)
        val shown = watch()

        apps.bringFirst(spotify)

        assertThat(shown.content.title).isEqualTo("Looped")
    }

    // ---------- changes come by themselves ----------

    @Test
    fun `un changement de morceau arrive tout seul`() = runTest(dispatcher) {
        val spotify = apps.open(SPOTIFY, "Looped")
        val shown = watch()

        apps.load(spotify, "Says", "Nils Frahm", seconds = 528)

        assertThat(shown.content.title).isEqualTo("Says")
        assertThat(shown.content.artist).isEqualTo("Nils Frahm")
        assertThat(shown.content.duration).isEqualTo(528.seconds)
    }

    @Test
    fun `la mise en pause arrive toute seule`() = runTest(dispatcher) {
        val spotify = apps.open(SPOTIFY)
        val shown = watch()

        apps.say(spotify, PlaybackState.STATE_PAUSED)

        assertThat(shown.loaded.playing).isFalse()
    }

    @Test
    fun `une app ouverte pendant que l'accueil est affiché apparaît aussitôt`() = runTest(dispatcher) {
        val shown = watch()
        assertThat(shown.state).isEqualTo(MediaState.None)

        apps.open(SPOTIFY, "Looped")

        assertThat(shown.content.title).isEqualTo("Looped")
    }

    @Test
    fun `quand l'app ferme sa session, plus rien n'est chargé`() = runTest(dispatcher) {
        val spotify = apps.open(SPOTIFY)
        val shown = watch()

        apps.close(spotify)

        assertThat(shown.state).isEqualTo(MediaState.None)
        assertThat(apps.watchersOf(spotify)).isEqualTo(0)
    }

    // ---------- the remote ----------

    @Test
    fun `la télécommande a les boutons que l'app accepte`() = runTest(dispatcher) {
        val spotify = apps.open(SPOTIFY)
        val shown = watch()
        apps.say(spotify, PlaybackState.STATE_PLAYING)

        assertThat(shown.loaded.actions)
            .containsExactlyInAnyOrder(MediaAction.Previous, MediaAction.Next, MediaAction.Seek)
    }

    @Test
    fun `YouTube n'accepte pas Précédent, sa télécommande ne l'a pas`() = runTest(dispatcher) {
        val youtube = apps.open(YOUTUBE, "Tiny Desk Concert", "NPR Music")
        val shown = watch()

        apps.say(
            youtube,
            PlaybackState.STATE_PLAYING,
            actions =
                PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SEEK_TO
        )

        assertThat(shown.loaded.actions).containsExactlyInAnyOrder(MediaAction.Next, MediaAction.Seek)
    }

    @Test
    fun `les boutons vont à l'app montrée, pas aux autres`() = runTest(dispatcher) {
        apps.open(YOUTUBE, "Tiny Desk Concert", state = PlaybackState.STATE_PAUSED)
        apps.open(SPOTIFY, "Looped")
        watch()

        media.pause()
        media.play()
        media.next()
        media.previous()
        media.seekTo(61.seconds)

        assertThat(pressed).containsExactly(
            "$SPOTIFY: pause",
            "$SPOTIFY: play",
            "$SPOTIFY: next",
            "$SPOTIFY: previous",
            "$SPOTIFY: seek to 61000"
        )
    }

    @Test
    fun `sans rien de chargé, les boutons ne font rien`() = runTest(dispatcher) {
        watch()

        media.play()
        media.next()

        assertThat(pressed).isEmpty()
    }

    // ---------- where the content is ----------

    @Test
    fun `la position avance avec le morceau, seconde après seconde`() = runTest(dispatcher) {
        val spotify = apps.open(SPOTIFY)
        val shown = watch()
        apps.say(spotify, PlaybackState.STATE_PLAYING, positionSeconds = 72, at = currentTime)
        assertThat(shown.position).isEqualTo(72.seconds)

        advanceTimeBy(3.seconds)
        runCurrent()

        assertThat(shown.position).isEqualTo(75.seconds)
    }

    @Test
    fun `en pause, la position ne bouge pas et plus rien ne tourne`() = runTest(dispatcher) {
        val spotify = apps.open(SPOTIFY)
        val shown = watch()
        apps.say(spotify, PlaybackState.STATE_PAUSED, positionSeconds = 72, at = currentTime)

        advanceTimeBy(60.seconds)
        runCurrent()

        assertThat(shown.position).isEqualTo(72.seconds)
    }

    @Test
    fun `la position ne dépasse jamais la fin du morceau`() = runTest(dispatcher) {
        val spotify = apps.open(SPOTIFY)
        val shown = watch()
        apps.say(spotify, PlaybackState.STATE_PLAYING, positionSeconds = 240, at = currentTime)

        advanceTimeBy(30.seconds)
        runCurrent()

        assertThat(shown.position).isEqualTo(245.seconds)
    }

    @Test
    fun `sans rien de chargé, la position est au début`() = runTest(dispatcher) {
        assertThat(watch().position).isEqualTo(Duration.ZERO)
    }

    // ---------- the cover ----------

    @Test
    fun `la pochette est lue telle que l'app la donne`() = runTest(dispatcher) {
        apps.open(SPOTIFY, cover = cover())

        val picture = watch().content.cover

        assertThat(picture).isNotNull()
        assertThat(picture!!.width).isEqualTo(300)
        assertThat(picture.height).isEqualTo(300)
        assertThat(picture.pixels.first()).isEqualTo(BLUE)
        assertThat(picture.pixels.last()).isEqualTo(ORANGE)
    }

    @Test
    fun `la pochette donne ses deux couleurs, la plus présente d'abord`() = runTest(dispatcher) {
        apps.open(SPOTIFY, cover = cover())

        val colors = watch().content.colors

        assertThat(colors).isNotNull()
        assertThat(Color.blue(colors!!.first)).isGreaterThan(Color.red(colors.first))
        assertThat(Color.red(colors.second)).isGreaterThan(Color.blue(colors.second))
    }

    @Test
    fun `une pochette d'une seule couleur donne deux fois cette couleur`() = runTest(dispatcher) {
        val plain = Bitmap.createBitmap(IntArray(100 * 100) { BLUE }, 100, 100, Bitmap.Config.ARGB_8888)
        apps.open(SPOTIFY, cover = plain)

        val colors = watch().content.colors

        assertThat(colors).isNotNull()
        assertThat(colors!!.second).isEqualTo(colors.first)
    }

    @Test
    fun `une pochette toute noire ne donne aucune couleur, Brume garde alors celles du moment`() = runTest(dispatcher) {
        val black = Bitmap.createBitmap(IntArray(100 * 100) { Color.BLACK }, 100, 100, Bitmap.Config.ARGB_8888)
        apps.open(SPOTIFY, cover = black)

        val shown = watch()

        assertThat(shown.content.cover).isNotNull()
        assertThat(shown.content.colors).isNull()
    }

    @Test
    fun `sans pochette, il n'y a ni image ni couleurs`() = runTest(dispatcher) {
        apps.open(SPOTIFY)

        val shown = watch()

        assertThat(shown.content.cover).isNull()
        assertThat(shown.content.colors).isNull()
    }

    @Test
    fun `une grande vignette est réduite avant d'être gardée`() = runTest(dispatcher) {
        apps.open(YOUTUBE, "Tiny Desk Concert", cover = cover(width = 1280, height = 720))

        val picture = watch().content.cover

        assertThat(picture!!.width).isEqualTo(512)
        assertThat(picture.height).isEqualTo(288)
    }

    @Test
    fun `la pochette arrive parfois après le titre, et s'affiche alors`() = runTest(dispatcher) {
        val spotify = apps.open(SPOTIFY, "Looped")
        val shown = watch()
        assertThat(shown.content.cover).isNull()

        apps.load(spotify, "Looped", cover = cover())

        assertThat(shown.content.cover).isNotNull()
    }

    @Test
    fun `une mise en pause ne relit pas la pochette`() = runTest(dispatcher) {
        val spotify = apps.open(SPOTIFY, cover = cover())
        val shown = watch()
        val before = shown.content

        apps.say(spotify, PlaybackState.STATE_PAUSED)

        assertThat(shown.content.cover).isEqualTo(before.cover)
        assertThat(shown.content).isEqualTo(before)
    }

    // ---------- the way to the app ----------

    @Test
    fun `Ouvrir dans l'app montre le contenu là où l'app le propose`() = runTest(dispatcher) {
        val spotify = apps.open(SPOTIFY)
        val player = Intent("now playing").setPackage(SPOTIFY)
        shadowOf(spotify).setSessionActivity(PendingIntent.getActivity(phone, 0, player, PendingIntent.FLAG_IMMUTABLE))
        watch()

        media.openApp()

        assertThat(shadowOf(phone).nextStartedActivity.action).isEqualTo("now playing")
    }

    @Test
    fun `si l'app ne propose pas de chemin vers ce qui joue, elle s'ouvre comme par son icône`() = runTest(dispatcher) {
        val icon = ComponentName(SPOTIFY, "$SPOTIFY.MainActivity")
        shadowOf(phone.packageManager).addActivityIfNotPresent(icon)
        shadowOf(phone.packageManager).addIntentFilterForActivity(
            icon,
            IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        )
        apps.open(SPOTIFY)
        watch()

        media.openApp()

        val opened = shadowOf(phone).nextStartedActivity
        assertThat(opened.component).isEqualTo(icon)
        assertThat(opened.categories.orEmpty()).contains(Intent.CATEGORY_LAUNCHER)
    }

    @Test
    fun `sans rien de chargé, il n'y a pas d'app à ouvrir`() = runTest(dispatcher) {
        watch()

        media.openApp()

        assertThat(shadowOf(phone).nextStartedActivity).isNull()
    }

    // ---------- nothing without the user's agreement, nothing while nobody looks ----------

    @Test
    fun `sans l'accès aux notifications, rien n'est chargé et aucune session n'est suivie`() = runTest(dispatcher) {
        access.refuse(Access.Notifications)
        val spotify = apps.open(SPOTIFY)

        val shown = watch()

        assertThat(shown.state).isEqualTo(MediaState.None)
        assertThat(apps.watchersOf(spotify)).isEqualTo(0)
    }

    @Test
    fun `dès que l'accès est donné, ce qui joue apparaît`() = runTest(dispatcher) {
        access.refuse(Access.Notifications)
        apps.open(SPOTIFY, "Looped")
        val shown = watch()

        access.grant(Access.Notifications)

        assertThat(shown.content.title).isEqualTo("Looped")
    }

    @Test
    fun `si l'accès est repris, plus rien n'est chargé ni suivi`() = runTest(dispatcher) {
        val spotify = apps.open(SPOTIFY)
        val shown = watch()

        access.refuse(Access.Notifications)

        assertThat(shown.state).isEqualTo(MediaState.None)
        assertThat(apps.watchersOf(spotify)).isEqualTo(0)
    }

    @Test
    fun `tant que l'accueil est affiché, chaque session est suivie une seule fois`() = runTest(dispatcher) {
        val spotify = apps.open(SPOTIFY)
        val youtube = apps.open(YOUTUBE)

        watch()
        apps.bringFirst(spotify)

        assertThat(apps.watchersOf(spotify)).isEqualTo(1)
        assertThat(apps.watchersOf(youtube)).isEqualTo(1)
    }

    @Test
    fun `dès que l'accueil est caché, plus aucune session n'est suivie`() = runTest(dispatcher) {
        val spotify = apps.open(SPOTIFY)
        val shown = watch()
        assertThat(apps.watchersOf(spotify)).isEqualTo(1)

        shown.job.cancel()
        runCurrent()

        assertThat(apps.watchersOf(spotify)).isEqualTo(0)
        assertThat(apps.listeners).isEqualTo(0)
    }

    @Test
    fun `accueil caché, les boutons ne font plus rien`() = runTest(dispatcher) {
        apps.open(SPOTIFY)
        val shown = watch()
        shown.job.cancel()
        runCurrent()

        media.pause()

        assertThat(pressed).isEmpty()
    }

    private companion object {
        val BLUE = Color.rgb(32, 64, 192)
        val ORANGE = Color.rgb(232, 128, 32)
    }
}
