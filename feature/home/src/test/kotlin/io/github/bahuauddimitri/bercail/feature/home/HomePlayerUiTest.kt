package io.github.bahuauddimitri.bercail.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isBetween
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThanOrEqualTo
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.Picture
import io.github.bahuauddimitri.bercail.core.domain.media.MediaAction
import io.github.bahuauddimitri.bercail.core.domain.media.MediaContent
import io.github.bahuauddimitri.bercail.core.domain.media.MediaSource
import io.github.bahuauddimitri.bercail.core.testing.FakeMediaSource
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Moment
import io.github.bahuauddimitri.bercail.core.testing.Samples
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The Listen screen on a simulated Pixel 9: the player alone, its arch from the top edge, all of it on screen. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class HomePlayerUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Shows the home screen, then touches the music pill, which leads to the Listen screen. */
    private fun listen(media: MediaSource = FakeWorld(Moment.Matin).media) {
        val home = FakeWorld(Moment.Matin).homeViewModel(media)
        compose.setContent { BcTheme { HomeRoute(home) } }
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "En lecture")).performClick()
    }

    private fun bounds(text: String) = compose.onNodeWithText(text).getUnclippedBoundsInRoot()

    private fun boundsOf(description: String) =
        compose.onNodeWithContentDescription(description).getUnclippedBoundsInRoot()

    private val seekable = SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)

    @Test
    fun `la pochette est une arche de 300 sur 484 dp, pendue au bord haut de l'écran et centrée`() {
        listen()

        val cover = boundsOf("Pochette de Looped")

        assertThat(cover.top).isEqualTo(0.dp)
        assertThat(cover.left.value).isCloseTo((PHONE_WIDTH - ARCH_WIDTH) / 2, TOLERANCE)
        compose.onNodeWithContentDescription("Pochette de Looped")
            .assertWidthIsEqualTo(300.dp)
            .assertHeightIsEqualTo(484.dp)
    }

    @Test
    fun `sous l'arche, le temps écoulé et la durée, le titre, puis l'artiste et l'album`() {
        listen()

        compose.onNodeWithText("1:12").assertExists()
        compose.onNodeWithText("4:05").assertExists()
        compose.onNodeWithText("Looped").assertExists()
        compose.onNodeWithText("Kiasmos · Blurred EP").assertExists()
        compose.onNodeWithText("Ouvrir dans Spotify").assertExists()
    }

    @Test
    fun `les temps sont au centre de l'arc, 514 dp sous le bord haut`() {
        listen()

        // The centre of the arch's curve is 334 dp below its top; the times sit 180 dp under it.
        assertThat(bounds("1:12").top.value).isCloseTo(TIMES_TOP, TEXT_TOLERANCE)
    }

    @Test
    fun `sous l'arche, le titre, la télécommande et « Ouvrir dans » descendent avec elle, 44 dp plus bas qu'avant`() {
        listen()

        assertThat(bounds("Looped").top.value).isCloseTo(TITLE_TOP, TEXT_TOLERANCE)
        assertThat(boundsOf("Pause").top.value).isCloseTo(REMOTE_TOP, TOLERANCE)
        val open = compose.onNode(hasText("Ouvrir dans Spotify") and hasClickAction()).getUnclippedBoundsInRoot()
        assertThat(open.top.value).isCloseTo(OPEN_TOP, TOLERANCE)
    }

    @Test
    fun `la grande pochette garde aussi des versions réduites, pour rester nette dans la petite pastille`() {
        val cover = Picture(COVER_SIDE, COVER_SIDE, IntArray(COVER_SIDE * COVER_SIDE) { BLUE })
        var image: ImageBitmap? = null

        compose.setContent { image = cover.asImage() }

        assertThat(image!!.asAndroidBitmap().hasMipMap()).isTrue()
    }

    @Test
    fun `sans album connu, la ligne sous le titre montre l'artiste seul`() {
        val single = MediaContent("Nuvole bianche", "Ludovico Einaudi", "Spotify", 340.seconds)
        listen(FakeMediaSource(listOf(single), playing = true))

        compose.onNodeWithText("Ludovico Einaudi").assertExists()
    }

    @Test
    fun `le bouton lecture-pause est un rond de 76 dp, Précédent et Suivant des ronds de 48 dp`() {
        listen()

        compose.onNodeWithContentDescription("Pause").assertWidthIsEqualTo(76.dp).assertHeightIsEqualTo(76.dp)
        compose.onNodeWithContentDescription("Précédent").assertWidthIsEqualTo(48.dp)
        compose.onNodeWithContentDescription("Suivant").assertWidthIsEqualTo(48.dp)
    }

    @Test
    fun `sans Précédent, lecture-pause reste au milieu de l'écran`() {
        val youtube = FakeMediaSource(Samples.youtube, playing = true, actions = setOf(MediaAction.Next))
        listen(youtube)

        val pause = boundsOf("Pause")

        compose.onNodeWithContentDescription("Précédent").assertDoesNotExist()
        assertThat((pause.left + pause.right).value / 2).isCloseTo(PHONE_WIDTH / 2, TOLERANCE)
    }

    @Test
    fun `un titre de deux lignes tient sur l'écran du Pixel 9 sans que rien ne se chevauche`() {
        val long = MediaContent(LONG_TITLE, "Cuisine du soir", "YouTube", 1080.seconds, album = "Recettes de saison")
        listen(FakeMediaSource(listOf(long), playing = true))

        val title = bounds(LONG_TITLE)
        assertThat(title.height.value).isBetween(TWO_LINES, TWO_LINES + 2 * TEXT_TOLERANCE)
        // From top to bottom: the times, the title, the artist and album, the remote, the app, the chevron.
        val column = listOf(
            bounds("0:00"),
            title,
            bounds("Cuisine du soir · Recettes de saison"),
            boundsOf("Pause"),
            bounds("Ouvrir dans YouTube"),
            boundsOf(CHEVRON)
        )
        column.zipWithNext().forEach { (above, below) -> assertThat(above.bottom).isLessThanOrEqualTo(below.top) }
        (column + boundsOf("Pochette de $LONG_TITLE")).forEach { assertThat(it.isOnScreen()).isTrue() }
    }

    @Test
    fun `sous le chevron, il reste plus de 70 dp sur le Pixel 9`() {
        listen()

        val chevron = boundsOf(CHEVRON)
        val screen = compose.onRoot().getUnclippedBoundsInRoot()

        assertThat(chevron.top.value).isCloseTo(CHEVRON_TOP, TOLERANCE)
        assertThat((screen.bottom - chevron.bottom).value).isGreaterThan(MIN_ROOM_UNDER_CHEVRON)
    }

    @Test
    fun `un direct dit « En direct » au centre de l'arc, sans les temps, et l'arc ne se tire pas`() {
        val live = MediaContent("Le 7/9", "France Inter", "Radio France", duration = Duration.ZERO)
        listen(FakeMediaSource(listOf(live), playing = true))

        compose.onNodeWithText("En direct").assertExists()
        compose.onAllNodesWithText("0:00").assertCountEquals(0)
        compose.onAllNodes(seekable).assertCountEquals(0)
        compose.onNodeWithContentDescription("Pause").assertExists()
    }

    @Test
    fun `l'arc se tire quand l'app accepte qu'on se déplace dans le morceau`() {
        listen()

        compose.onAllNodes(seekable).assertCountEquals(1)
    }

    @Test
    fun `quand l'app n'accepte pas qu'on se déplace dans le morceau, l'arc ne réagit pas`() {
        listen(FakeMediaSource(Samples.spotify, playing = true, actions = setOf(MediaAction.Next)))

        compose.onAllNodes(seekable).assertCountEquals(0)
    }

    // ---------- leaving ----------

    private fun backHome() {
        compose.onNodeWithText("Looped · Kiasmos").assertExists()
        compose.onNodeWithContentDescription("Pause").assertDoesNotExist()
    }

    @Test
    fun `glisser vers le haut sur Écoute revient à l'Accueil`() {
        listen()

        compose.onNodeWithTag(PLAYER_TAG).performTouchInput { swipeUp() }

        backHome()
    }

    @Test
    fun `glisser vers le haut en partant de l'arc déplace dans le morceau, sans quitter Écoute`() {
        listen()

        compose.onNodeWithTag(PLAYER_TAG).performTouchInput {
            // The bottom of the arc: 164 dp under the centre of the arch's curve, itself 334 dp under the top edge.
            val bottomOfArc = Offset(centerX, (334 + 164).dp.toPx())
            swipe(start = bottomOfArc, end = bottomOfArc - Offset(0f, 150.dp.toPx()))
        }

        compose.onNodeWithContentDescription("Pause").assertExists()
        compose.onNodeWithText("1:12").assertDoesNotExist()
    }

    @Test
    fun `aucun glissement vers la gauche ou la droite ne fait rien sur Écoute`() {
        listen()

        compose.onNodeWithTag(PLAYER_TAG).performTouchInput { swipeLeft() }
        compose.onNodeWithTag(PLAYER_TAG).performTouchInput { swipeRight() }

        compose.onNodeWithContentDescription("Pause").assertExists()
        compose.onNodeWithText("1:12").assertExists()
    }

    @Test
    fun `un lecteur d'écran revient à l'Accueil par l'action « Revenir à l'accueil » du chevron`() {
        listen()
        val chevron = compose.onNodeWithContentDescription(CHEVRON)
        val actions = chevron.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertThat(actions.map { it.label }).containsExactly("Revenir à l'accueil")

        compose.runOnUiThread { actions.single().action() }

        backHome()
    }

    private fun DpRect.isOnScreen(): Boolean =
        top >= 0.dp && left >= 0.dp && bottom.value <= PHONE_HEIGHT && right.value <= PHONE_WIDTH

    private companion object {
        const val CHEVRON = "Glisser vers le haut pour revenir à l'accueil"
        const val PHONE_WIDTH = 411f
        const val PHONE_HEIGHT = 923f
        const val ARCH_WIDTH = 300f
        const val TIMES_TOP = 514f
        const val TITLE_TOP = 550f
        const val REMOTE_TOP = 648f
        const val OPEN_TOP = 746f
        const val CHEVRON_TOP = 820f
        const val MIN_ROOM_UNDER_CHEVRON = 70f
        const val TOLERANCE = 1f
        const val TEXT_TOLERANCE = 4f
        const val TWO_LINES = 62f
        const val COVER_SIDE = 1280
        const val BLUE = 0xFF2040C0.toInt()
        const val LONG_TITLE = "Ramen maison en 20 minutes, la recette complète du bouillon aux nouilles fraîches"
    }
}
