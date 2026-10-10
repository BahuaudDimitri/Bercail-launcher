package io.github.bahuauddimitri.bercail.core.domain.screen

import io.github.bahuauddimitri.bercail.core.domain.media.CoverColors
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import io.github.bahuauddimitri.bercail.core.domain.settings.IdleBackground

/** The two screens of the home: Accueil, and Écoute while a content is loaded. */
enum class HomePage {
    Home,
    Listen;

    /** The page really shown when this one is wanted: Écoute only exists while something is paused or playing. */
    fun allowedWith(media: MediaState): HomePage = if (media.isLoaded) this else Home
}

/** What is behind the home screen. */
sealed interface HomeBackground {
    /** The user's own wallpaper, drawn by the system. */
    data object Wallpaper : HomeBackground

    /** Brume, in the colors of the [cover], or in the colors of the moment without one. */
    data class Brume(val cover: CoverColors?) : HomeBackground
}

/** Brume while a content is loaded; otherwise the wallpaper, or Brume of the moment if the setting asks for it. */
fun backgroundFor(media: MediaState, idle: IdleBackground): HomeBackground = when {
    media is MediaState.Loaded -> HomeBackground.Brume(media.content.colors)
    idle == IdleBackground.Brume -> HomeBackground.Brume(cover = null)
    else -> HomeBackground.Wallpaper
}
