package io.github.bahuauddimitri.bercail.feature.search

import android.graphics.Bitmap
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.bahuauddimitri.bercail.core.domain.apps.AppsSource

/**
 * The icons of the apps in one colour (their glyphs), as pictures ready to draw in the ring of their row. Each one is
 * asked for once, when its row first shows, then kept; only the most recently used are kept, so that a long list of
 * apps does not fill the memory. The phone draws them away from the main thread, once per version of the app.
 */
@Stable
class AppIcons(private val apps: AppsSource, private val capacity: Int = CAPACITY) {
    private val kept = object : LinkedHashMap<String, ImageBitmap?>(capacity, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap?>): Boolean = size > capacity
    }

    /** The glyph if it was already drawn; nothing otherwise, or for an app without one. */
    fun known(appId: String): ImageBitmap? = kept[appId]

    /** The glyph of the app, drawn now if needed. An app without one keeps its initial. */
    suspend fun of(appId: String): ImageBitmap? {
        if (kept.containsKey(appId)) return kept[appId]
        val glyph = apps.glyph(appId)?.let {
            Bitmap.createBitmap(it.pixels, it.width, it.height, Bitmap.Config.ARGB_8888).asImageBitmap()
        }
        kept[appId] = glyph
        return glyph
    }

    private companion object {
        const val CAPACITY = 120
        const val LOAD_FACTOR = 0.75f
    }
}
