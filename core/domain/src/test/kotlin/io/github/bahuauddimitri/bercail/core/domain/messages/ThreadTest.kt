package io.github.bahuauddimitri.bercail.core.domain.messages

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import java.time.LocalDateTime
import org.junit.Test

/** What the conversation sheet writes: who and where, each bubble with its true author and its hour. */
class ThreadTest {
    /** Friday 2 October 2026, 13:35. */
    private val now = LocalDateTime.of(2026, 10, 2, 13, 35)
    private val lea = Conversation("lea", "Léa", "WhatsApp")
    private val team = Conversation("team", "Équipe", "Slack", group = true)

    private fun theirs(id: String, text: String, at: LocalDateTime, author: String = "Léa") =
        Message(id, text, mine = false, author = author, at = at)

    private fun mine(id: String, text: String, at: LocalDateTime) = Message(id, text, mine = true, author = "", at = at)

    @Test
    fun `l'en-tête nomme le réseau par lequel passe la conversation`() {
        assertThat(lea.via).isEqualTo("WhatsApp via Beeper")
        assertThat(Conversation("x", "Sam", "Beeper").via).isEqualTo("Beeper")
    }

    @Test
    fun `un message du jour porte son heure, celui de la veille « hier », un plus ancien son jour`() {
        assertThat(messageTime(now.withHour(12).withMinute(58), now)).isEqualTo("12:58")
        assertThat(messageTime(now.minusDays(1).withHour(9).withMinute(5), now)).isEqualTo("hier 09:05")
        assertThat(messageTime(now.minusDays(3).withHour(18).withMinute(0), now)).isEqualTo("mar. 18:00")
        assertThat(messageTime(LocalDateTime.of(2026, 9, 1, 8, 0), now)).isEqualTo("1er sept.")
        assertThat(messageTime(LocalDateTime.of(2025, 12, 24, 8, 0), now)).isEqualTo("24 déc. 2025")
    }

    @Test
    fun `mes messages sont signés « Toi », jamais du nom du contact`() {
        val bubbles = threadOf(lea, listOf(mine("1", "J'arrive", now.minusMinutes(4))), emptyList(), now)

        assertThat(bubbles.single().mine).isTrue()
        assertThat(bubbles.single().author).isEqualTo("Moi")
        assertThat(bubbles.single().header).isEqualTo("Toi · 13:31")
    }

    @Test
    fun `en tête-à-tête, un message reçu ne porte que son heure`() {
        val bubbles = threadOf(lea, listOf(theirs("1", "Merci pour le déj !", now.minusMinutes(4))), emptyList(), now)

        assertThat(bubbles.single().mine).isFalse()
        assertThat(bubbles.single().author).isEqualTo("Léa")
        assertThat(bubbles.single().header).isEqualTo("13:31")
    }

    @Test
    fun `dans un groupe, chaque message reçu nomme celui qui l'a écrit`() {
        val message = theirs("1", "Slides dans le drive", now.minusMinutes(25), author = "Camille")

        val bubbles = threadOf(team, listOf(message), emptyList(), now)

        assertThat(bubbles.single().header).isEqualTo("Camille · 13:10")
        assertThat(bubbles.single().author).isEqualTo("Camille")
    }

    @Test
    fun `les messages sont rangés du plus ancien au plus récent, le dernier en bas`() {
        val history = listOf(
            theirs("2", "Merci pour le déj !", now.minusMinutes(4)),
            theirs("1", "Je suis en terrasse", now.minusMinutes(37))
        )

        val bubbles = threadOf(lea, history, emptyList(), now)

        assertThat(bubbles.map { it.text }).containsExactly("Je suis en terrasse", "Merci pour le déj !")
    }

    @Test
    fun `un message que je viens d'envoyer s'affiche tout de suite, à la suite, avant que Beeper le rende`() {
        val history = listOf(theirs("1", "On se retrouve à 12h30 ?", now.minusMinutes(18)))
        val sent = listOf(mine("local-1", "Oui, parfait", now))

        val bubbles = threadOf(lea, history, sent, now)

        assertThat(bubbles.map { it.text }).containsExactly("On se retrouve à 12h30 ?", "Oui, parfait")
        assertThat(bubbles.last().mine).isTrue()
    }

    @Test
    fun `une fois que Beeper rend mon message, il n'apparaît pas deux fois`() {
        val sent = listOf(mine("local-1", "Oui, parfait", now))
        val history = listOf(
            theirs("1", "On se retrouve à 12h30 ?", now.minusMinutes(18)),
            mine("\$abc", "Oui, parfait", now.plusSeconds(2))
        )

        assertThat(notYetIn(history, sent)).isEmpty()
        assertThat(threadOf(lea, history, notYetIn(history, sent), now).map { it.text })
            .containsExactly("On se retrouve à 12h30 ?", "Oui, parfait")
    }

    @Test
    fun `le même texte envoyé plus tôt ne fait pas disparaître celui que je viens d'envoyer`() {
        val sent = listOf(mine("local-1", "Oui", now))
        val history = listOf(mine("\$old", "Oui", now.minusHours(2)))

        assertThat(notYetIn(history, sent)).isEqualTo(sent)
    }

    @Test
    fun `un envoi vide ou fait d'espaces ne part pas, et un texte part sans ses espaces autour`() {
        assertThat(sendable("")).isEqualTo(null)
        assertThat(sendable("   \n")).isEqualTo(null)
        assertThat(sendable("  J'arrive ")).isEqualTo("J'arrive")
    }

    @Test
    fun `l'initiale d'un nom qui commence par un emoji est cet emoji entier`() {
        assertThat(Conversation("x", "🥰❤️", "WhatsApp").initials).isEqualTo("🥰")
        assertThat(Conversation("x", "", "WhatsApp").initials).isEqualTo("")
    }
}
