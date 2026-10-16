package io.github.bahuauddimitri.bercail.core.domain.apps

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import io.github.bahuauddimitri.bercail.core.domain.apps.AppShortcut.Kind
import org.junit.Test

/** The shortcuts an app of the dock shows on a long press. */
class ShortcutsTest {
    private fun shortcut(id: String, kind: Kind = Kind.Dynamic, rank: Int = 0, label: String = id) =
        AppShortcut(appId = "chrome", id = id, label = label, kind = kind, rank = rank)

    @Test
    fun `les raccourcis déclarés viennent d'abord, puis ceux de l'usage, puis les épinglés`() {
        val offered = listOf(
            shortcut("épinglé", Kind.Pinned),
            shortcut("récent", Kind.Dynamic),
            shortcut("nouvel onglet", Kind.Manifest)
        )

        assertThat(shortcutsShown(offered).map { it.id }).containsExactly("nouvel onglet", "récent", "épinglé")
    }

    @Test
    fun `parmi les raccourcis d'une même sorte, l'app choisit l'ordre`() {
        val offered = listOf(shortcut("b", rank = 2), shortcut("a", rank = 0), shortcut("c", rank = 1))

        assertThat(shortcutsShown(offered).map { it.id }).containsExactly("a", "c", "b")
    }

    @Test
    fun `le menu montre quatre raccourcis au plus, comme le launcher du Pixel`() {
        val offered = (0 until 6).map { shortcut("r$it", rank = it) }

        assertThat(shortcutsShown(offered).map { it.id }).containsExactly("r0", "r1", "r2", "r3")
    }

    @Test
    fun `un raccourci donné deux fois ne s'affiche qu'une fois`() {
        val offered = listOf(shortcut("onglet", Kind.Manifest), shortcut("onglet", Kind.Pinned))

        assertThat(shortcutsShown(offered).map { it.id }).containsExactly("onglet")
    }

    @Test
    fun `un raccourci sans nom ne s'affiche pas`() {
        assertThat(shortcutsShown(listOf(shortcut("vide", label = " ")))).isEmpty()
    }

    @Test
    fun `une app sans raccourci n'a pas de menu`() {
        assertThat(shortcutsShown(emptyList())).isEmpty()
    }
}
