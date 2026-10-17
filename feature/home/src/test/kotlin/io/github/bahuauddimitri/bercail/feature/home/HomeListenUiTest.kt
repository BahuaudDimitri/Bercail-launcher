package io.github.bahuauddimitri.bercail.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsAtLeast
import assertk.assertions.containsExactly
import assertk.assertions.containsNone
import assertk.assertions.isLessThan
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Moment
import io.github.bahuauddimitri.bercail.core.testing.Samples
import kotlin.math.abs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The « Écouter » sheet, where the music pill leads while nothing is loaded: its apps, and the access it needs. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PIXEL_9)
class HomeListenUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val opened = mutableListOf<String>()
    private val links = object : HomeLinks {
        override fun onAskAccess(access: Access) = opened.add("demande d'accès $access").let {}
        override fun onOpenAppSettings() = opened.add("page de Bercail dans les Paramètres").let {}
    }

    private fun show(world: FakeWorld = FakeWorld(Moment.Matin)): FakeWorld {
        val home = world.homeViewModel(world.media)
        compose.setContent { BcTheme { HomeRoute(home, links = links) } }
        return world
    }

    private fun silent(moment: Moment = Moment.Matin) = FakeWorld(moment).apply { media.stop() }

    private val inTheListenSheet = hasAnyAncestor(hasTestTag(LISTEN_TAG))

    @Test
    fun `sans contenu, la pastille propose « Écouter… » et ouvre la feuille Écouter`() {
        show(silent())

        compose.onNodeWithText("Écouter…").performClick()

        compose.onNode(hasText("Écouter") and isHeading() and inTheListenSheet).assertExists()
        listOf("Spotify", "Netflix", "Pocket Casts", "YouTube").forEach {
            compose.onNode(hasText(it) and inTheListenSheet).assertExists()
        }
    }

    @Test
    fun `toucher une app de la feuille Écouter l'ouvre`() {
        val world = show(silent())
        compose.onNodeWithText("Écouter…").performClick()

        compose.onNode(hasText("YouTube") and inTheListenSheet).performClick()

        assertThat(world.apps.opened).containsExactly("youtube")
    }

    @Test
    fun `l'app principale propose de reprendre le dernier morceau, et son bouton lecture le relance`() {
        val world = show()
        world.media.stop()
        world.media.lastPlayed = Samples.spotify
        compose.onNodeWithText("Écouter…").performClick()
        compose.onNodeWithText("Reprendre « Looped »").assertExists()

        compose.onNodeWithContentDescription("Reprendre dans Spotify").performClick()

        compose.onNodeWithText("Looped · Kiasmos").assertExists()
        compose.onNodeWithTag(LISTEN_TAG).assertDoesNotExist()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `le bouton lecture de l'app principale est plein de l'accent du moment, pêche le matin`() {
        val world = show()
        world.media.stop()
        world.media.lastPlayed = Samples.spotify
        compose.onNodeWithText("Écouter…").performClick()

        val play = compose.onNodeWithContentDescription("Reprendre dans Spotify").captureToImage().toPixelMap()
        val inside = with(compose.density) { 8.dp.roundToPx() }

        val fill = play[inside, play.height / 2]
        assertThat(maxOf(abs(fill.red - PEACH.red), abs(fill.green - PEACH.green), abs(fill.blue - PEACH.blue)))
            .isLessThan(0.03f)
    }

    @Test
    fun `le bouton lecture de l'app principale est un rond de 40 dp, qui tient dans sa ligne de 50 dp`() {
        val world = show()
        world.media.stop()
        world.media.lastPlayed = Samples.spotify
        compose.onNodeWithText("Écouter…").performClick()

        compose.onNodeWithContentDescription("Reprendre dans Spotify")
            .assertWidthIsEqualTo(40.dp)
            .assertHeightIsEqualTo(40.dp)
    }

    @Test
    fun `les apps de la feuille Écouter montrent leur vraie icône en couleurs, sans anneau`() {
        val world = show(silent())
        compose.onNodeWithText("Écouter…").performClick()
        compose.waitForIdle()

        assertThat(world.apps.iconRequests).containsAtLeast("spotify", "netflix", "pocket casts", "youtube")
        assertThat(world.apps.glyphRequests).containsNone("spotify", "netflix", "pocket casts", "youtube")
    }

    @Test
    fun `sans app de musique installée, la feuille Écouter le dit`() {
        show(silent().apply { mediaApps.apps.value = emptyList() })

        compose.onNodeWithText("Écouter…").performClick()

        compose.onNode(hasText("Aucune app de musique ou de vidéo", substring = true)).assertExists()
    }

    @Test
    fun `sous la feuille Écouter, l'accueil n'est plus lu, et le geste retour la referme`() {
        show(silent())
        compose.onNodeWithText("Écouter…").performClick()
        compose.onNodeWithText("Écouter…").assertDoesNotExist()

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

        compose.onNodeWithText("Écouter…").assertExists()
    }

    @Test
    fun `sans l'accès aux notifications, la feuille Écouter dit pourquoi le donner et le demande à Android`() {
        show(silent().apply { access.refuse(Access.Notifications) })
        compose.onNodeWithText("Écouter…").performClick()

        compose.onNode(hasText("Bercail ne lit aucune notification", substring = true)).assertExists()
        compose.onNodeWithText("Autoriser dans Android").performClick()

        assertThat(opened).containsExactly("demande d'accès Notifications")
    }

    @Test
    fun `si Android refuse l'accès, la feuille mène à la page de Bercail dans les Paramètres`() {
        show(silent().apply { access.refuse(Access.Notifications) })
        compose.onNodeWithText("Écouter…").performClick()

        compose.onNode(hasText("Paramètre restreint", substring = true)).assertExists()
        compose.onNodeWithText("Page de Bercail dans les Paramètres").performClick()

        assertThat(opened).containsExactly("page de Bercail dans les Paramètres")
    }

    @Test
    fun `sans l'accès aux notifications, la musique qui joue n'est pas montrée`() {
        show(FakeWorld(Moment.Matin).apply { access.refuse(Access.Notifications) })

        compose.onNodeWithText("Looped · Kiasmos").assertDoesNotExist()
        compose.onNodeWithText("Écouter…").assertExists()
    }

    @Test
    fun `dès qu'une musique démarre, la feuille Écouter se referme et la pastille la montre`() {
        val world = show(silent())
        compose.onNodeWithText("Écouter…").performClick()

        world.media.load(Samples.spotify)

        compose.onNodeWithTag(LISTEN_TAG).assertDoesNotExist()
        compose.onNodeWithText("Looped · Kiasmos").assertExists()
    }
}

/** The accent of the morning. */
private val PEACH = Color(0xFFF6C28B)
