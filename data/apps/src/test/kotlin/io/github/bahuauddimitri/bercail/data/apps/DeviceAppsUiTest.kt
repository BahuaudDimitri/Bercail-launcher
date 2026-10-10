package io.github.bahuauddimitri.bercail.data.apps

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** The apps installed on the phone, as Android lists them, and the places the phone can be sent to. */
@RunWith(AndroidJUnit4::class)
class DeviceAppsUiTest {
    private val phone = ApplicationProvider.getApplicationContext<Application>()
    private val launchable = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    private val apps = DeviceApps(phone, background = Dispatchers.Unconfined)

    @Suppress("DEPRECATION") // The newer helper needs a full fake package; one launchable entry is enough here.
    private fun install(packageName: String, label: String) {
        val activity = ActivityInfo().apply {
            this.packageName = packageName
            name = "$packageName.Main"
            applicationInfo = ApplicationInfo().apply { this.packageName = packageName }
        }
        val entry = ResolveInfo().apply {
            activityInfo = activity
            nonLocalizedLabel = label
        }
        shadowOf(phone.packageManager).addResolveInfoForIntent(launchable, entry)
    }

    private fun androidAnnouncesNewApp(packageName: String) {
        shadowOf(phone.getSystemService(LauncherApps::class.java)).notifyPackageAdded(packageName)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `le téléphone liste les apps qu'on peut ouvrir, avec leur nom`() = runBlocking {
        install("com.spotify.music", "Spotify")
        install("com.beeper.android", "Beeper")

        val names = apps.apps.first().map { it.name }

        assertThat(names).containsExactly("Spotify", "Beeper")
    }

    @Test
    fun `chaque app est identifiée par son activité de lancement`() = runBlocking {
        install("com.spotify.music", "Spotify")

        assertThat(apps.apps.first().single().id).isEqualTo("com.spotify.music/.Main")
    }

    @Test
    fun `Bercail ne se liste pas lui-même`() = runBlocking {
        install(phone.packageName, "Bercail")
        install("com.spotify.music", "Spotify")

        assertThat(apps.apps.first().map { it.name }).containsExactly("Spotify")
    }

    @Test
    fun `une app installée pendant qu'on regarde la liste y apparaît`() {
        install("com.spotify.music", "Spotify")
        val seen = mutableListOf<List<String>>()
        val watching = CoroutineScope(Dispatchers.Unconfined)
        watching.launch { apps.apps.collect { list -> seen += list.map { it.name } } }
        shadowOf(Looper.getMainLooper()).idle()

        install("org.signal", "Signal")
        androidAnnouncesNewApp("org.signal")
        watching.cancel()

        assertThat(seen.last()).containsExactly("Spotify", "Signal")
    }

    @Test
    fun `ouvrir une app la lance`() {
        apps.open("com.spotify.music/.Main")

        val started = shadowOf(phone).nextStartedActivity

        assertThat(started.component).isEqualTo(ComponentName("com.spotify.music", "com.spotify.music.Main"))
        assertThat(started.action).isEqualTo(Intent.ACTION_MAIN)
    }

    @Test
    fun `un identifiant qui n'est pas une app ne lance rien`() {
        apps.open("n'importe quoi")

        assertThat(shadowOf(phone).nextStartedActivity).isNull()
    }

    @Test
    fun `l'icône d'une app est rendue en image carrée`() = runBlocking {
        val spotify = ComponentName("com.spotify.music", "com.spotify.music.Main")
        shadowOf(phone.packageManager).addActivityIfNotPresent(spotify)
        shadowOf(phone.packageManager).addActivityIcon(spotify, ColorDrawable(Color.GREEN))

        val icon = apps.icon("com.spotify.music/.Main")

        assertThat(icon).isNotNull()
        assertThat(icon!!.width).isEqualTo(icon.height)
        assertThat(icon.pixels.size).isEqualTo(icon.width * icon.height)
        assertThat(icon.pixels.first()).isEqualTo(Color.GREEN)
    }

    @Test
    fun `une app disparue n'a pas d'icône`() = runBlocking {
        assertThat(apps.icon("com.gone/.Main")).isNull()
        assertThat(apps.icon("n'importe quoi")).isNull()
    }

    @Test
    fun `chercher sur le web ouvre la recherche du téléphone avec ce qu'on a tapé`() {
        DevicePhone(phone).searchWeb("ramen maison")

        val started = shadowOf(phone).nextStartedActivity

        assertThat(started.action).isEqualTo(Intent.ACTION_WEB_SEARCH)
        assertThat(started.getStringExtra("query")).isEqualTo("ramen maison")
    }

    @Test
    fun `chercher sur le Play Store ouvre sa recherche avec ce qu'on a tapé`() {
        DevicePhone(phone).searchStore("ramen maison")

        val started = shadowOf(phone).nextStartedActivity

        assertThat(started.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(started.data.toString()).isEqualTo("market://search?q=ramen%20maison")
    }

    @Test
    fun `les réglages renvoient vers les Paramètres du téléphone`() {
        DevicePhone(phone).openSettings()
        assertThat(shadowOf(phone).nextStartedActivity.action).isEqualTo(Settings.ACTION_SETTINGS)

        DevicePhone(phone).openHomeScreenSettings()
        assertThat(shadowOf(phone).nextStartedActivity.action).isEqualTo(Settings.ACTION_HOME_SETTINGS)
    }
}
