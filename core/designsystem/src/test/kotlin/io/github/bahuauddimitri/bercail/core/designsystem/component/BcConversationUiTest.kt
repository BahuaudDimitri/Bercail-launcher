package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BcConversationUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `la barre Chercher est annoncée comme le champ Chercher`() {
        compose.setContent { BcTheme { BcSearchField(value = "", onValueChange = {}) } }

        compose.onNode(hasSetTextAction()).assert(hasContentDescription("Chercher"))
    }

    @Test
    fun `la barre Chercher est une pilule de 48 dp de haut`() {
        compose.setContent { BcTheme { BcSearchField(value = "", onValueChange = {}) } }

        compose.onNodeWithTag(SEARCH_FIELD_TAG).assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun `taper dans la barre Chercher transmet le texte`() {
        var query by mutableStateOf("")
        compose.setContent { BcTheme { BcSearchField(value = query, onValueChange = { query = it }) } }

        compose.onNodeWithContentDescription("Chercher").performTextInput("spo")

        assertThat(query).isEqualTo("spo")
    }

    @Test
    fun `une barre Chercher désactivée reste visible mais ne se laisse pas remplir`() {
        compose.setContent { BcTheme { BcSearchField(value = "", onValueChange = {}, enabled = false) } }

        compose.onNodeWithContentDescription("Chercher").assertIsNotEnabled()
    }

    @Test
    fun `le bouton effacer vide la recherche`() {
        var query by mutableStateOf("spotify")
        compose.setContent { BcTheme { BcSearchField(value = query, onValueChange = { query = it }) } }

        compose.onNodeWithContentDescription("Effacer").assertHeightIsAtLeast(40.dp).performClick()

        assertThat(query).isEqualTo("")
    }

    @Test
    fun `le champ de réponse s'annonce « Répondre » et transmet ce qu'on y écrit`() {
        var draft by mutableStateOf("")
        compose.setContent { BcTheme { BcReplyField(value = draft, onValueChange = { draft = it }, onSend = {}) } }

        compose.onNode(hasSetTextAction()).assert(hasContentDescription("Répondre"))
        compose.onNodeWithContentDescription("Répondre").performTextInput("J'arrive")

        assertThat(draft).isEqualTo("J'arrive")
    }

    @Test
    fun `le bouton Envoyer est désactivé tant que le champ est vide, et envoie dès qu'il ne l'est plus`() {
        var draft by mutableStateOf("  ")
        var sent = 0
        compose.setContent {
            BcTheme { BcReplyField(value = draft, onValueChange = { draft = it }, onSend = { sent++ }) }
        }
        compose.onNodeWithContentDescription("Envoyer").assertIsNotEnabled()

        draft = "J'arrive"
        compose.onNodeWithContentDescription("Envoyer").assertHeightIsAtLeast(40.dp).performClick()

        assertThat(sent).isEqualTo(1)
    }

    @Test
    fun `mes messages ne sont jamais annoncés comme écrits par le contact`() {
        compose.setContent {
            BcTheme {
                BcBubble("J'arrive", author = "Léa", mine = true, pastel = BcPastel.Coral)
                BcBubble("Tu es où ?", author = "Léa", mine = false, pastel = BcPastel.Coral)
            }
        }

        compose.onNodeWithContentDescription("Moi : J'arrive").assertExists()
        compose.onNodeWithContentDescription("Léa : Tu es où ?").assertExists()
        compose.onNodeWithContentDescription("Léa : J'arrive").assertDoesNotExist()
    }

    @Test
    fun `annuler l'envoi est une cible d'au moins 40 dp qui déclenche l'annulation`() {
        var undone = 0
        compose.setContent { BcTheme { BcUndoButton(remaining = 0.5f, onUndo = { undone++ }) } }

        compose.onNodeWithText("Annuler l'envoi").assertHeightIsAtLeast(40.dp).performClick()

        assertThat(undone).isEqualTo(1)
    }

    @Test
    fun `la pastille musique annonce si la lecture est en cours ou en pause`() {
        compose.setContent {
            BcTheme {
                BcMusicPill(title = "Nekketsu", artist = "Nekfeu", cover = null, playing = false, onClick = {})
            }
        }

        compose.onNodeWithText("Nekketsu · Nekfeu")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "En pause"))
    }

    @Test
    fun `sans artiste connu, la pastille musique n'écrit que le titre`() {
        compose.setContent {
            BcTheme {
                BcMusicPill(title = "Tiny Desk Concert", artist = "", cover = null, playing = true, onClick = {})
            }
        }

        compose.onNodeWithText("Tiny Desk Concert").assertExists()
    }

    @Test
    fun `toucher la pastille musique ouvre l'écran Écoute`() {
        var opened = 0
        compose.setContent {
            BcTheme {
                BcMusicPill(title = "Nekketsu", artist = "Nekfeu", cover = null, playing = true, onClick = { opened++ })
            }
        }

        compose.onNodeWithText("Nekketsu · Nekfeu")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "En lecture"))
            .assertHeightIsAtLeast(40.dp)
            .performClick()

        assertThat(opened).isEqualTo(1)
    }
}
