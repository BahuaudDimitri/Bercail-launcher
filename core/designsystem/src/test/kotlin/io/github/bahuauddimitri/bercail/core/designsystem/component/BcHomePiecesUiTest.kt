package io.github.bahuauddimitri.bercail.core.designsystem.component

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions.OnClick
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcWeather
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The small pieces the home screen is made of: summary lines, page dots, cover, weather line. */
@RunWith(AndroidJUnit4::class)
class BcHomePiecesUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var clicks = 0
    private var page by mutableIntStateOf(0)

    @Test
    fun `une ligne de résumé écrit le nombre puis les noms`() {
        compose.setContent {
            BcTheme {
                BcSummaryRow(
                    text = "Léa, Tom",
                    strong = "2 non lus",
                    leading = BcSummaryLeading.Dots(listOf(BcPastel.Coral, BcPastel.Blue)),
                    onClick = {}
                )
            }
        }

        compose.onNodeWithText("2 non lus · Léa, Tom").assertExists()
    }

    @Test
    fun `une ligne de résumé sans nombre n'écrit que son texte`() {
        compose.setContent {
            BcTheme {
                BcSummaryRow("Maison au repos", leading = BcSummaryLeading.Icon(BcIcons.House), onClick = {})
            }
        }

        compose.onNodeWithText("Maison au repos").assertExists()
    }

    @Test
    fun `une ligne de résumé se touche sur au moins 40 dp de haut et dit ce qu'elle ouvre`() {
        compose.setContent {
            BcTheme {
                BcSummaryRow(
                    text = "Aucun message",
                    leading = BcSummaryLeading.Dots(emptyList()),
                    onClick = { clicks++ },
                    onClickLabel = "Ouvrir le tiroir"
                )
            }
        }

        compose.onNodeWithText("Aucun message")
            .assertHeightIsAtLeast(40.dp)
            .assert(SemanticsMatcher("dit « Ouvrir le tiroir »") { it.config[OnClick].label == "Ouvrir le tiroir" })
            .performClick()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun `toucher un point de pagination demande son écran`() {
        compose.setContent {
            BcTheme { BcPageDots(listOf("Accueil", "Écoute"), selected = page, onSelect = { page = it }) }
        }

        compose.onNodeWithContentDescription("Écran Écoute").performClick()

        assertThat(page).isEqualTo(1)
        compose.onNodeWithContentDescription("Écran Écoute").assertIsSelected()
        compose.onNodeWithContentDescription("Écran Accueil").assertIsNotSelected()
    }

    @Test
    fun `chaque point de pagination se touche sur au moins 40 dp de haut`() {
        compose.setContent {
            BcTheme { BcPageDots(listOf("Accueil", "Écoute"), selected = 0, onSelect = {}) }
        }

        compose.onNodeWithContentDescription("Écran Accueil").assertHeightIsAtLeast(40.dp)
    }

    @Test
    fun `la grande pochette fait 200 dp et s'annonce par son titre`() {
        compose.setContent {
            BcTheme {
                BcCover(image = null, colors = Color.Red to Color.Blue, contentDescription = "Pochette de Looped")
            }
        }

        compose.onNodeWithContentDescription(
            "Pochette de Looped"
        ).assertWidthIsEqualTo(200.dp).assertHeightIsEqualTo(200.dp)
    }

    @Test
    fun `la ligne météo écrit la température et la phrase`() {
        compose.setContent { BcTheme { BcWeatherLine("11° · Ciel dégagé ce matin", BcWeather.Clear) } }

        compose.onNodeWithText("11° · Ciel dégagé ce matin").assertExists()
    }

    @Test
    fun `un bouton rond peut être agrandi pour la télécommande`() {
        compose.setContent {
            BcTheme { BcIconButton(BcIcons.Pause, "Pause", onClick = {}, size = 66.dp) }
        }

        compose.onNodeWithContentDescription("Pause").assertExists()
        compose.onRoot().assertWidthIsEqualTo(66.dp).assertHeightIsEqualTo(66.dp)
    }

    @Test
    fun `le tiroir laisse la barre de navigation du téléphone sous son contenu`() {
        compose.setContent {
            BcTheme {
                Box(Modifier.fillMaxSize()) {
                    BcDrawer(
                        expanded = false,
                        onExpandedChange = {},
                        modifier = Modifier.align(Alignment.BottomCenter).testTag("drawer")
                    ) {
                        BcButton("Salon", onClick = {}, modifier = Modifier.testTag("content"))
                    }
                }
            }
        }
        val before = gapUnderContent()

        compose.activityRule.scenario.onActivity { activity ->
            val composeView = activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
            val bars = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, NAVIGATION_BAR_PX))
                .build()
            ViewCompat.dispatchApplyWindowInsets(composeView, bars)
        }
        compose.waitForIdle()

        assertThat(gapUnderContent()).isGreaterThan(before)
    }

    private fun gapUnderContent(): Float {
        val drawer = compose.onNodeWithTag("drawer").getUnclippedBoundsInRoot()
        val content = compose.onNodeWithTag("content").getUnclippedBoundsInRoot()
        return (drawer.bottom - content.bottom).value
    }

    private companion object {
        const val NAVIGATION_BAR_PX = 200
    }
}
