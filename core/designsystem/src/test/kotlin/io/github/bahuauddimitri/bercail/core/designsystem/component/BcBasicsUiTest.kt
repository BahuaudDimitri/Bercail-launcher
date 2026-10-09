package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BcBasicsUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `le texte s'affiche`() {
        compose.setContent { BcTheme { BcText("Dans 1 h 20") } }

        compose.onNodeWithText("Dans 1 h 20").assertExists()
    }

    @Test
    fun `une icône avec description est annoncée par TalkBack`() {
        compose.setContent { BcTheme { BcIcon(BcIcons.Mic, contentDescription = "Dicter") } }

        compose.onNodeWithContentDescription("Dicter").assertExists()
    }

    @Test
    fun `toucher le bouton déclenche son action`() {
        var clicks = 0
        compose.setContent { BcTheme { BcButton("Ouvrir dans Spotify", onClick = { clicks++ }) } }

        compose.onNodeWithText("Ouvrir dans Spotify").performClick()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun `un bouton désactivé ne réagit pas`() {
        var clicks = 0
        compose.setContent { BcTheme { BcButton("Envoyer", onClick = { clicks++ }, enabled = false) } }

        compose.onNodeWithText("Envoyer").assertIsNotEnabled().performClick()

        assertThat(clicks).isEqualTo(0)
    }

    @Test
    fun `le bouton est annoncé comme un bouton et fait au moins 40 dp de haut`() {
        compose.setContent { BcTheme { BcButton("Reprendre", onClick = {}) } }

        compose.onNodeWithText("Reprendre")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(40.dp)
    }

    @Test
    fun `le bouton rond est annoncé par sa description et fait au moins 40 dp`() {
        var clicks = 0
        compose.setContent {
            BcTheme { BcIconButton(BcIcons.Send, contentDescription = "Envoyer", onClick = { clicks++ }) }
        }

        compose.onNodeWithContentDescription("Envoyer").assertHeightIsAtLeast(40.dp).performClick()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun `toucher une pastille déclenche son action, sur au moins 40 dp`() {
        var clicks = 0
        compose.setContent { BcTheme { BcPill("J'arrive", onClick = { clicks++ }) } }

        compose.onNodeWithText("J'arrive").assertHeightIsAtLeast(40.dp).performClick()

        assertThat(clicks).isEqualTo(1)
    }
}
