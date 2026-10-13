package io.github.bahuauddimitri.bercail.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.screen.HomePage
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Moment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * What is slid to the right on the home screen, on a simulated phone: the messages line of the folded drawer opens
 * Beeper, the music pill opens the app that plays.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PIXEL_9)
class HomeSwipeUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val opened = mutableListOf<String>()
    private val links = object : HomeLinks {
        override fun onOpenMessagesApp() = opened.add("app de messages").let {}
    }
    private lateinit var home: HomeViewModel

    private fun show(world: FakeWorld = FakeWorld(Moment.Matin)) {
        home = world.homeViewModel()
        compose.setContent { BcTheme { HomeRoute(home, links = links) } }
    }

    /** Slides the line that holds [text] to the right, far enough to count. */
    private fun slideRight(text: String) = compose.onNodeWithText(text, substring = true).performTouchInput {
        swipe(start = Offset(40f, centerY), end = Offset(40f + 150.dp.toPx(), centerY))
    }

    @Test
    fun `glisser la ligne des messages vers la droite ouvre Beeper, sans déplier le tiroir`() {
        show()

        slideRight("non lus")

        assertThat(opened).containsExactly("app de messages")
        assertThat(home.state.value.drawerExpanded).isEqualTo(false)
    }

    @Test
    fun `la ligne des messages dit aux lecteurs d'écran qu'elle ouvre Beeper`() {
        show()

        val actions = compose.onNodeWithText("non lus", substring = true)
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]

        assertThat(actions.map { it.label }).containsExactly("Ouvrir Beeper")
    }

    @Test
    fun `tant que Beeper n'est pas relié, glisser la ligne des messages n'ouvre rien`() {
        show(FakeWorld(Moment.Matin).apply { access.refuse(Access.Messages) })

        slideRight("Beeper n'est pas relié")

        assertThat(opened).isEmpty()
    }

    @Test
    fun `la ligne de la maison ne glisse pas, tant que rien n'est derrière elle`() {
        show()

        slideRight("Salon")

        assertThat(opened).isEmpty()
        val house = compose.onNodeWithText("Salon", substring = true).fetchSemanticsNode()
        assertThat(house.config.getOrNull(SemanticsActions.CustomActions)).isNull()
    }

    @Test
    fun `glisser la pastille musique vers la droite ouvre l'app qui joue, sans quitter l'accueil`() {
        val world = FakeWorld(Moment.Trajet)
        show(world)

        slideRight("Looped")

        assertThat(world.media.appOpenings).isEqualTo(1)
        assertThat(home.state.value.page).isEqualTo(HomePage.Home)
    }

    @Test
    fun `glisser vers la gauche depuis la pastille musique mène toujours à l'écran Écoute`() {
        val world = FakeWorld(Moment.Trajet)
        show(world)

        compose.onNodeWithText("Looped", substring = true).performTouchInput {
            swipe(start = Offset(centerX, centerY), end = Offset(centerX - 150.dp.toPx(), centerY))
        }

        assertThat(world.media.appOpenings).isEqualTo(0)
        assertThat(home.state.value.page).isEqualTo(HomePage.Listen)
    }

    @Test
    fun `la pastille musique dit aux lecteurs d'écran quelle app elle ouvre`() {
        show(FakeWorld(Moment.Trajet))

        val actions = compose.onNodeWithText("Looped", substring = true)
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]

        assertThat(actions.map { it.label }).containsExactly("Ouvrir Spotify")
    }
}
