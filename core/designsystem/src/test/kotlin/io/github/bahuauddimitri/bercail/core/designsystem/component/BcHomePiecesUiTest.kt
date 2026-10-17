package io.github.bahuauddimitri.bercail.core.designsystem.component

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions.OnClick
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
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

/**
 * The small pieces the home screen is made of: summary lines, music pill, weather line, the buttons of the Listen
 * screen.
 */
@RunWith(AndroidJUnit4::class)
class BcHomePiecesUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var clicks = 0

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
    fun `une ligne de résumé peut écrire la maison à la suite des messages, sur la même ligne`() {
        compose.setContent {
            BcTheme {
                BcSummaryRow(
                    text = "Léa, Tom",
                    strong = "2 non lus",
                    aside = "Salon 60 %, Volets ouverts",
                    leading = BcSummaryLeading.Dots(listOf(BcPastel.Coral, BcPastel.Blue)),
                    onClick = { clicks++ }
                )
            }
        }

        val messages = compose.onNodeWithText("2 non lus · Léa, Tom", useUnmergedTree = true)
        val home = compose.onNodeWithText("Salon 60 %, Volets ouverts", useUnmergedTree = true)
        assertThat(home.getUnclippedBoundsInRoot().left).isGreaterThan(messages.getUnclippedBoundsInRoot().right)
        assertThat(home.getUnclippedBoundsInRoot().top).isEqualTo(messages.getUnclippedBoundsInRoot().top)
        home.performClick()
        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun `une ligne de résumé se touche sur au moins 40 dp de haut et dit ce qu'elle ouvre`() {
        compose.setContent {
            BcTheme {
                BcSummaryRow(
                    text = "Aucun message",
                    leading = BcSummaryLeading.Dots(emptyList()),
                    onClick = { clicks++ },
                    onClickLabel = "Ouvrir la feuille"
                )
            }
        }

        compose.onNodeWithText("Aucun message")
            .assertHeightIsAtLeast(40.dp)
            .assert(SemanticsMatcher("dit « Ouvrir la feuille »") { it.config[OnClick].label == "Ouvrir la feuille" })
            .performClick()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun `la pastille musique finit par un chevron vers la droite, après le titre et l'égaliseur`() {
        compose.setContent {
            BcTheme {
                BcMusicPill(title = "Looped", artist = "Kiasmos", cover = null, playing = true, onClick = {})
            }
        }

        val chevron = compose.onNodeWithTag(MUSIC_PILL_CHEVRON_TAG, useUnmergedTree = true)
            .assertWidthIsEqualTo(16.dp)
            .getUnclippedBoundsInRoot()
        val text = compose.onNodeWithText("Looped · Kiasmos", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertThat(chevron.left).isGreaterThan(text.right)
    }

    @Test
    fun `la ligne météo écrit la température et la phrase`() {
        compose.setContent { BcTheme { BcWeatherLine("11° · Ciel dégagé ce matin", BcWeather.Clear) } }

        compose.onNodeWithText("11° · Ciel dégagé ce matin").assertExists()
    }

    @Test
    fun `la ligne météo commence par le glyphe de son ciel, la lune pour une nuit claire`() {
        assertThat(BcWeather.Clear.glyph(night = false)).isEqualTo(BcIcons.Sun)
        assertThat(BcWeather.Clear.glyph(night = true)).isEqualTo(BcIcons.Moon)
        assertThat(BcWeather.Rain.glyph(night = true)).isEqualTo(BcIcons.Rain)
        assertThat(BcWeather.Wind.glyph(night = false)).isEqualTo(BcIcons.Wind)
        assertThat(BcWeather.Snow.glyph(night = false)).isEqualTo(BcIcons.Snow)
    }

    @Test
    fun `le bouton lecture-pause d'Écoute est un rond de 76 dp, son icône de 30 dp`() {
        compose.setContent {
            BcTheme { BcIconButton(BcIcons.Pause, "Pause", onClick = {}, size = 76.dp) }
        }

        compose.onRoot().assertWidthIsEqualTo(76.dp).assertHeightIsEqualTo(76.dp)
        compose.onNodeWithContentDescription("Pause", useUnmergedTree = true)
            .assertWidthIsEqualTo(30.dp)
            .assertHeightIsEqualTo(30.dp)
    }

    @Test
    fun `le bandeau laisse la barre de navigation du téléphone sous son contenu`() {
        compose.setContent {
            BcTheme {
                Box(Modifier.fillMaxSize()) {
                    BcBand(
                        onOpen = {},
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
