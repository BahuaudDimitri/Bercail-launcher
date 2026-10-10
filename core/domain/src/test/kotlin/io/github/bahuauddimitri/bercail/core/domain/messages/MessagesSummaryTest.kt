package io.github.bahuauddimitri.bercail.core.domain.messages

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.Test

/** The messages line of the folded drawer, and the favorite tiles of the open drawer. */
class MessagesSummaryTest {
    private val lea = Conversation("lea", "Léa", "WhatsApp", unread = 1)
    private val tom = Conversation("tom", "Tom", "Signal", unread = 1)
    private val team = Conversation("team", "Équipe", "Slack")
    private val mum = Conversation("mum", "Maman", "SMS")
    private val julien = Conversation("julien", "Julien", "Instagram", unread = 2)

    @Test
    fun `le résumé compte les non-lus et nomme ceux qui ont écrit`() {
        val summary = messagesSummary(listOf(lea, tom, team))

        assertThat(summary.headline).isEqualTo("2 non lus")
        assertThat(summary.detail).isEqualTo("Léa, Tom")
    }

    @Test
    fun `un seul message s'écrit au singulier`() {
        val summary = messagesSummary(listOf(lea, team))

        assertThat(summary.headline).isEqualTo("1 non lu")
    }

    @Test
    fun `plusieurs messages d'une même personne ne la nomment qu'une fois`() {
        val summary = messagesSummary(listOf(julien))

        assertThat(summary.headline).isEqualTo("2 non lus")
        assertThat(summary.detail).isEqualTo("Julien")
    }

    @Test
    fun `sans non-lu, le résumé dit « Aucun message »`() {
        val summary = messagesSummary(listOf(team, mum))

        assertThat(summary.headline).isNull()
        assertThat(summary.detail).isEqualTo("Aucun message")
        assertThat(summary.senders).isEmpty()
    }

    @Test
    fun `il y a un point par personne qui a écrit`() {
        val summary = messagesSummary(listOf(lea, team, julien))

        assertThat(summary.senders.map { it.name }).containsExactly("Léa", "Julien")
    }

    @Test
    fun `les favoris gardent l'ordre choisi dans les réglages`() {
        val favorites = favoritesAmong(listOf(julien, mum, lea, tom), favoriteIds = listOf("lea", "tom", "mum"))

        assertThat(favorites.map { it.name }).containsExactly("Léa", "Tom", "Maman")
    }

    @Test
    fun `un favori sans conversation n'a pas de tuile`() {
        val favorites = favoritesAmong(listOf(lea), favoriteIds = listOf("lea", "disparu"))

        assertThat(favorites.map { it.name }).containsExactly("Léa")
    }

    @Test
    fun `la tuile « Tous » compte les non-lus hors favoris`() {
        val others = unreadOutsideFavorites(listOf(lea, tom, julien), favoriteIds = listOf("lea", "tom"))

        assertThat(others).isEqualTo(2)
    }

    @Test
    fun `la tuile « Tous » s'annonce avec l'app qu'elle ouvre et ses non-lus`() {
        assertThat(allMessagesDescription(otherUnread = 0)).isEqualTo("Tous les messages dans Beeper")
        assertThat(allMessagesDescription(otherUnread = 1)).isEqualTo("Tous les messages dans Beeper, 1 non lu")
        assertThat(allMessagesDescription(otherUnread = 2)).isEqualTo("Tous les messages dans Beeper, 2 non lus")
    }

    @Test
    fun `une personne est annoncée avec son nombre de non-lus`() {
        assertThat(lea.description).isEqualTo("Léa, 1 non lu")
        assertThat(julien.description).isEqualTo("Julien, 2 non lus")
        assertThat(team.description).isEqualTo("Équipe")
    }

    @Test
    fun `l'initiale d'une personne garde son accent`() {
        assertThat(team.initials).isEqualTo("É")
        assertThat(Conversation("x", "léa", "SMS").initials).isEqualTo("L")
    }
}
