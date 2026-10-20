package io.github.bahuauddimitri.bercail.data.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.LruCache
import io.github.bahuauddimitri.bercail.core.domain.Picture
import io.github.bahuauddimitri.bercail.core.domain.apps.PackIcon

/**
 * The white glyphs of the apps, for the dock and the lists, and those of the drawings of the icon packs, for the
 * icon picker. Each is drawn once per version of the app and of the pack, and only the last ones seen are kept.
 */
internal class Glyphs(private val context: Context, private val packs: IconPacks) {
    private val packages = context.packageManager

    /**
     * The [drawing] chosen for the app while its pack still has it; otherwise the drawing the [iconPack] gives the
     * app; otherwise the monochrome layer Android 13 asks apps for, framed as the icon is; without it, the real icon
     * in one colour. A pack that is gone draws nothing: the app falls back without a word.
     */
    fun of(appId: String, component: ComponentName, iconPack: String?, drawing: PackIcon?): Picture? {
        val packUpdated = iconPack?.let(packs::updated)
        val pack = iconPack?.takeIf { packUpdated != null }
        val drawingUpdated = drawing?.let { packs.updated(it.pack) }
        val chosen = drawing?.takeIf { drawingUpdated != null }
        val stamp = Stamp(updatedOf(component.packageName), pack, packUpdated, chosen, drawingUpdated)
        glyphs.get(appId)?.takeIf { it.stamp == stamp }?.let { return it.glyph }
        val glyph = chosen?.let(packs::drawing)?.packGlyph(context, DOCK_GLYPH_DP)
            ?: fromPack(pack, packUpdated, component)?.packGlyph(context, DOCK_GLYPH_DP)
            ?: ownIcon(component)?.glyph(context, DOCK_GLYPH_DP)
        glyphs.put(appId, Drawn(stamp, glyph))
        return glyph
    }

    /** One drawing of a pack, small, for the icon picker; nothing when the pack is gone or the drawing empty. */
    fun ofPackIcon(icon: PackIcon): Picture? = packs.updated(icon.pack)?.let { updated ->
        val stamp = Stamp(updated, icon.pack, updated, icon, updated)
        val kept = packIcons.get(icon)?.takeIf { it.stamp == stamp }
        if (kept != null) {
            kept.glyph
        } else {
            packs.drawing(icon)?.packGlyph(context, PICKER_GLYPH_DP).also { packIcons.put(icon, Drawn(stamp, it)) }
        }
    }

    private fun fromPack(pack: String?, updated: Long?, component: ComponentName): Drawable? =
        if (pack != null && updated != null) packs.drawingOf(pack, updated, component) else null

    private fun updatedOf(packageName: String): Long = try {
        packages.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).lastUpdateTime
    } catch (_: PackageManager.NameNotFoundException) {
        NEVER_UPDATED
    }

    private fun ownIcon(component: ComponentName): Drawable? = try {
        packages.getActivityIcon(component)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    /** The versions a glyph was drawn for: the app's, the dock pack's and the chosen drawing's, when they drew it. */
    private data class Stamp(
        val app: Long,
        val pack: String?,
        val packUpdated: Long?,
        val drawing: PackIcon?,
        val drawingUpdated: Long?
    )

    /** A glyph drawn for one version of an app or a pack; nothing when it has none that reads. */
    private class Drawn(val stamp: Stamp, val glyph: Picture?)

    /**
     * The glyphs drawn lately: only the [GLYPHS_KEPT] apps seen last, about 5.5 MB, so that a long list of apps does
     * not fill the memory. An app let go is drawn again when it shows.
     */
    private val glyphs = LruCache<String, Drawn>(GLYPHS_KEPT)

    /** The drawings of the picker seen last, about 4 MB: those scrolled away are drawn again when they come back. */
    private val packIcons = LruCache<PackIcon, Drawn>(PACK_ICONS_KEPT)

    private companion object {
        /** A package Android tells nothing about: its glyph is drawn once and kept. */
        const val NEVER_UPDATED = -1L

        /** How many glyphs are kept: enough for the dock, the media sheet and a screen of the search. */
        const val GLYPHS_KEPT = 64

        /** How many drawings of the picker are kept: a little more than a screen of its grid. */
        const val PACK_ICONS_KEPT = 96
    }
}

/**
 * The glyph of an app is drawn at the size the dock shows it: a 56 dp frame (`BcDock`), its drawing, the middle
 * half, at about 28 dp. Drawn smaller, it would be enlarged on screen, and blur.
 */
private const val DOCK_GLYPH_DP = 56

/** A drawing of the picker is drawn at the size its grid shows it (`BcIconGrid`): a 40 dp frame, about 44 KB. */
private const val PICKER_GLYPH_DP = 40
