package io.github.bahuauddimitri.bercail.data.apps

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Icon
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import io.github.bahuauddimitri.bercail.core.domain.Picture
import io.github.bahuauddimitri.bercail.core.domain.apps.AppShortcut
import io.github.bahuauddimitri.bercail.core.domain.apps.AppShortcut.Kind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The shortcuts of an app, read on a long press in the dock, and opened from its menu. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(shadows = [StartedShortcuts::class])
class AppShortcutsUiTest {
    private val phone = ApplicationProvider.getApplicationContext<Application>()
    private val launcher = checkNotNull(phone.getSystemService(LauncherApps::class.java))
    private val apps = DeviceApps(phone, background = Dispatchers.Unconfined)
    private val chrome = ComponentName("com.android.chrome", "com.android.chrome.Main")
    private val id = "com.android.chrome/.Main"

    @Before
    fun bercailIsTheHomeApp() {
        StartedShortcuts.started.clear()
        shadowOf(launcher).setHasShortcutHostPermission(true)
    }

    /** Chrome offers a shortcut, as Android gives it to the home app: [flags] say where it comes from. */
    private fun offer(shortcut: String, label: String, flags: Int = DYNAMIC, rank: Int = 0, icon: Icon? = null) {
        val info = ShortcutInfo.Builder(phone, shortcut)
            .setShortLabel(label)
            .setActivity(chrome)
            .setIntent(Intent(Intent.ACTION_VIEW))
            .setRank(rank)
            .apply { icon?.let(::setIcon) }
            .build()
        // The builder takes the package of the app that builds it: here, the shortcut is Chrome's.
        ShortcutInfo::class.java.getDeclaredField("mPackageName").apply {
            isAccessible = true
        }.set(info, chrome.packageName)
        ShortcutInfo::class.java.getMethod("addFlags", Int::class.javaPrimitiveType).invoke(info, flags)
        shadowOf(launcher).addDynamicShortcut(info)
    }

    /** A square [outer] picture with a [inner] square in its middle half. */
    private fun bitmap(outer: Int, inner: Int): Bitmap =
        Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888).apply {
            eraseColor(outer)
            for (x in SIZE / 4 until SIZE * 3 / 4) for (y in SIZE / 4 until SIZE * 3 / 4) setPixel(x, y, inner)
        }

    private fun Picture.at(x: Float, y: Float): Int = pixels[(y * height).toInt() * width + (x * width).toInt()]

    @Test
    fun `Bercail lit les raccourcis d'une app, avec leur nom et leur sorte`() = runBlocking {
        offer("new_tab", "Nouvel onglet", flags = MANIFEST)
        offer("incognito", "Navigation privée", flags = DYNAMIC, rank = 1)
        offer("lemonde", "Le Monde", flags = PINNED)

        val shortcuts = apps.shortcuts(id)

        assertThat(shortcuts).containsExactlyInAnyOrder(
            AppShortcut(id, "new_tab", "Nouvel onglet", Kind.Manifest),
            AppShortcut(id, "incognito", "Navigation privée", Kind.Dynamic, rank = 1),
            AppShortcut(id, "lemonde", "Le Monde", Kind.Pinned)
        )
    }

    @Test
    fun `sans être l'écran d'accueil par défaut, Bercail ne lit aucun raccourci`() = runBlocking {
        offer("new_tab", "Nouvel onglet", flags = MANIFEST)
        shadowOf(launcher).setHasShortcutHostPermission(false)

        assertThat(apps.shortcuts(id)).isEmpty()
    }

    @Test
    fun `un raccourci que l'app a désactivé ne se lit pas`() = runBlocking {
        offer("new_tab", "Nouvel onglet", flags = MANIFEST)
        offer("old", "Ancien", flags = DYNAMIC or DISABLED)

        assertThat(apps.shortcuts(id).map { it.id }).containsExactly("new_tab")
    }

    @Test
    fun `une app disparue ou mal nommée n'a pas de raccourci`() = runBlocking {
        assertThat(apps.shortcuts("com.gone/.Main")).isEmpty()
        assertThat(apps.shortcuts("n'importe quoi")).isEmpty()
    }

    @Test
    fun `l'icône d'un raccourci est dessinée en une seule couleur, comme celles du dock`() = runBlocking {
        offer("new_tab", "Nouvel onglet", icon = Icon.createWithBitmap(bitmap(Color.YELLOW, Color.BLACK)))

        val glyph = apps.shortcutGlyph(apps.shortcuts(id).single())

        assertThat(glyph).isNotNull()
        assertThat(glyph!!.at(0.5f, 0.5f)).isEqualTo(Color.WHITE)
        assertThat(glyph.at(0.1f, 0.5f) ushr ALPHA_SHIFT).isEqualTo(0)
    }

    @Test
    fun `un raccourci sans icône n'a pas de dessin`() = runBlocking {
        offer("new_tab", "Nouvel onglet")

        assertThat(apps.shortcutGlyph(apps.shortcuts(id).single())).isNull()
    }

    @Test
    fun `toucher un raccourci l'ouvre dans son app`() = runBlocking {
        offer("new_tab", "Nouvel onglet", flags = MANIFEST)

        apps.openShortcut(apps.shortcuts(id).single())

        assertThat(StartedShortcuts.started).containsExactly("com.android.chrome/new_tab")
    }

    private companion object {
        // The flags Android gives a shortcut (ShortcutInfo.FLAG_…, hidden from apps).
        const val DYNAMIC = 1
        const val PINNED = 2
        const val MANIFEST = 32
        const val DISABLED = 64
        const val SIZE = 48
        const val ALPHA_SHIFT = 24
    }
}
