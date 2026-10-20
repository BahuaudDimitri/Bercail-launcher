package io.github.bahuauddimitri.bercail.data.apps

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.util.Xml
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isSameInstanceAs
import io.github.bahuauddimitri.bercail.core.domain.Picture
import io.github.bahuauddimitri.bercail.core.domain.apps.IconPack
import java.io.StringReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

/** The icon packs of the dock: their `appfilter`, their list, and the drawings they give the apps. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IconPacksUiTest {
    private val phone = ApplicationProvider.getApplicationContext<Application>()
    private val files = PackFiles()
    private val apps = DeviceApps(phone, Dispatchers.Unconfined, files)
    private val chrome = ComponentName("com.android.chrome", "com.android.chrome.Main")
    private val chromeId = "com.android.chrome/.Main"
    private val camera = ComponentName("com.google.android.GoogleCamera", "com.android.camera.CameraLauncher")
    private val lines = "com.natewren.linesfree"

    private val appFilter = """
        <resources>
            <iconback img1="iconback" />
            <scale factor="0.75" />
            <item component=":BROWSER" drawable="browser" />
            <item component="ComponentInfo{com.android.chrome/com.android.chrome.Main}" drawable="chrome" />
            <item component="ComponentInfo{com.android.chrome/com.android.chrome.Main}" drawable="chrome2" />
            <item component="ComponentInfo{com.spotify.music/com.spotify.music.MainActivity}" drawable="spotify" />
        </resources>
    """.trimIndent()

    private val parsed = mapOf(
        chrome to "chrome",
        ComponentName("com.spotify.music", "com.spotify.music.MainActivity") to "spotify"
    )

    /** An app with its own icon: a monochrome layer drawn over 20 % to 80 % of the icon. */
    private fun installApp(component: ComponentName) {
        installPackage(component.packageName, updated = 1L)
        shadowOf(phone.packageManager).addActivityIfNotPresent(component)
        val layer = InsetDrawable(ColorDrawable(Color.BLACK), LAYER_INSET)
        val icon = AdaptiveIconDrawable(ColorDrawable(Color.BLUE), null, layer)
        shadowOf(phone.packageManager).addActivityIcon(component, icon)
    }

    private fun installPackage(packageName: String, updated: Long) {
        shadowOf(phone.packageManager).installPackage(
            PackageInfo().apply {
                this.packageName = packageName
                lastUpdateTime = updated
                applicationInfo = ApplicationInfo().apply { this.packageName = packageName }
            }
        )
    }

    /** An icon pack that draws Chrome as a white square over the whole drawing. */
    @Suppress("DEPRECATION") // The newer helper needs a full fake package; one entry answering the pack intent will do.
    private fun installPack(packageName: String = lines, name: String = "Lines Free", updated: Long = 1L) {
        installPackage(packageName, updated)
        val entry = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                this.packageName = packageName
                this.name = "$packageName.Themes"
                applicationInfo = ApplicationInfo().apply {
                    this.packageName = packageName
                    nonLocalizedLabel = name
                }
            }
        }
        shadowOf(phone.packageManager).addResolveInfoForIntent(Intent(ICON_PACK_ACTION), entry)
        files.filters[packageName] = mapOf(chrome to "chrome")
        files.drawings["$packageName/chrome"] = ColorDrawable(Color.WHITE)
    }

    private fun Picture.at(x: Float, y: Float): Int = pixels[(y * height).toInt() * width + (x * width).toInt()]

    @Test
    fun `l'appfilter compilé dans les ressources du pack donne le dessin de chaque app`() {
        val compiled = Xml.newPullParser().apply { setInput(StringReader(appFilter)) }

        assertThat(readAppFilter(compiled) { null }).isEqualTo(parsed)
    }

    @Test
    fun `l'appfilter en fichier dans les assets du pack est lu quand ses ressources n'en ont pas`() {
        assertThat(readAppFilter(null) { appFilter.byteInputStream() }).isEqualTo(parsed)
    }

    @Test
    fun `un appfilter illisible ou absent ne dessine aucune app`() {
        assertThat(readAppFilter(null) { "<resources><item".byteInputStream() }).isEmpty()
        assertThat(readAppFilter(null) { null }).isEmpty()
    }

    @Test
    fun `les packs d'icônes installés sont listés par le nom de leur app, de A à Z`() = runBlocking {
        installPack(lines, "Lines Free")
        installPack("com.whicons.iconpack", "Whicons")
        installPack("com.arcticons.dark", "Arcticons")

        assertThat(apps.iconPacks.first().map { it.name }).containsExactly("Arcticons", "Lines Free", "Whicons")
        assertThat(apps.iconPacks.first().first()).isEqualTo(IconPack("com.arcticons.dark", "Arcticons"))
    }

    @Test
    fun `une app que le pack dessine prend son dessin, en blanc, au cadre du dock`() = runBlocking {
        installApp(chrome)
        installPack()

        val glyph = apps.glyph(chromeId, lines)

        // The pack's drawing covers the middle three quarters of the frame; the app's own, 20 % to 80 % of it.
        assertThat(glyph).isNotNull()
        assertThat(glyph!!.at(0.15f, 0.5f)).isEqualTo(Color.WHITE)
        assertThat(glyph.at(0.05f, 0.5f) ushr ALPHA_SHIFT).isEqualTo(0)
    }

    @Test
    fun `une app que le pack ne dessine pas garde son dessin d'une seule couleur`() = runBlocking {
        installApp(camera)
        installPack()

        val glyph = apps.glyph(camera.flattenToShortString(), lines)!!

        assertThat(glyph.at(0.5f, 0.5f)).isEqualTo(Color.WHITE)
        assertThat(glyph.at(0.15f, 0.5f) ushr ALPHA_SHIFT).isEqualTo(0)
    }

    @Test
    fun `avec Aucun, le dessin de chaque app ne change pas`() = runBlocking {
        installApp(chrome)
        installPack()

        val glyph = apps.glyph(chromeId, iconPack = null)!!

        assertThat(glyph.at(0.15f, 0.5f) ushr ALPHA_SHIFT).isEqualTo(0)
        assertThat(files.filterReads).isEqualTo(0)
    }

    @Test
    fun `un pack désinstallé laisse sans rien dire chaque app à son dessin`() = runBlocking {
        installApp(chrome)
        installPack()
        apps.glyph(chromeId, lines)
        shadowOf(phone.packageManager).removePackage(lines)

        val glyph = apps.glyph(chromeId, lines)!!

        assertThat(glyph.at(0.15f, 0.5f) ushr ALPHA_SHIFT).isEqualTo(0)
        assertThat(glyph.at(0.5f, 0.5f)).isEqualTo(Color.WHITE)
    }

    @Test
    fun `l'appfilter d'un pack n'est lu qu'une fois, puis de nouveau quand le pack est mis à jour`() = runBlocking {
        installApp(chrome)
        installApp(camera)
        installPack(updated = 1L)
        val first = apps.glyph(chromeId, lines)
        apps.glyph(camera.flattenToShortString(), lines)

        assertThat(files.filterReads).isEqualTo(1)
        assertThat(apps.glyph(chromeId, lines)).isSameInstanceAs(first)

        installPack(updated = 2L)
        apps.glyph(chromeId, lines)

        assertThat(files.filterReads).isEqualTo(2)
    }

    /** The files of the packs, as the tests give them; it counts how often an `appfilter` is read. */
    private class PackFiles : IconPackFiles {
        val filters = mutableMapOf<String, Map<ComponentName, String>>()
        val drawings = mutableMapOf<String, Drawable>()
        var filterReads = 0

        override fun appFilter(pack: String): Map<ComponentName, String>? {
            filterReads++
            return filters[pack]
        }

        override fun drawing(pack: String, name: String): Drawable? = drawings["$pack/$name"]
    }

    private companion object {
        const val ALPHA_SHIFT = 24

        /** The monochrome layer of the apps leaves 30 % of the layer empty on each side. */
        const val LAYER_INSET = 0.3f
    }
}
