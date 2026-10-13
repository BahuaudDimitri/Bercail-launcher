package io.github.bahuauddimitri.bercail.data.messages

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.none
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.messages.Message
import io.github.bahuauddimitri.bercail.core.testing.FakeAccess
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

/**
 * One conversation, read from Beeper and answered through it: its last messages, sending, and what the user has
 * already seen. Beeper is played by [FakeBeeper].
 */
@RunWith(AndroidJUnit4::class)
class BeeperThreadUiTest {
    private val phone = ApplicationProvider.getApplicationContext<Application>()
    private val beeper = Robolectric.buildContentProvider(FakeBeeper::class.java).create("com.beeper.api").get()
    private val access = FakeAccess()
    private val paris = ZoneId.of("Europe/Paris")
    private val seen = MemorySeen()
    private val messages = BeeperMessages(phone, access, seen, zone = { paris }, background = Dispatchers.Unconfined)
    private val chatsAddress = Uri.parse("content://com.beeper.api/chats")

    private fun at(hour: Int, minute: Int): LocalDateTime = LocalDateTime.of(2026, 10, 2, hour, minute)

    private fun millis(hour: Int, minute: Int): Long = at(hour, minute).atZone(paris).toInstant().toEpochMilli()

    private fun history(id: String = "lea"): List<Message>? = runBlocking { messages.history(id).first { it != null } }

    private fun unreadOf(id: String): Int = runBlocking { messages.conversations.first().first { it.id == id }.unread }

    /** The conversation is open: someone listens to its messages. */
    private fun open(id: String, seen: MutableList<List<String>?>): CoroutineScope =
        CoroutineScope(Dispatchers.Unconfined).also { listener ->
            listener.launch { messages.history(id).collect { seen += it.texts() } }
        }

    @Test
    fun `les messages d'une conversation sont lus du plus ancien au plus récent, avec leur auteur et leur heure`() {
        beeper.chat("lea", "Léa")
        beeper.message("lea", "\$2", "Merci pour le déj !", millis(13, 31))
        beeper.message("lea", "\$1", "Je suis en terrasse", millis(12, 58))

        assertThat(history()).isEqualTo(
            listOf(
                Message("\$1", "Je suis en terrasse", mine = false, author = "Léa", at = at(12, 58)),
                Message("\$2", "Merci pour le déj !", mine = false, author = "Léa", at = at(13, 31))
            )
        )
    }

    @Test
    fun `mes messages sont reconnus comme les miens`() {
        beeper.chat("lea", "Léa")
        beeper.message("lea", "\$1", "J'arrive", millis(13, 0), mine = true, sender = null)

        assertThat(history()?.single()?.mine).isEqualTo(true)
    }

    @Test
    fun `Beeper est interrogé sur les trente derniers messages de cette conversation, et d'aucune autre`() {
        beeper.chat("lea", "Léa")
        beeper.chat("tom", "Tom")
        beeper.message("tom", "\$t", "Tu as vu le match hier ?", millis(7, 20))

        assertThat(history().texts()).isEqualTo(emptyList<String>())
        assertThat(beeper.asked.map { it.toString() }.filter { "/messages" in it })
            .containsExactly("content://com.beeper.api/messages?roomIds=lea&limit=30")
    }

    @Test
    fun `un message supprimé n'est pas montré`() {
        beeper.chat("lea", "Léa")
        beeper.message("lea", "\$1", "Oups", millis(13, 0), deleted = true)
        beeper.message("lea", "\$2", "Merci pour le déj !", millis(13, 31))

        assertThat(history().texts()).isEqualTo(listOf("Merci pour le déj !"))
    }

    @Test
    fun `une photo ou une vidéo, que Beeper ne donne pas, est annoncée comme une pièce jointe`() {
        beeper.chat("lea", "Léa")
        beeper.message("lea", "\$1", null, millis(13, 0), type = "IMAGE")
        beeper.message("lea", "\$2", " ", millis(13, 1), type = "VIDEO")

        assertThat(history().texts()).isEqualTo(listOf("Pièce jointe", "Pièce jointe"))
    }

    @Test
    fun `lire les conversations pour l'accueil ne lit l'historique d'aucune`() {
        beeper.chat("lea", "Léa", unread = 1)
        beeper.message("lea", "\$1", "Merci pour le déj !", millis(13, 31))

        runBlocking { messages.conversations.first() }

        assertThat(beeper.asked.map { it.path }).none { it.isEqualTo("/messages") }
    }

