package io.github.bahuauddimitri.bercail.feature.home

import android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotEqualTo
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.settings.TimelineOrientation
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Moment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The home screen on a simulated phone, fed by fake sources: what is shown, and what each gesture does. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PIXEL_9)
class HomeScreenUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val opened = mutableListOf<String>()
    private val links = object : HomeLinks {
        override fun onOpenDay() = opened.add("journée").let {}
        override fun onOpenConversation(id: String) = opened.add("conversation $id").let {}
        override fun onOpenMessagesApp() = opened.add("app de messages").let {}
        override fun onOpenHomeApp() = opened.add("app de la maison").let {}
        override fun onOpenMediaApps() = opened.add("apps média").let {}
        override fun onOpenMediaApp(app: String) = opened.add("app $app").let {}
    }

    private fun show(world: FakeWorld = FakeWorld(Moment.Matin)): FakeWorld {
        val home = world.homeViewModel()
        compose.setContent { BcTheme { HomeRoute(home, links = links) } }
        return world
    }

    private fun silent(moment: Moment = Moment.Matin) = FakeWorld(moment).apply { media.stop() }

    private fun searchBarBottom() = compose.onNodeWithContentDescription("Chercher").getUnclippedBoundsInRoot().bottom

    // ---------- top of the screen ----------

    @Test
    fun `le matin, l'accueil annonce la météo et le prochain rendez-vous`() {
        show()

        compose.onNodeWithText("11° · Ciel dégagé ce matin").assertExists()
        compose.onNodeWithText("Dans 1 h 20 · 09:30").assertExists()
        compose.onNodeWithText("Stand-up").assertExists()
        compose.onNodeWithText("Ensuite Déjeuner avec Léa à 12:30").assertExists()
    }

    @Test
    fun `le soir, l'accueil annonce le vent et le dernier rendez-vous`() {
        show(FakeWorld(Moment.Soir))

        compose.onNodeWithText("12° · Vent cette nuit").assertExists()
        compose.onNodeWithText("Ciné à la maison").assertExists()
        compose.onNodeWithText("Rien d'autre aujourd'hui").assertExists()
    }

    @Test
    fun `toucher l'en-tête demande la journée`() {
        show()

        compose.onNodeWithText("Stand-up").performClick()

        assertThat(opened).containsExactly("journée")
    }

    @Test
    fun `la ligne du temps verticale longe l'écran avec l'heure de chaque rendez-vous`() {
        show()

        val line = compose.onNodeWithTag(TIMELINE_TAG).getUnclippedBoundsInRoot()

        assertThat(line.height).isGreaterThan(line.width)
        listOf("09:30", "12:30", "14:00", "18:30", "21:00").forEach { compose.onNodeWithText(it).assertExists() }
    }

    @Test
    fun `en réglage horizontal, la ligne du temps se couche sous l'en-tête`() {
        val world = FakeWorld(Moment.Matin)
        world.settings.change { it.copy(timeline = TimelineOrientation.Horizontal) }
        show(world)

        val line = compose.onNodeWithTag(TIMELINE_TAG).getUnclippedBoundsInRoot()

        assertThat(line.width).isGreaterThan(line.height)
    }

    // ---------- drawer ----------

    @Test
    fun `le tiroir replié résume les messages et la maison`() {
        show()

        compose.onNodeWithText("2 non lus · Léa, Tom").assertExists()
        compose.onNodeWithText("Salon 60 %, Volets ouverts").assertExists()
        compose.onNodeWithContentDescription("Léa, 1 non lu").assertDoesNotExist()
    }

    @Test
    fun `sans message, le résumé dit « Aucun message »`() {
        val world = FakeWorld(Moment.Matin)
        world.messages.markRead("lea")
        world.messages.markRead("tom")
        show(world)

        compose.onNodeWithText("Aucun message").assertExists()
    }

    @Test
    fun `toucher la poignée déplie le tiroir, les favoris et la maison apparaissent`() {
        show()

        compose.onNodeWithContentDescription("Déplier le tiroir").performClick()

        compose.onNodeWithContentDescription("Léa, 1 non lu").assertExists()
        compose.onNodeWithContentDescription("Tom, 1 non lu").assertExists()
        compose.onNodeWithContentDescription("Équipe").assertExists()
        compose.onNodeWithContentDescription("Maman").assertExists()
        compose.onNodeWithContentDescription("Salon").assertIsOn()
        compose.onNodeWithContentDescription("Cinéma").assertIsOff()
        compose.onNodeWithText("2 non lus · Léa, Tom").assertDoesNotExist()
    }

    @Test
    fun `toucher une ligne du résumé déplie le tiroir`() {
        show()

        compose.onNodeWithText("Salon 60 %, Volets ouverts").performClick()

        compose.onNodeWithContentDescription("Replier le tiroir").assertExists()
        compose.onNodeWithContentDescription("Volets").assertExists()
    }

    @Test
    fun `glisser de plus de 24 dp vers le haut sur le tiroir le déplie, vers le bas le replie`() {
        show()

        compose.onNodeWithTag(DRAWER_TAG).performTouchInput {
            swipe(start = Offset(centerX, bottom - 8f), end = Offset(centerX, bottom - 8f - 40.dp.toPx()), 300)
        }
        compose.onNodeWithContentDescription("Replier le tiroir").assertExists()

        compose.onNodeWithTag(DRAWER_TAG).performTouchInput {
            swipe(start = Offset(centerX, top + 8f), end = Offset(centerX, top + 8f + 40.dp.toPx()), 300)
        }
        compose.onNodeWithContentDescription("Déplier le tiroir").assertExists()
    }

    @Test
    fun `la barre Chercher ne change jamais de place`() {
        show()
        val folded = searchBarBottom()

        compose.onNodeWithContentDescription("Déplier le tiroir").performClick()
        assertThat(searchBarBottom()).isEqualTo(folded)

        compose.onNodeWithText("Looped · Kiasmos").performClick()
        assertThat(searchBarBottom()).isEqualTo(folded)
    }

    @Test
    fun `sans recherche branchée, la barre Chercher reste inactive`() {
        show()

        compose.onNodeWithContentDescription("Chercher").assertIsNotEnabled()
    }

    @Test
    fun `toucher une personne demande sa conversation`() {
        show()
        compose.onNodeWithContentDescription("Déplier le tiroir").performClick()

        compose.onNodeWithContentDescription("Léa, 1 non lu").performClick()

        assertThat(opened).containsExactly("conversation lea")
    }

    @Test
    fun `« Tous » annonce les non-lus hors favoris et demande l'app de messages`() {
        show(FakeWorld(Moment.Soir))
        compose.onNodeWithContentDescription("Déplier le tiroir").performClick()

        compose.onNodeWithContentDescription("Tous les messages dans Beeper, 2 non lus").performClick()

        assertThat(opened).containsExactly("app de messages")
    }

    @Test
    fun `toucher une commande maison la bascule sur place`() {
        show()
        compose.onNodeWithContentDescription("Déplier le tiroir").performClick()

        compose.onNodeWithContentDescription("Volets").performClick()

        compose.onNodeWithContentDescription("Volets")
            .assertIsOff()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "fermés"))
    }

    @Test
    fun `la scène Cinéma met le Salon à 10 pour cent et ferme les volets d'un seul toucher`() {
        show()
        compose.onNodeWithContentDescription("Déplier le tiroir").performClick()

        compose.onNodeWithContentDescription("Cinéma").performClick()

        compose.onNodeWithContentDescription("Salon")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "10 %"))
        compose.onNodeWithContentDescription("Volets").assertIsOff()
        compose.onNodeWithContentDescription("Replier le tiroir").performClick()
        compose.onNodeWithText("Salon 10 %, Cinéma active").assertExists()
    }

    @Test
    fun `« Tout » demande l'app de la maison`() {
        show()
        compose.onNodeWithContentDescription("Déplier le tiroir").performClick()

        compose.onNodeWithContentDescription("Tout").performClick()

        assertThat(opened).containsExactly("app de la maison")
    }

    // ---------- music and screens ----------

    @Test
    fun `avec un contenu chargé, la pastille montre le titre et l'artiste`() {
        show()

        compose.onNodeWithText("Looped · Kiasmos")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "En lecture"))
    }

    @Test
    fun `sans contenu, la pastille propose « Écouter… » et demande les apps média`() {
        show(silent())

        compose.onNodeWithText("Écouter…").performClick()

        assertThat(opened).containsExactly("apps média")
    }

    @Test
    fun `toucher la pastille ouvre l'écran Écoute et sa télécommande`() {
        show()

        compose.onNodeWithText("Looped · Kiasmos").performClick()

        compose.onNodeWithContentDescription("Pochette de Looped").assertExists()
        compose.onNodeWithText("Looped").assertExists()
        compose.onNodeWithText("Kiasmos").assertExists()
        compose.onNodeWithText("Ouvrir dans Spotify").assertExists()
        compose.onNodeWithText("1:12").assertExists()
        compose.onNodeWithText("4:05").assertExists()
        listOf("Précédent", "Pause", "Suivant").forEach { compose.onNodeWithContentDescription(it).assertExists() }
    }

    @Test
    fun `glisser vers la gauche au milieu ouvre l'écran Écoute, vers la droite revient à l'Accueil`() {
        show()

        compose.onNodeWithTag(MIDDLE_TAG).performTouchInput { swipeLeft() }
        compose.onNodeWithContentDescription("Pause").assertExists()

        compose.onNodeWithTag(MIDDLE_TAG).performTouchInput { swipeRight() }
        compose.onNodeWithText("Looped · Kiasmos").assertExists()
        compose.onNodeWithContentDescription("Pause").assertDoesNotExist()
    }

    @Test
    fun `sans contenu, glisser vers la gauche ne fait rien`() {
        show(silent())

        compose.onNodeWithTag(MIDDLE_TAG).performTouchInput { swipeLeft() }

        compose.onNodeWithText("Écouter…").assertExists()
        compose.onNodeWithContentDescription("Lecture").assertDoesNotExist()
    }

    @Test
    fun `les points de pagination n'existent que si un contenu est chargé`() {
        val world = show()
        compose.onNodeWithContentDescription("Écran Accueil").assertIsSelected()

        world.media.stop()

        compose.onNodeWithContentDescription("Écran Accueil").assertDoesNotExist()
    }

    @Test
    fun `toucher le point « Écoute » ouvre l'écran Écoute`() {
        show()

        compose.onNodeWithContentDescription("Écran Écoute").performClick()

        compose.onNodeWithContentDescription("Écran Écoute").assertIsSelected()
        compose.onNodeWithText("Ouvrir dans Spotify").assertExists()
    }

    @Test
    fun `sur l'écran Écoute, le tiroir devient la télécommande et la ligne du temps s'efface`() {
        show()

        compose.onNodeWithText("Looped · Kiasmos").performClick()

        compose.onNodeWithContentDescription("Déplier le tiroir").assertDoesNotExist()
        compose.onNodeWithText("2 non lus · Léa, Tom").assertDoesNotExist()
        compose.onNodeWithTag(TIMELINE_TAG).assertDoesNotExist()
    }

    @Test
    fun `le bouton Pause met en pause et devient Lecture`() {
        show()
        compose.onNodeWithText("Looped · Kiasmos").performClick()

        compose.onNodeWithContentDescription("Pause").performClick()

        compose.onNodeWithContentDescription("Lecture").assertExists()
    }

    @Test
    fun `Suivant change de morceau, sur la télécommande comme sur la pochette`() {
        show()
        compose.onNodeWithText("Looped · Kiasmos").performClick()

        compose.onNodeWithContentDescription("Suivant").performClick()

        compose.onNodeWithText("Says").assertExists()
        compose.onNodeWithText("Nils Frahm").assertExists()
        compose.onNodeWithText("8:48").assertExists()
    }

    @Test
    fun `Précédent revient au début du morceau après 5 secondes`() {
        show()
        compose.onNodeWithText("Looped · Kiasmos").performClick()

        compose.onNodeWithContentDescription("Précédent").performClick()

        compose.onNodeWithText("Looped").assertExists()
        compose.onNodeWithText("0:00").assertExists()
    }

    @Test
    fun `« Ouvrir dans Spotify » demande l'app qui joue`() {
        show()
        compose.onNodeWithText("Looped · Kiasmos").performClick()

        compose.onNodeWithText("Ouvrir dans Spotify").performClick()

        assertThat(opened).containsExactly("app Spotify")
    }

    @Test
    fun `quand la musique s'arrête sur l'écran Écoute, l'Accueil revient`() {
        val world = show()
        compose.onNodeWithText("Looped · Kiasmos").performClick()

        world.media.stop()

        compose.onNodeWithText("Écouter…").assertExists()
        compose.onNodeWithText("2 non lus · Léa, Tom").assertExists()
    }

    @Test
    fun `le tiroir retrouve son état en revenant de l'écran Écoute`() {
        show()
        compose.onNodeWithContentDescription("Déplier le tiroir").performClick()
        compose.onNodeWithContentDescription("Écran Écoute").performClick()

        compose.onNodeWithContentDescription("Écran Accueil").performClick()

        compose.onNodeWithContentDescription("Replier le tiroir").assertExists()
        compose.onNodeWithContentDescription("Léa, 1 non lu").assertExists()
    }

    // ---------- background and battery ----------

    @Test
    fun `avec un contenu chargé, Brume couvre l'écran et cache le fond d'écran du téléphone`() {
        compose.activity.window.addFlags(FLAG_SHOW_WALLPAPER)
        show()

        compose.onNodeWithTag(BRUME_TAG).assertExists()
        assertThat(compose.activity.window.attributes.flags and FLAG_SHOW_WALLPAPER).isEqualTo(0)
    }

    @Test
    fun `sans contenu, le fond d'écran du téléphone reste visible`() {
        compose.activity.window.addFlags(FLAG_SHOW_WALLPAPER)
        show(silent())

        compose.onNodeWithTag(BRUME_TAG).assertDoesNotExist()
        assertThat(compose.activity.window.attributes.flags and FLAG_SHOW_WALLPAPER).isNotEqualTo(0)
    }

    @Test
    fun `quand la musique s'arrête, le fond d'écran du téléphone revient`() {
        compose.activity.window.addFlags(FLAG_SHOW_WALLPAPER)
        val world = show()

        world.media.stop()
        compose.waitForIdle()

        compose.onNodeWithTag(BRUME_TAG).assertDoesNotExist()
        assertThat(compose.activity.window.attributes.flags and FLAG_SHOW_WALLPAPER).isNotEqualTo(0)
    }

    @Test
    fun `sans source branchée, l'accueil montre ses états vides, jamais de données inventées`() {
        val home = unconnectedHomeViewModel(hour = 9)
        compose.setContent { BcTheme { HomeRoute(home) } }

        compose.onNodeWithText("Plus rien aujourd'hui").assertExists()
        compose.onNodeWithText("Journée libre").assertExists()
        compose.onNodeWithText("Aucun message").assertExists()
        compose.onNodeWithText("Maison au repos").assertExists()
        compose.onNodeWithText("Écouter…").assertExists()
        compose.onNodeWithTag(BRUME_TAG).assertDoesNotExist()
    }

    @Test
    fun `quand l'accueil n'est plus à l'écran, plus aucune source n'est écoutée`() {
        val world = show()
        compose.waitForIdle()
        assertThat(world.messages.conversations.subscriptionCount.value).isEqualTo(1)

        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.waitForIdle()

        assertThat(world.messages.conversations.subscriptionCount.value).isEqualTo(0)
        assertThat(world.home.controls.subscriptionCount.value).isEqualTo(0)
        assertThat(world.weather.weather.subscriptionCount.value).isEqualTo(0)
    }
}
