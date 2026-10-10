package io.github.bahuauddimitri.bercail.feature.search

import android.graphics.Bitmap
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.bahuauddimitri.bercail.core.domain.apps.AppsSource

/**
 * The icons of the apps as pictures ready to draw. Each one is asked for once, when its row first shows, then
 * kept; only the most recently used are kept, so that a long list of apps does not fill the memory.
 */
@Stable
class AppIcons(private val apps: AppsSource, private val capacity: Int = CAPACITY) {
    private val kept = object : LinkedHashMap<String, ImageBitmap?>(capacity, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap?>): Boolean = size > capacity
    }

    /** The icon if it was already drawn; nothing otherwise, or for an app without icon. */
    fun known(appId: String): ImageBitmap? = kept[appId]

    /** The icon of the app, drawn now if needed. An app without icon keeps its initial. */
    suspend fun of(appId: String): ImageBitmap? {
        if (kept.containsKey(appId)) return kept[appId]
        val icon = apps.icon(appId)?.let {
            Bitmap.createBitmap(it.pixels, it.width, it.height, Bitmap.Config.ARGB_8888).asImageBitmap()
        }
        kept[appId] = icon
        return icon
    }

    private companion object {
        const val CAPACITY = 120
        const val LOAD_FACTOR = 0.75f
    }
}
