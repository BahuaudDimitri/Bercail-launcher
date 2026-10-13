package io.github.bahuauddimitri.bercail.data.messages

import android.app.Application
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.messages.Conversation
import io.github.bahuauddimitri.bercail.core.testing.FakeAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/** The conversations, read from Beeper on the phone. Beeper is played by [FakeBeeper]. */
@RunWith(AndroidJUnit4::class)
class BeeperMessagesUiTest {
    private val phone = ApplicationProvider.getApplicationContext<Application>()
    private val beeper = Robolectric.buildContentProvider(FakeBeeper::class.java).create("com.beeper.api").get()
    private val access = FakeAccess()
    private val messages = BeeperMessages(phone, access, MemorySeen(), background = Dispatchers.Unconfined)
    private val chatsAddress = Uri.parse("content://com.beeper.api/chats")

    private fun conversations(): List<Conversation> = runBlocking { messages.conversations.first() }

    @Test
    fun `les conversations sont lues avec leur nom, leur réseau et leurs non-lus`() {
        beeper.chat("!lea:beeper.local", "Léa", protocol = "whatsapp", unread = 2)

        assertThat(conversations()).containsExactly(Conversation("!lea:beeper.local", "Léa", "WhatsApp", unread = 2))
    }

    @Test
    fun `la conversation qui a bougé en dernier vient en premier`() {
        beeper.chat("old", "Tom", at = 1_000)
        beeper.chat("new", "Léa", at = 3_000)
        beeper.chat("middle", "Maman", at = 2_000)

        assertThat(conversations().map { it.name }).containsExactly("Léa", "Maman", "Tom")
    }

    @Test
    fun `Beeper est interrogé une fois, sur cent conversations au plus, sans les archivées ni les moins importantes`() {
        conversations()

        assertThat(beeper.asked.map { it.toString() })
            .containsExactly("content://com.beeper.api/chats?limit=100&isArchived=0&isLowPriority=0")
    }

    @Test
    fun `une conversation mise en sourdine dans Beeper est reconnue`() {
        beeper.chat("group", "Les voisins", muted = true, at = 2)
        beeper.chat("lea", "Léa", at = 1)

        assertThat(conversations().map { it.name to it.muted })
            .containsExactly("Les voisins" to true, "Léa" to false)
    }

    @Test
    fun `chaque réseau porte le nom qu'on lui connaît`() {
        val named = mapOf(
            "whatsapp" to "WhatsApp",
            "signal" to "Signal",
            "telegram" to "Telegram",
            "instagram" to "Instagram",
            "instagramgo" to "Instagram",
            "facebook" to "Messenger",
            "facebookgo" to "Messenger",
            "imessage" to "iMessage",
            "gmessages" to "Google Messages",
            "sms" to "SMS",
            "rcs" to "RCS",
            "discord" to "Discord",
            "slack" to "Slack",
            "linkedin" to "LinkedIn",
            "twitter" to "X",
            "googlechat" to "Google Chat",
            "beeper" to "Beeper",
            "matrix" to "Beeper",
            "hungryserv" to "Beeper"
        )

        named.forEach { (protocol, name) -> assertThat(networkName(protocol), protocol).isEqualTo(name) }
    }

    @Test
    fun `un réseau inconnu garde son nom, avec une majuscule, et un réseau absent s'appelle Beeper`() {
        assertThat(networkName("threema")).isEqualTo("Threema")
        assertThat(networkName(null)).isEqualTo("Beeper")
        assertThat(networkName(" ")).isEqualTo("Beeper")
    }

    @Test
    fun `une conversation sans titre s'appelle « Sans nom »`() {
        beeper.chat("a", null)
        beeper.chat("b", "  ")

        assertThat(conversations().map { it.name }).containsExactly("Sans nom", "Sans nom")
    }

    @Test
    fun `sans l'accord de l'utilisateur, Beeper n'est ni interrogé ni observé`() {
        access.refuse(Access.Messages)
        beeper.chat("lea", "Léa")

        assertThat(conversations()).isEmpty()
        assertThat(beeper.asked).isEmpty()
        assertThat(shadowOf(phone.contentResolver).getContentObservers(chatsAddress)).isEmpty()
    }

    @Test
    fun `dès que l'utilisateur donne son accord, les conversations sont lues`() {
        access.refuse(Access.Messages)
        beeper.chat("lea", "Léa")
        val listener = CoroutineScope(Dispatchers.Unconfined)
        val seen = mutableListOf<List<String>>()
        listener.launch { messages.conversations.collect { all -> seen += all.map { it.name } } }

        access.grant(Access.Messages)

        assertThat(seen).containsExactly(emptyList<String>(), listOf("Léa"))
        listener.cancel()
    }

    @Test
    fun `quand Beeper signale un changement, les conversations sont relues`() {
        beeper.chat("lea", "Léa")
        val listener = CoroutineScope(Dispatchers.Unconfined)
        val seen = mutableListOf<List<Int>>()
        listener.launch { messages.conversations.collect { all -> seen += all.map { it.unread } } }

        beeper.chat("lea", "Léa", unread = 1)
        phone.contentResolver.notifyChange(chatsAddress, null)

        assertThat(seen).containsExactly(listOf(0), listOf(1))
        listener.cancel()
    }

    @Test
    fun `dès que plus personne n'écoute, Beeper n'a plus à prévenir Bercail`() {
        val listener = CoroutineScope(Dispatchers.Unconfined)
        listener.launch { messages.conversations.collect {} }
        assertThat(shadowOf(phone.contentResolver).getContentObservers(chatsAddress)).hasSize(1)

        listener.cancel()

        assertThat(shadowOf(phone.contentResolver).getContentObservers(chatsAddress)).isEmpty()
    }

    @Test
    fun `si l'accès est repris pendant la lecture, la liste se vide sans faire tomber l'accueil`() {
        beeper.chat("lea", "Léa")
        beeper.locked = true

        assertThat(conversations()).isEmpty()
    }

    @Test
    fun `si Beeper trébuche, la liste se vide sans faire tomber l'accueil`() {
        beeper.chat("lea", "Léa")
        beeper.broken = true

        assertThat(conversations()).isEmpty()
    }

    @Test
    fun `l'accès à Beeper, ce sont ses deux droits, lire et envoyer`() {
        assertThat(BeeperAccess.permissions).containsExactly(
            "com.beeper.android.permission.READ_PERMISSION",
            "com.beeper.android.permission.SEND_PERMISSION"
        )
        assertThat(BeeperAccess.granted(phone)).isFalse()

        shadowOf(phone).grantPermissions("com.beeper.android.permission.READ_PERMISSION")
        assertThat(BeeperAccess.granted(phone)).isFalse()

        shadowOf(phone).grantPermissions("com.beeper.android.permission.SEND_PERMISSION")
        assertThat(BeeperAccess.granted(phone)).isTrue()
    }

    @Test
    fun `« Tous » ouvre Beeper`() {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage("com.beeper.android")
        val activity = ActivityInfo().apply {
            packageName = "com.beeper.android"
            name = "com.beeper.android.MainActivity"
        }
        shadowOf(phone.packageManager).addResolveInfoForIntent(
            launcher,
            ResolveInfo().apply {
                activityInfo = activity
            }
        )

        messages.openApp()

        assertThat(shadowOf(phone).nextStartedActivity.component?.packageName).isEqualTo("com.beeper.android")
    }

    @Test
    fun `sans Beeper sur le téléphone, « Tous » n'ouvre rien`() {
        messages.openApp()

        assertThat(shadowOf(phone).nextStartedActivity).isNull()
    }
}
