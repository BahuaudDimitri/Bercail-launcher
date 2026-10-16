package io.github.bahuauddimitri.bercail

import android.content.ComponentName
import android.graphics.drawable.AdaptiveIconDrawable
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isNotEmpty
import io.github.bahuauddimitri.bercail.data.apps.DeviceApps
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A probe for the phone plugged in over USB: how each app will be drawn in the dock. It changes nothing; read the
 * answer with `adb logcat -s BercailGlyphs`.
 */
@RunWith(AndroidJUnit4::class)
class GlyphProbeDeviceTest {
    @Test
    fun `chaque app du téléphone dit comment son icône sera dessinée dans le dock`() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val apps = DeviceApps(context)

        val installed = apps.apps.first()
        installed.sortedBy { it.name }.forEach { app ->
            val component = checkNotNull(ComponentName.unflattenFromString(app.id))
            val icon = context.packageManager.getActivityIcon(component)
            val drawn = when {
                (icon as? AdaptiveIconDrawable)?.monochrome != null -> "monochrome"
                apps.glyph(app.id) != null -> "silhouette"
                else -> "initiale"
            }
            Log.i(TAG, "${app.name} · $drawn · ${app.id}")
        }

        assertThat(installed).isNotEmpty()
    }

    private companion object {
        const val TAG = "BercailGlyphs"
    }
}
