package io.github.bahuauddimitri.bercail

import android.app.Activity
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Looper
import android.speech.RecognizerIntent
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.bahuauddimitri.bercail.data.messages.BeeperAccess
import java.time.Duration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The conversations in the whole app on a simulated phone: Bercail asks before reading Beeper, shows nothing of it
 * until the user agrees, then the drawer follows what Beeper says. Beeper is played by [BeeperOnThePhone].
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h923dp-420dpi")
class MessagesUiTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app = ApplicationProvider.getApplicationContext<BercailApplication>()
    private val beeper = Robolectric.buildContentProvider(BeeperOnThePhone::class.java).create("com.beeper.api").get()

    private fun showHome(): ActivityScenario<MainActivity> = ActivityScenario.launch(MainActivity::class.java)

    /** The user agrees, on Android's screen, to let Bercail read Beeper and answer in it. */
    private fun linkBeeper() = shadowOf(app).grantPermissions(*BeeperAccess.permissions.toTypedArray())

    private fun awaitText(text: String) =
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun awaitDescription(text: String) = compose.waitUntil(TIMEOUT) {
        compose.onAllNodesWithContentDescription(text).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun `tant que Beeper n'est pas relié, aucune conversation n'est lue ni montrée`() {
        beeper.chat("lea", "Léa", unread = 1)

        showHome()

        awaitText("Beeper n'est pas relié")
        assertThat(beeper.questions).isEqualTo(0)
    }

    @Test
    fun `une fois Beeper relié, le tiroir résume qui a écrit et montre les dernières conversations`() {
        beeper.chat("lea", "Léa", unread = 1, at = 3)
        beeper.chat("tom", "Tom", unread = 2, at = 2)
        beeper.chat("mum", "Maman", at = 1)
        linkBeeper()

        showHome()

        awaitText("3 non lus · Léa, Tom")
        compose.onNodeWithContentDescription("Déplier le tiroir").performClick()
        awaitDescription("Léa, 1 non lu")
        compose.onNodeWithContentDescription("Tom, 2 non lus").assertExists()
        compose.onNodeWithContentDescription("Maman").assertExists()
    }

    @Test
    fun `un message qui arrive dans Beeper met le tiroir à jour sans rien toucher`() {
        beeper.chat("lea", "Léa")
        linkBeeper()
        showHome()
        awaitText("Aucun message")

        beeper.chat("lea", "Léa", unread = 1)
        app.contentResolver.notifyChange(Uri.parse("content://com.beeper.api/chats"), null)

        awaitText("1 non lu · Léa")
    }

    @Test
    fun `la tuile « Tous » ouvre Beeper`() {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(BEEPER)
        val activity = ActivityInfo().apply {
            packageName = BEEPER
            name = "$BEEPER.MainActivity"
        }
        shadowOf(app.packageManager).addResolveInfoForIntent(launcher, ResolveInfo().apply { activityInfo = activity })
        linkBeeper()
        showHome()
        awaitText("Aucun message")
        compose.onNodeWithContentDescription("Déplier le tiroir").performClick()

        compose.onNodeWithContentDescription("Tous les messages dans Beeper").performClick()

        assertThat(shadowOf(app).nextStartedActivity.component?.packageName).isEqualTo(BEEPER)
    }

    private fun openLea(): ActivityScenario<MainActivity> {
        beeper.chat("lea", "Léa", unread = 1, at = 3)
        beeper.message("lea", "Merci pour le déj !", at = 3)
        linkBeeper()
        val home = showHome()
        awaitText("1 non lu · Léa")
        compose.onNodeWithContentDescription("Déplier le tiroir").performClick()
        awaitDescription("Léa, 1 non lu")
        compose.onNodeWithContentDescription("Léa, 1 non lu").performClick()
        awaitDescription("Léa : Merci pour le déj !")
        return home
    }

    @Test
    fun `toucher une personne ouvre sa conversation, lue dans Beeper, et son badge s'efface`() {
        openLea()

        compose.onNodeWithText("WhatsApp via Beeper").assertExists()
        compose.onNodeWithContentDescription("Fermer la feuille").performClick()
        awaitDescription("Léa")
        compose.onNodeWithContentDescription("Léa, 1 non lu").assertDoesNotExist()
    }

    @Test
    fun `une réponse n'est remise à Beeper qu'après les six secondes où elle peut être annulée`() {
        openLea()

        compose.onNodeWithContentDescription("Répondre").performTextInput("De rien !")
        compose.onNodeWithContentDescription("Envoyer").performClick()
        awaitText("Annuler l'envoi")
        assertThat(beeper.sent).isEqualTo(emptyList<String>())

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(6_500))
        compose.waitUntil(TIMEOUT) { beeper.sent.isNotEmpty() }

        assertThat(beeper.sent).isEqualTo(listOf("lea: De rien !"))
    }

    @Test
    fun `le micro ouvre la dictée d'Android, et ce qui est entendu arrive dans le champ`() {
        val home = openLea()

        compose.onNodeWithContentDescription("Dicter").performClick()
        val asked = shadowOf(app).nextStartedActivityForResult
        assertThat(asked.intent.action).isEqualTo(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

        val heard = Intent().putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, arrayListOf("à tout à l'heure"))
        home.onActivity { shadowOf(it).receiveResult(asked.intent, Activity.RESULT_OK, heard) }

        compose.waitUntil(TIMEOUT) {
            compose.onAllNodes(hasSetTextAction() and hasText("à tout à l'heure")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun `le bouton Accueil referme la conversation`() {
        val home = openLea()

        home.onActivity { activity ->
            val pressed = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(activity, pressed)
        }

        compose.waitUntil(TIMEOUT) {
            compose.onAllNodesWithText("WhatsApp via Beeper").fetchSemanticsNodes().isEmpty()
        }
    }

    private companion object {
        const val BEEPER = "com.beeper.android"
        const val TIMEOUT = 5_000L
    }
}

/** Beeper as the app sees it on the phone: an address that answers with the conversations it was given. */
class BeeperOnThePhone : ContentProvider() {
    private val chats = mutableMapOf<String, List<Any?>>()
    private val messages = mutableListOf<List<Any?>>()

    /** How many times Bercail asked for the conversations. */
    var questions = 0
        private set

    /** What Bercail sent so far: "lea: J'arrive". */
    val sent = mutableListOf<String>()

    fun chat(roomId: String, title: String, unread: Int = 0, at: Long = 0) {
        chats[roomId] = listOf(roomId, title, "whatsapp", unread, 0, at, 1)
    }

    fun message(roomId: String, text: String, at: Long, mine: Boolean = false) {
        messages += listOf(roomId, "\$${messages.size}", text, if (mine) 1 else 0, 0, "Léa", at)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        questions++
        val room = uri.getQueryParameter("roomIds")
        return if (uri.path == "/messages") {
            MatrixCursor(MESSAGE_COLUMNS).apply { messages.filter { it[0] == room }.forEach(::addRow) }
        } else {
            MatrixCursor(CHAT_COLUMNS).apply { chats.values.filter { room == null || it[0] == room }.forEach(::addRow) }
        }
    }

    /** A message sent: Beeper keeps it in the conversation, which moves. */
    override fun insert(uri: Uri, values: ContentValues?): Uri {
        val room = checkNotNull(uri.getQueryParameter("roomId"))
        val text = checkNotNull(uri.getQueryParameter("text"))
        sent += "$room: $text"
        val now = (chats[room]?.get(STAMP) as Long) + 1
        message(room, text, now, mine = true)
        chats[room] = chats.getValue(room).toMutableList().also { it[STAMP] = now }
        context?.contentResolver?.notifyChange(Uri.parse("content://com.beeper.api/chats"), null)
        return uri
    }

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        0

    private companion object {
        const val STAMP = 5
        val CHAT_COLUMNS = arrayOf("roomId", "title", "protocol", "unreadCount", "isMuted", "timestamp", "oneToOne")
        val MESSAGE_COLUMNS =
            arrayOf("roomId", "originalId", "text_content", "isSentByMe", "isDeleted", "displayName", "timestamp")
    }
}
