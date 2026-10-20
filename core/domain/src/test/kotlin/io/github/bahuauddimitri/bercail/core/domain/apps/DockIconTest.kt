package io.github.bahuauddimitri.bercail.core.domain.apps

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.github.bahuauddimitri.bercail.core.domain.settings.Settings
import org.junit.Test

/** The icon of each app of the dock: the choices the picker offers, and what the dock asks for. */
class DockIconTest {
    private val lines = IconPack(id = "com.natewren.linesfree", name = "Lines Free")
    private val whicons = IconPack(id = "com.whicons.iconpack", name = "Whicons")
    private val music = App(id = "com.spotify.music/.MainActivity", name = "Spotify")

    private val linesIcons = PackCatalog(
        pack = lines,
        icons = listOf("spotify_alt", "musique_note", "mail", "Gmail", "spotify", "écouteurs"),
        apps = mapOf(music.id to "music_player")
    )
    private val whiconsIcons = PackCatalog(pack = whicons, icons = listOf("mail_box", "spotify"), apps = emptyMap())

    private fun PackIcons.names() = icons.map { it.name }

    @Test
    fun `le nom d'une icône se lit avec des espaces à la place des tirets bas`() {
        assertThat(PackIcon(lines.id, "google_maps_2").label).isEqualTo("google maps 2")
    }

    @Test
    fun `toutes les icônes se rangent par pack, chaque pack de A à Z`() {
        val choices = iconChoices(music, listOf(linesIcons, whiconsIcons), query = "")

        assertThat(choices.packs.map { it.pack }).containsExactly(lines, whicons)
        assertThat(choices.packs.first().names())
            .containsExactly("écouteurs", "Gmail", "mail", "musique_note", "spotify", "spotify_alt")
    }

    @Test
    fun `chercher ignore les majuscules et les accents`() {
        val choices = iconChoices(music, listOf(linesIcons, whiconsIcons), query = "  ECOUTEURS ")

        assertThat(choices.packs.single().names()).containsExactly("écouteurs")
        assertThat(iconChoices(music, listOf(linesIcons), "Musique").packs.single().names())
            .containsExactly("musique_note")
    }

    @Test
    fun `chercher « mail » trouve les icônes de chaque pack qui le contiennent`() {
        val choices = iconChoices(music, listOf(linesIcons, whiconsIcons), query = "mail")

        assertThat(choices.packs.map { it.names() }).containsExactly(listOf("Gmail", "mail"), listOf("mail_box"))
    }

    @Test
    fun `un nom que nulle icône ne porte ne laisse aucun pack`() {
        assertThat(iconChoices(music, listOf(linesIcons), query = "zèbre").packs).isEmpty()
    }

    @Test
    fun `les icônes proposées viennent d'abord de l'appfilter, puis du nom de l'app`() {
        val choices = iconChoices(music, listOf(linesIcons, whiconsIcons), query = "")

        assertThat(choices.proposed).containsExactly(
            PackIcon(lines.id, "music_player"),
            PackIcon(lines.id, "spotify"),
            PackIcon(lines.id, "spotify_alt"),
            PackIcon(whicons.id, "spotify")
        )
    }

    @Test
    fun `les icônes proposées suivent aussi la recherche`() {
        val choices = iconChoices(music, listOf(linesIcons), query = "alt")

        assertThat(choices.proposed).containsExactly(PackIcon(lines.id, "spotify_alt"))
    }

    @Test
    fun `une app sans choix garde l'icône du pack`() {
        assertThat(Settings().dockIconOf(music.id)).isEqualTo(DockIcon.FromPack)
    }

    @Test
    fun `choisir une icône la garde pour cette app seule, l'icône du pack efface le choix`() {
        val drawing = DockIcon.Drawing(PackIcon(lines.id, "spotify_alt"))

        val chosen = Settings().withDockIcon(music.id, drawing).withDockIcon("chrome", DockIcon.Origin)

        assertThat(chosen.dockIconOf(music.id)).isEqualTo(drawing)
        assertThat(chosen.dockIconOf("chrome")).isEqualTo(DockIcon.Origin)
        assertThat(chosen.withDockIcon(music.id, DockIcon.FromPack).dockIcons.keys).containsOnly("chrome")
    }

    @Test
    fun `le dessin d'origine ne demande aucun pack, l'icône du pack demande celui du dock`() {
        assertThat(DockIcon.Origin.packFor(lines.id)).isNull()
        assertThat(DockIcon.FromPack.packFor(lines.id)).isEqualTo(lines.id)
        assertThat(DockIcon.FromPack.drawing).isNull()
    }

    @Test
    fun `une icône choisie demande son dessin, et le pack du dock au cas où il manquerait`() {
        val icon = PackIcon(whicons.id, "spotify")

        assertThat(DockIcon.Drawing(icon).drawing).isEqualTo(icon)
        assertThat(DockIcon.Drawing(icon).packFor(lines.id)).isEqualTo(lines.id)
    }
}
