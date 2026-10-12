package io.github.bahuauddimitri.bercail

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.bahuauddimitri.bercail.data.messages.BeeperAccess
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

    private companion object {
        const val BEEPER = "com.beeper.android"
        const val TIMEOUT = 5_000L
    }
}

/** Beeper as the app sees it on the phone: an address that answers with the conversations it was given. */
class BeeperOnThePhone : ContentProvider() {
    private val chats = mutableMapOf<String, List<Any?>>()

    /** How many times Bercail asked for the conversations. */
    var questions = 0
        private set

    fun chat(roomId: String, title: String, unread: Int = 0, at: Long = 0) {
        chats[roomId] = listOf(roomId, title, "whatsapp", unread, 0, at)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        questions++
        val columns = arrayOf("roomId", "title", "protocol", "unreadCount", "isMuted", "timestamp")
        return MatrixCursor(columns).apply { chats.values.forEach(::addRow) }
    }

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        0
}