    @Test
    fun `à la réouverture, les messages déjà lus sont rendus tout de suite, puis relus`() {
        beeper.chat("lea", "Léa", at = 1)
        beeper.message("lea", "\$1", "Je suis en terrasse", millis(12, 58))
        val first = mutableListOf<List<String>?>()
        open("lea", first).cancel()
        assertThat(first).containsExactly(null, listOf("Je suis en terrasse"))

        beeper.message("lea", "\$2", "Merci pour le déj !", millis(13, 31))
        val second = mutableListOf<List<String>?>()
        open("lea", second).cancel()

        assertThat(second).containsExactly(
            listOf("Je suis en terrasse"),
            listOf("Je suis en terrasse", "Merci pour le déj !")
        )
    }

    @Test
    fun `quand la conversation ouverte bouge dans Beeper, ses messages sont relus, pas quand c'en est une autre`() {
        beeper.chat("lea", "Léa", at = 1)
        beeper.chat("tom", "Tom", at = 1)
        beeper.message("lea", "\$1", "Je suis en terrasse", millis(12, 58))
        val shown = mutableListOf<List<String>?>()
        val listener = open("lea", shown)
        val readings = { beeper.asked.count { it.path == "/messages" } }
        assertThat(readings()).isEqualTo(1)

        beeper.chat("tom", "Tom", unread = 1, at = 2)
        phone.contentResolver.notifyChange(chatsAddress, null)
        assertThat(readings()).isEqualTo(1)

        beeper.message("lea", "\$2", "Merci pour le déj !", millis(13, 31))
        beeper.chat("lea", "Léa", unread = 1, at = 3)
        phone.contentResolver.notifyChange(chatsAddress, null)

        assertThat(readings()).isEqualTo(2)
        assertThat(shown.last()).isEqualTo(listOf("Je suis en terrasse", "Merci pour le déj !"))
        listener.cancel()
    }

    @Test
    fun `sans l'accord de l'utilisateur, aucun message n'est lu ni envoyé`() {
        access.refuse(Access.Messages)
        beeper.chat("lea", "Léa")
        beeper.message("lea", "\$1", "Merci pour le déj !", millis(13, 31))

        assertThat(history().texts()).isEqualTo(emptyList<String>())
        assertThat(runBlocking { messages.send("lea", "J'arrive") }).isFalse()
        assertThat(beeper.asked).isEmpty()
        assertThat(beeper.sent).isEmpty()
    }

    @Test
    fun `envoyer remet le texte à Beeper, tel quel, dans la conversation voulue`() {
        beeper.chat("!lea:beeper.local", "Léa")

        val taken = runBlocking { messages.send("!lea:beeper.local", "J'arrive dans 10 min & toi ?") }

        assertThat(taken).isTrue()
        assertThat(beeper.sent).containsExactly("!lea:beeper.local" to "J'arrive dans 10 min & toi ?")
    }

    @Test
    fun `si Beeper ne prend pas le message, ou trébuche, l'envoi le dit`() {
        beeper.chat("lea", "Léa")
        beeper.refusing = true
        assertThat(runBlocking { messages.send("lea", "J'arrive") }).isFalse()

        beeper.refusing = false
        beeper.broken = true
        assertThat(runBlocking { messages.send("lea", "J'arrive") }).isFalse()
    }

    @Test
    fun `une conversation vue n'a plus de non-lus, jusqu'au message suivant`() {
        beeper.chat("lea", "Léa", unread = 2, at = 10)
        assertThat(unreadOf("lea")).isEqualTo(2)

        runBlocking { messages.markRead("lea") }
        assertThat(unreadOf("lea")).isEqualTo(0)

        beeper.chat("lea", "Léa", unread = 3, at = 11)
        assertThat(unreadOf("lea")).isEqualTo(3)
    }

    @Test
    fun `voir une conversation ne touche pas aux non-lus des autres`() {
        beeper.chat("lea", "Léa", unread = 2, at = 10)
        beeper.chat("tom", "Tom", unread = 1, at = 12)

        runBlocking { messages.markRead("lea") }

        assertThat(unreadOf("tom")).isEqualTo(1)
        assertThat(seen.seen.value).isEqualTo(mapOf("lea" to 10L))
    }

    @Test
    fun `une conversation inconnue de Beeper ne note rien comme vu`() {
        runBlocking { messages.markRead("disparue") }

        assertThat(seen.seen.value).isEmpty()
        assertThat(runBlocking { messages.history("disparue").first() }).isNull()
    }

    @Test
    fun `une conversation à plusieurs est reconnue comme un groupe`() {
        beeper.chat("team", "Équipe", protocol = "slack", group = true)
        beeper.chat("lea", "Léa", at = 1)

        val conversations = runBlocking { messages.conversations.first() }

        assertThat(conversations.map { it.name to it.group }).containsExactly("Léa" to false, "Équipe" to true)
    }
}
