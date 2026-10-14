package io.github.bahuauddimitri.bercail.data.settings

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.domain.settings.IdleBackground
import io.github.bahuauddimitri.bercail.core.domain.settings.Settings
import io.github.bahuauddimitri.bercail.core.domain.weather.City
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The settings are kept in a file on the phone: they come back as they were left. */
class StoredSettingsTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val file by lazy { File(folder.root, "settings.preferences_pb") }

    /** Opens the settings file as the app does at start, runs [block], then closes it as when the app stops. */
    private fun <T> withSettings(block: suspend (StoredSettings) -> T): T = runBlocking {
        val job = Job()
        val settings = StoredSettings(settingsStore(file, CoroutineScope(Dispatchers.IO + job)))
        try {
            block(settings)
        } finally {
            job.cancelAndJoin()
        }
    }

    @Test
    fun `au premier lancement, les réglages ont leurs valeurs par défaut`() {
        val settings = withSettings { it.settings.first() }

        assertThat(settings).isEqualTo(Settings())
        assertThat(settings.idleBackground).isEqualTo(IdleBackground.Wallpaper)
        assertThat(settings.drawerExpandedAtStart).isEqualTo(false)
    }

    @Test
    fun `un réglage changé est lu changé`() {
        val settings = withSettings {
            it.update { current -> current.copy(idleBackground = IdleBackground.Brume) }
            it.settings.first()
        }

        assertThat(settings.idleBackground).isEqualTo(IdleBackground.Brume)
    }

    @Test
    fun `les réglages survivent à un redémarrage de l'app`() {
        withSettings {
            it.update { current ->
                current.copy(
                    favoriteApps = listOf("com.spotify.music/.Main", "com.beeper/.Home"),
                    favoritePeople = listOf("lea", "tom"),
                    drawerExpandedAtStart = true,
                    idleBackground = IdleBackground.Brume
                )
            }
        }

        val afterRestart = withSettings { it.settings.first() }

        assertThat(afterRestart.favoriteApps).containsExactly("com.spotify.music/.Main", "com.beeper/.Home")
        assertThat(afterRestart.favoritePeople).containsExactly("lea", "tom")
        assertThat(afterRestart.drawerExpandedAtStart).isTrue()
        assertThat(afterRestart.idleBackground).isEqualTo(IdleBackground.Brume)
    }

    @Test
    fun `la ville choisie pour la météo est gardée, puis oubliée quand on revient à la position du téléphone`() {
        val nantes = City("Nantes", "Pays de la Loire, France", 47.21725, -1.55336)
        withSettings { it.update { current -> current.copy(weatherCity = nantes) } }
        assertThat(withSettings { it.settings.first() }.weatherCity).isEqualTo(nantes)

        withSettings { it.update { current -> current.copy(weatherCity = null) } }

        assertThat(withSettings { it.settings.first() }.weatherCity).isNull()
    }

    @Test
    fun `sans choix, il n'y a pas d'app média principale, et celle qui est choisie est gardée`() {
        assertThat(withSettings { it.settings.first() }.mainMediaApp).isNull()

        withSettings { it.update { current -> current.copy(mainMediaApp = "com.google.android.youtube/.Home") } }

        assertThat(withSettings { it.settings.first() }.mainMediaApp).isEqualTo("com.google.android.youtube/.Home")
    }

    @Test
    fun `l'ordre des favoris est conservé`() {
        withSettings { it.update { current -> current.copy(favoriteApps = listOf("c", "a", "b")) } }

        assertThat(withSettings { it.settings.first() }.favoriteApps).containsExactly("c", "a", "b")
    }

    @Test
    fun `changer un réglage ne touche pas aux autres`() {
        val settings = withSettings {
            it.update { current -> current.copy(favoriteApps = listOf("a")) }
            it.update { current -> current.copy(drawerExpandedAtStart = true) }
            it.settings.first()
        }

        assertThat(settings.favoriteApps).containsExactly("a")
        assertThat(settings.drawerExpandedAtStart).isTrue()
    }

    @Test
    fun `une valeur inconnue dans le fichier laisse le réglage à sa valeur par défaut`() = runBlocking {
        val job = Job()
        val store = settingsStore(file, CoroutineScope(Dispatchers.IO + job))
        store.edit { it[stringPreferencesKey("idle_background")] = "Diagonal" }

        val settings = StoredSettings(store).settings.first()
        job.cancelAndJoin()

        assertThat(settings.idleBackground).isEqualTo(IdleBackground.Wallpaper)
    }

    @Test
    fun `la ligne couchée d'une version précédente est effacée du fichier au premier réglage changé`() = runBlocking {
        val job = Job()
        val store = settingsStore(file, CoroutineScope(Dispatchers.IO + job))
        store.edit { it[stringPreferencesKey("timeline")] = "Horizontal" }

        StoredSettings(store).update { it.copy(drawerExpandedAtStart = true) }
        val saved = store.data.first()
        job.cancelAndJoin()

        assertThat(saved[stringPreferencesKey("timeline")]).isNull()
    }
}
