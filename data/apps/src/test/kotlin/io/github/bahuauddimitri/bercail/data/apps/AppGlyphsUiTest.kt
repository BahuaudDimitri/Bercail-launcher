package io.github.bahuauddimitri.bercail.data.apps

import android.app.Application
import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import io.github.bahuauddimitri.bercail.core.domain.Picture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

/** The icons of the apps as one-colour glyphs, for the dock: the monochrome layer, or the real icon in one colour. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppGlyphsUiTest {
    private val phone = ApplicationProvider.getApplicationContext<Application>()
    private val apps = DeviceApps(phone, background = Dispatchers.Unconfined)
    private val chrome = ComponentName("com.android.chrome", "com.android.chrome.Main")
    private val id = "com.android.chrome/.Main"

    private fun iconOfChrome(icon: Drawable, updated: Long = 1L) {
        shadowOf(phone.packageManager).installPackage(
            PackageInfo().apply {
                packageName = chrome.packageName
                lastUpdateTime = updated
                applicationInfo = ApplicationInfo().apply { packageName = chrome.packageName }
            }
        )
        shadowOf(phone.packageManager).addActivityIfNotPresent(chrome)
        shadowOf(phone.packageManager).addActivityIcon(chrome, icon)
    }

    /** A drawing in the middle of a layer: [inset] of the layer left empty on each side. */
    private fun middle(color: Int, inset: Float) = InsetDrawable(ColorDrawable(color), inset)

    private fun Picture.at(x: Float, y: Float): Int = pixels[(y * height).toInt() * width + (x * width).toInt()]

    @Test
    fun `une icône avec un calque monochrome est rendue en dessin blanc d'une seule couleur`() = runBlocking {
        iconOfChrome(AdaptiveIconDrawable(ColorDrawable(Color.BLUE), null, middle(Color.BLACK, 0.3f)))

        val glyph = apps.glyph(id)

        assertThat(glyph).isNotNull()
        assertThat(glyph!!.width).isEqualTo(glyph.height)
        assertThat(glyph.at(0.5f, 0.5f)).isEqualTo(Color.WHITE)
        assertThat(glyph.at(0.02f, 0.02f) ushr ALPHA_SHIFT).isEqualTo(0)
    }

    @Test
    fun `le dessin monochrome est cadré comme l'icône, sur la partie visible du calque`() = runBlocking {
        // The layer is half as large again as the icon: a drawing over the middle 40 % of the layer spans 60 % of
        // the icon, from 20 % to 80 % of its width.
        iconOfChrome(AdaptiveIconDrawable(ColorDrawable(Color.BLUE), null, middle(Color.BLACK, 0.3f)))

        val glyph = apps.glyph(id)!!

        assertThat(glyph.at(0.25f, 0.5f)).isEqualTo(Color.WHITE)
        assertThat(glyph.at(0.15f, 0.5f) ushr ALPHA_SHIFT).isEqualTo(0)
    }

    @Test
    fun `une icône sans calque monochrome est sa vraie icône en une seule couleur, son fond effacé`() = runBlocking {
        iconOfChrome(AdaptiveIconDrawable(ColorDrawable(Color.BLUE), middle(Color.YELLOW, 0.35f)))

        val glyph = apps.glyph(id)

        assertThat(glyph).isNotNull()
        assertThat(glyph!!.at(0.5f, 0.5f)).isEqualTo(Color.WHITE)
        assertThat(glyph.at(0.15f, 0.5f) ushr ALPHA_SHIFT).isEqualTo(0)
    }

    @Test
    fun `une icône dont le premier plan couvre tout se lit encore par ce qui se détache de lui`() = runBlocking {
        val foreground = LayerDrawable(arrayOf(ColorDrawable(Color.YELLOW), middle(Color.BLACK, 0.35f)))
        iconOfChrome(AdaptiveIconDrawable(ColorDrawable(Color.BLUE), foreground))

        val glyph = apps.glyph(id)

        assertThat(glyph).isNotNull()
        assertThat(glyph!!.at(0.5f, 0.5f)).isEqualTo(Color.WHITE)
        assertThat(glyph.at(0.15f, 0.5f) ushr ALPHA_SHIFT).isEqualTo(0)
    }

    @Test
    fun `une icône d'ancien format dessinée sur un fond transparent garde sa forme`() = runBlocking {
        iconOfChrome(middle(Color.RED, 0.3f))

        val glyph = apps.glyph(id)

        assertThat(glyph).isNotNull()
        assertThat(glyph!!.at(0.5f, 0.5f)).isEqualTo(Color.WHITE)
        assertThat(glyph.at(0.1f, 0.5f) ushr ALPHA_SHIFT).isEqualTo(0)
    }

    @Test
    fun `une icône unie, où rien ne se détache, ne donne pas de dessin`() = runBlocking {
        iconOfChrome(ColorDrawable(Color.GREEN))

        assertThat(apps.glyph(id)).isNull()
    }

    @Test
    fun `une app disparue n'a pas de dessin`() = runBlocking {
        assertThat(apps.glyph("com.gone/.Main")).isNull()
        assertThat(apps.glyph("n'importe quoi")).isNull()
    }

    @Test
    fun `le dessin d'une app n'est rendu qu'une fois tant qu'elle n'est pas mise à jour`() = runBlocking {
        iconOfChrome(AdaptiveIconDrawable(ColorDrawable(Color.BLUE), null, middle(Color.BLACK, 0.3f)))
        val first = apps.glyph(id)

        assertThat(apps.glyph(id)).isSameInstanceAs(first)

        iconOfChrome(AdaptiveIconDrawable(ColorDrawable(Color.BLUE), null, middle(Color.BLACK, 0.2f)), updated = 2L)

        assertThat(apps.glyph(id)).isNotSameInstanceAs(first)
    }

    private companion object {
        const val ALPHA_SHIFT = 24
    }
}
