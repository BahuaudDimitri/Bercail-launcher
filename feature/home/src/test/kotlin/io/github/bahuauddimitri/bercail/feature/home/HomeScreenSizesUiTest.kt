package io.github.bahuauddimitri.bercail.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.media.MediaContent
import io.github.bahuauddimitri.bercail.core.testing.FakeMediaSource
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Moment
import kotlin.time.Duration.Companion.seconds
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The home, its sheets and the Listen screen on the Pixel 9 and on smaller screens: what matters is shown whole, on
 * the screen, and nothing overlaps.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenSizesUiTest(size: ScreenSize) {
    @get:Rule(order = 0)
    val screen = ScreenSizeRule(size)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun show(world: FakeWorld = FakeWorld(Moment.Matin)) {
        val home = world.homeViewModel()
        compose.setContent { BcTheme { HomeRoute(home) } }
    }

    private fun text(text: String) = compose.onNodeWithText(text, useUnmergedTree = true)

    private fun described(description: String) = compose.onNodeWithContentDescription(description)

    private fun inside(tag: String, matcher: SemanticsMatcher) =
        compose.onNode(matcher and hasAnyAncestor(hasTestTag(tag)))

    private fun listen(content: MediaContent? = null) {
        val world = FakeWorld(Moment.Matin)
        val media = content?.let { FakeMediaSource(listOf(it), playing = true) } ?: world.media
        val home = world.homeViewModel(media)
        compose.setContent { BcTheme { HomeRoute(home) } }
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "En lecture")).performClick()
    }

    @Test
    fun `l'accueil montre en entier le héros, la pastille, le réveil, le bandeau, le dock et Chercher`() {
        show()

        compose.assertWholeAndApart(
            text("11° · Ciel dégagé, 17° cet après-midi"),
            text("Dans 1 h 20 · 09:30"),
            compose.onNode(hasText("Stand-up") and !hasAnyAncestor(hasTestTag(TIMELINE_TAG)), useUnmergedTree = true),
            text("Ensuite Déjeuner avec Léa à 12:30"),
            text("Looped · Kiasmos"),
            text("Salon 60 %, Volets ouverts"),
            described("Réveil demain à 07:15"),
            described("Chrome"),
            described("Téléphone"),
            described("Beeper"),
            described("Appareil photo"),
            described("Agenda"),
            described("Chercher")
        )
    }

    @Test
    fun `une pastille au titre long s'arrête avant le réveil au bout de la ligne du temps`() {
        val long = MediaContent(LONG_TITLE, "Un artiste au nom très long lui aussi", "Spotify", 300.seconds)
        val world = FakeWorld(Moment.Matin)
        val home = world.homeViewModel(FakeMediaSource(listOf(long), playing = true))
        compose.setContent { BcTheme { HomeRoute(home) } }

        compose.assertWholeAndApart(
            compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "En lecture")),
            described("Réveil demain à 07:15")
        )
    }

    @Test
    fun `la feuille ouverte montre en entier les gens, Tous, la maison et Tout`() {
        show()
        compose.openSheet()

        compose.assertWholeAndApart(
            described("Fermer la feuille"),
            text("Gens"),
            described("Léa, 1 non lu"),
            described("Tom, 1 non lu"),
            described("Équipe"),
            described("Maman"),
            described("Tous les messages dans Beeper"),
            text("Maison"),
            described("Salon"),
            described("Cinéma"),
            described("Volets"),
            described("Chauffage"),
            described("Tout")
        )
    }

    @Test
    fun `chaque anneau de la feuille garde son rond entier`() {
        show()
        compose.openSheet()

        listOf("Léa, 1 non lu", "Tous les messages dans Beeper").forEach {
            described(it).assertWidthIsAtLeast(RING).assertHeightIsAtLeast(RING)
        }
    }

    @Test
    fun `la conversation montre en entier son en-tête, son dernier message et le champ pour répondre`() {
        show()
        compose.openSheet()
        described("Léa, 1 non lu").performClick()

        compose.assertWholeAndApart(
            inside(TALK_TAG, hasText("Léa") and isHeading()),
            inside(TALK_TAG, hasText("WhatsApp via Beeper")),
            inside(TALK_TAG, hasText("Beeper") and hasClickAction()),
            inside(TALK_TAG, hasContentDescription("Léa : On se retrouve à 12h30 ?")),
            inside(TALK_TAG, hasSetTextAction()),
            inside(TALK_TAG, hasContentDescription("Envoyer"))
        )
    }

    @Test
    fun `la feuille de la journée montre en entier son titre, la date, Agenda et le premier rendez-vous`() {
        show()
        compose.onHeaderTitle("Stand-up").performClick()

        compose.assertWholeAndApart(
            inside(DAY_TAG, hasText("Aujourd'hui") and isHeading()),
            inside(DAY_TAG, hasText("vendredi 2 octobre")),
            inside(DAY_TAG, hasText("Agenda")),
            inside(DAY_TAG, hasText("Stand-up"))
        )
    }

    @Test
    fun `la feuille Écouter montre en entier son titre et les apps d'écoute`() {
        show(FakeWorld(Moment.Matin).apply { media.stop() })
        text("Écouter…").performClick()

        compose.assertWholeAndApart(
            inside(LISTEN_TAG, hasText("Écouter") and isHeading()),
            inside(LISTEN_TAG, hasText("Spotify")),
            inside(LISTEN_TAG, hasText("Netflix"))
        )
    }

    @Test
    fun `Écoute montre en entier l'arche, les temps, le titre, la télécommande, Ouvrir dans et le chevron`() {
        listen()

        compose.assertWholeAndApart(
            described("Pochette de Looped"),
            text("1:12"),
            text("Looped"),
            text("Kiasmos · Blurred EP"),
            described("Précédent"),
            described("Pause"),
            described("Suivant"),
            compose.onNode(hasText("Ouvrir dans Spotify") and hasClickAction()),
            described(CHEVRON)
        )
    }

    @Test
    fun `sur Écoute, un titre de deux lignes ne touche ni les temps ni la télécommande`() {
        listen(MediaContent(LONG_TITLE, "Cuisine du soir", "YouTube", 1080.seconds, album = "Recettes de saison"))

        compose.assertWholeAndApart(
            text("0:00"),
            text(LONG_TITLE),
            text("Cuisine du soir · Recettes de saison"),
            described("Pause"),
            compose.onNode(hasText("Ouvrir dans YouTube") and hasClickAction()),
            described(CHEVRON)
        )
    }

    @Test
    fun `sur Écoute, chaque commande garde au moins 40 dp sous le doigt`() {
        listen()

        listOf("Précédent", "Pause", "Suivant").forEach {
            described(it).assertWidthIsAtLeast(TOUCH).assertHeightIsAtLeast(TOUCH)
        }
        compose.onNode(hasText("Ouvrir dans Spotify") and hasClickAction()).assertHeightIsAtLeast(TOUCH)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun sizes() = SCREEN_SIZES.map { arrayOf<Any>(it) }

        private const val CHEVRON = "Glisser vers le haut pour revenir à l'accueil"
        private const val LONG_TITLE =
            "Ramen maison en 20 minutes, la recette complète du bouillon aux nouilles fraîches"
        private val TOUCH = 40.dp
        private val RING = 52.dp
    }
}
