package io.github.bahuauddimitri.bercail.feature.home

import android.os.Looper
import android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
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
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaEvent
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The home screen on a simulated phone, fed by fake sources: what is shown, and what each gesture does. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PIXEL_9)
class HomeScreenUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val opened = mutableListOf<String>()
    private val links = object : HomeLinks {
        override fun onAskAccess(access: Access) = opened.add("demande d'accès $access").let {}
        override fun onOpenMessagesApp() = opened.add("app de messages").let {}
        override fun onOpenHomeApp() = opened.add("app de la maison").let {}
        override fun onOpenAppSettings() = opened.add("page de Bercail dans les Paramètres").let {}
    }

    private fun show(world: FakeWorld = FakeWorld(Moment.Matin), media: MediaSource = world.media): FakeWorld {
        val home = world.homeViewModel(media)
        compose.setContent { BcTheme { HomeRoute(home, links = links) } }
        return world
    }

    private fun silent(moment: Moment = Moment.Matin) = FakeWorld(moment).apply { media.stop() }

    private val inTheDaySheet = hasAnyAncestor(hasTestTag(DAY_TAG))
    private val inTheTalkSheet = hasAnyAncestor(hasTestTag(TALK_TAG))

    private fun searchBarBottom() = compose.onNodeWithContentDescription("Chercher").getUnclippedBoundsInRoot().bottom

    // ---------- top of the screen ----------

    @Test
    fun `le matin, l'accueil annonce la météo et le prochain rendez-vous`() {
        show()

        compose.onNodeWithText("11° · Ciel dégagé, 17° cet après-midi").assertExists()
        compose.onNodeWithText("Dans 1 h 20 · 09:30").assertExists()
        compose.onHeaderTitle("Stand-up").assertExists()
        compose.onNodeWithText("Ensuite Déjeuner avec Léa à 12:30").assertExists()
    }

    @Test
    fun `le soir, l'accueil annonce le vent et le dernier rendez-vous`() {
        show(FakeWorld(Moment.Soir))

        compose.onNodeWithText("12° · Vent 35 km/h cette nuit").assertExists()
        compose.onHeaderTitle("Ciné à la maison").assertExists()
        compose.onNodeWithText("Rien d'autre aujourd'hui").assertExists()
    }

    // ---------- the sheet of the day ----------

    @Test
    fun `toucher l'en-tête ouvre la feuille « Aujourd'hui », avec la date et les rendez-vous du jour`() {
        show()

        compose.onHeaderTitle("Stand-up").performClick()

        compose.onNode(hasText("Aujourd'hui") and isHeading()).assertExists()
        compose.onNodeWithText("vendredi 2 octobre").assertExists()
        listOf("Stand-up", "Déjeuner avec Léa", "Point design", "Courses", "Ciné à la maison").forEach {
            compose.onNode(hasText(it) and inTheDaySheet).assertExists()
        }
    }

    @Test
    fun `sous la feuille de la journée, l'accueil n'est plus lu`() {
        show()
        compose.onHeaderTitle("Stand-up").performClick()

        compose.onNodeWithText("Dans 1 h 20 · 09:30").assertDoesNotExist()
        compose.onNodeWithContentDescription("Chercher").assertDoesNotExist()
    }

    @Test
    fun `la feuille de la journée se referme par sa poignée, et l'accueil revient`() {
        show()
        compose.onHeaderTitle("Stand-up").performClick()

        compose.onNodeWithContentDescription("Fermer la feuille").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Aujourd'hui").assertDoesNotExist()
        compose.onNodeWithText("Dans 1 h 20 · 09:30").assertExists()
    }

    @Test
    fun `le geste retour referme la feuille de la journée`() {
        show()
        compose.onHeaderTitle("Stand-up").performClick()

        compose.waitForIdle()

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()

        compose.onNodeWithText("Aujourd'hui").assertDoesNotExist()
        compose.onNodeWithText("Dans 1 h 20 · 09:30").assertExists()
    }

    @Test
    fun `toucher un rendez-vous de la feuille l'ouvre dans l'app agenda du téléphone`() {
        val world = show()
        compose.onHeaderTitle("Stand-up").performClick()

        compose.onNode(hasText("Point design") and inTheDaySheet).performClick()

        assertThat(world.agenda.opened).containsExactly("event: Point design")
    }

    @Test
    fun `ouverte sur un rendez-vous, la feuille de la journée défile jusqu'à lui`() {
        val world = FakeWorld(Moment.Matin)
        val first = Samples.day.atTime(7, 0)
        world.agenda.set(
            (0 until MANY_EVENTS).map {
                val start = first.plusMinutes(EVENT_STEP * it)
                AgendaEvent("Rendez-vous $it", "Rendez-vous $it", start, start.plusMinutes(EVENT_STEP / 2))
            }
        )
        val home = world.homeViewModel(world.media)
        compose.setContent { BcTheme { HomeRoute(home, links = links) } }

        compose.runOnUiThread { home.onDayOpenChange(true, focus = "Rendez-vous 18") }

        compose.onNode(hasText("Rendez-vous 18") and inTheDaySheet).assertIsDisplayed()
    }

    @Test
    fun `le bouton Agenda de la feuille ouvre l'app agenda du téléphone à la date du jour`() {
        val world = show()
        compose.onHeaderTitle("Stand-up").performClick()

        compose.onNodeWithText("Agenda").performClick()

        assertThat(world.agenda.opened).containsExactly("calendar: 2026-10-02")
    }

    @Test
    fun `une journée sans rendez-vous le dit dans la feuille, ouverte par la ligne du temps`() {
        show(FakeWorld(Moment.Matin).apply { agenda.set(emptyList()) })

        compose.onNodeWithTag(TIMELINE_TAG).performTouchInput { click(Offset(width - 30f, height * 0.9f)) }

        compose.onNodeWithText("Rien de prévu aujourd'hui").assertExists()
    }

    @Test
    fun `sans autorisation, l'accueil propose de relier l'agenda, et la feuille de la demander à Android`() {
        show(FakeWorld(Moment.Matin).apply { access.refuse(Access.Calendar) })
        compose.onNodeWithText("Agenda pas encore relié").assertExists()
        compose.onHeaderTitle("Stand-up").assertDoesNotExist()

        compose.onNodeWithText("Relier l'agenda").performClick()
        compose.onNodeWithText("Autoriser la lecture de l'agenda").performClick()

        assertThat(opened).containsExactly("demande d'accès Calendar")
    }

    // ---------- the line of the day ----------

    @Test
    fun `la ligne du temps verticale longe l'écran avec l'heure de chaque rendez-vous`() {
        show()

        val line = compose.onNodeWithTag(TIMELINE_TAG).getUnclippedBoundsInRoot()

        assertThat(line.height).isGreaterThan(line.width)
        listOf("09:30", "12:30", "14:00", "18:30", "21:00").forEach { compose.onNodeWithText(it).assertExists() }
    }

    // ---------- drawer ----------

    @Test
    fun `le bandeau résume les messages et la maison sur une seule ligne`() {
        show()

        compose.onNodeWithText("2 non lus · Léa, Tom").assertExists()
        compose.onNodeWithText("Salon 60 %, Volets ouverts").assertExists()
        compose.onNodeWithContentDescription("Léa, 1 non lu").assertDoesNotExist()
    }

    @Test
    fun `sans message, le résumé dit « Aucun message »`() {
        val world = FakeWorld(Moment.Matin)
        world.messages.readNow("lea")
        world.messages.readNow("tom")
        show(world)

        compose.onNodeWithText("Aucun message").assertExists()
    }

    @Test
    fun `toucher la ligne résumé ouvre la feuille, les favoris et la maison apparaissent`() {
        show()

        compose.openSheet()

        compose.onNodeWithContentDescription("Léa, 1 non lu").assertExists()
        compose.onNodeWithContentDescription("Tom, 1 non lu").assertExists()
        compose.onNodeWithContentDescription("Équipe").assertExists()
        compose.onNodeWithContentDescription("Maman").assertExists()
        compose.onNodeWithContentDescription("Salon").assertIsOn()
        compose.onNodeWithContentDescription("Cinéma").assertIsOff()
        compose.onNodeWithText("2 non lus · Léa, Tom").assertDoesNotExist()
    }

    @Test
    fun `toucher la maison au bout de la ligne résumé ouvre aussi la feuille`() {
        show()

        compose.onNodeWithText("Salon 60 %, Volets ouverts").performClick()

        compose.onNodeWithContentDescription("Fermer la feuille").assertExists()
        compose.onNodeWithContentDescription("Volets").assertExists()
    }

    @Test
    fun `glisser de plus de 24 dp vers le haut sur le bandeau ouvre la feuille, vers le bas la ferme`() {
        show()

        compose.onNodeWithTag(DRAWER_TAG).performTouchInput {
            swipe(start = Offset(centerX, bottom - 8f), end = Offset(centerX, bottom - 8f - 40.dp.toPx()), 300)
        }
        compose.onNodeWithContentDescription("Fermer la feuille").assertExists()

        compose.onNodeWithTag(DRAWER_TAG).performTouchInput {
            swipe(start = Offset(centerX, top + 8f), end = Offset(centerX, top + 8f + 40.dp.toPx()), 300)
        }
        compose.onNode(OPENS_THE_SHEET).assertExists()
    }

    @Test
    fun `la feuille ouverte couvre la barre Chercher, qui n'a pas bougé quand elle revient`() {
        show()
        val folded = searchBarBottom()

        compose.openSheet()
        compose.onNodeWithContentDescription("Chercher").assertDoesNotExist()
        compose.onNodeWithText("2 non lus · Léa, Tom").assertDoesNotExist()

        compose.onNodeWithContentDescription("Fermer la feuille").performClick()
        assertThat(searchBarBottom()).isEqualTo(folded)
    }

    @Test
    fun `le geste retour ferme la feuille ouverte, toucher à côté ne la ferme pas`() {
        show()
        compose.openSheet()

        compose.onNodeWithTag(MIDDLE_TAG).performTouchInput { click(Offset(centerX, top + 10f)) }
        compose.onNodeWithContentDescription("Fermer la feuille").assertExists()

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Fermer la feuille").assertDoesNotExist()
        compose.onNode(OPENS_THE_SHEET).assertExists()
    }

    @Test
    fun `sans recherche branchée, la barre Chercher reste inactive`() {
        show()

        compose.onNodeWithContentDescription("Chercher").assertIsNotEnabled()
    }

    // ---------- conversation ----------

    /** The time during which a message can be taken back goes by, on the phone's own clock. */
    private fun sixSecondsPass() {
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(6_500))
        compose.waitForIdle()
    }

    private fun openTalk(world: FakeWorld = FakeWorld(Moment.Trajet), person: String = "Léa, 1 non lu"): FakeWorld {
        show(world)
        compose.openSheet()
        compose.onNodeWithContentDescription(person).performClick()
        return world
    }

    @Test
    fun `toucher une personne ouvre sa conversation, avec son réseau et ses messages`() {
        openTalk()

        compose.onNodeWithTag(TALK_TAG).assertExists()
        compose.onNode(hasText("Léa") and isHeading() and inTheTalkSheet).assertExists()
        compose.onNode(hasText("WhatsApp via Beeper") and inTheTalkSheet).assertExists()
        compose.onNodeWithContentDescription("Léa : Je suis en terrasse").assertExists()
        compose.onNodeWithContentDescription("Léa : Merci pour le déj !").assertIsDisplayed()
    }

    @Test
    fun `ouvrir la conversation retire son badge et la sort du résumé`() {
        val world = openTalk()

        compose.onNodeWithContentDescription("Fermer la feuille").performClick()

        compose.onNodeWithContentDescription("Léa").assertExists()
        compose.onNodeWithContentDescription("Léa, 1 non lu").assertDoesNotExist()
        assertThat(world.messages.conversations.value.first { it.id == "lea" }.unread).isEqualTo(0)
    }

    @Test
    fun `le bouton Envoyer est désactivé tant que le champ est vide`() {
        openTalk()

        compose.onNodeWithContentDescription("Envoyer").assertIsNotEnabled()
    }

    @Test
    fun `ce qu'on écrit puis envoie s'affiche comme mon message, avec « Annuler l'envoi » dessous`() {
        openTalk()

        compose.onNodeWithContentDescription("Répondre").performTextInput("De rien !")
        compose.onNodeWithContentDescription("Envoyer").performClick()

        compose.onNodeWithContentDescription("Moi : De rien !").assertIsDisplayed()
        compose.onNodeWithText("Annuler l'envoi").assertIsDisplayed()
        compose.onNodeWithContentDescription("Envoyer").assertIsNotEnabled()
    }

    @Test
    fun `« Annuler l'envoi » retire le message et rend son texte au champ`() {
        val world = openTalk()
        compose.onNodeWithContentDescription("Répondre").performTextInput("De rien !")
        compose.onNodeWithContentDescription("Envoyer").performClick()

        compose.onNodeWithText("Annuler l'envoi").performClick()

        compose.onNodeWithContentDescription("Moi : De rien !").assertDoesNotExist()
        compose.onNodeWithText("Annuler l'envoi").assertDoesNotExist()
        compose.onNode(hasSetTextAction() and hasText("De rien !")).assertExists()
        assertThat(world.messages.sent).isEqualTo(emptyList())
    }

    @Test
    fun `« Annuler l'envoi » disparaît de lui-même au bout de six secondes, et le message est parti`() {
        val world = openTalk()
        compose.onNodeWithContentDescription("Répondre").performTextInput("J'arrive")
        compose.onNodeWithContentDescription("Envoyer").performClick()
        compose.onNodeWithText("Annuler l'envoi").assertExists()

        sixSecondsPass()

        compose.onNodeWithText("Annuler l'envoi").assertDoesNotExist()
        assertThat(world.messages.sent).containsExactly("lea: J'arrive")
    }

    @Test
    fun `la feuille ne propose aucune réponse toute faite, seulement le champ où écrire`() {
        openTalk()

        compose.onNodeWithContentDescription("Répondre").assertIsDisplayed()
        listOf("D'accord", "J'arrive", "Je te rappelle").forEach { compose.onNodeWithText(it).assertDoesNotExist() }
    }

    @Test
    fun `dans un groupe, chaque bulle est annoncée avec celui qui l'a écrite`() {
        openTalk(person = "Équipe, 1 non lu")

        compose.onNodeWithContentDescription("Camille : Slides pour 14h dans le drive").assertExists()
        compose.onNode(hasText("Slack via Beeper") and inTheTalkSheet).assertExists()
    }

    @Test
    fun `le bouton Beeper de la feuille demande l'app de messages`() {
        openTalk()

        compose.onNode(hasText("Beeper") and hasClickAction() and inTheTalkSheet).performClick()

        assertThat(opened).containsExactly("app de messages")
    }

    @Test
    fun `la feuille de conversation n'a pas de micro, on y écrit sa réponse`() {
        openTalk()

        compose.onNodeWithContentDescription("Répondre").assertExists()
        compose.onNodeWithContentDescription("Dicter").assertDoesNotExist()
    }

    @Test
    fun `le brouillon est gardé quand la feuille se ferme, et retrouvé à la réouverture`() {
        openTalk()
        compose.onNodeWithContentDescription("Répondre").performTextInput("Avec plaisir")

        compose.onNodeWithContentDescription("Fermer la feuille").performClick()
        compose.onNodeWithTag(TALK_TAG).assertDoesNotExist()
        compose.onNodeWithContentDescription("Léa").performClick()

        compose.onNode(hasSetTextAction() and hasText("Avec plaisir")).assertExists()
    }

    @Test
    fun `sous la feuille de conversation, l'accueil n'est plus lu`() {
        openTalk()

        compose.onNodeWithText("Dans 25 min · 14:00").assertDoesNotExist()
        compose.onNodeWithContentDescription("Chercher").assertDoesNotExist()
    }

    @Test
    fun `si le message ne part pas, la feuille le dit et rend le texte au champ`() {
        openTalk(FakeWorld(Moment.Trajet).apply { messages.refusing = true })
        compose.onNodeWithContentDescription("Répondre").performTextInput("De rien !")
        compose.onNodeWithContentDescription("Envoyer").performClick()

        sixSecondsPass()

        compose.onNodeWithText("Le message n'est pas parti : il est revenu dans le champ.").assertExists()
        compose.onNode(hasSetTextAction() and hasText("De rien !")).assertExists()
    }

    @Test
    fun `« Tous » annonce les non-lus hors favoris et demande l'app de messages`() {
        show(FakeWorld(Moment.Soir))
        compose.openSheet()

        compose.onNodeWithContentDescription("Tous les messages dans Beeper, 2 non lus").performClick()

        assertThat(opened).containsExactly("app de messages")
    }

    @Test
    fun `sans l'accès à Beeper, la ligne résumé le dit et la feuille ouverte propose de le relier`() {
        show(FakeWorld(Moment.Matin).apply { access.refuse(Access.Messages) })
        compose.onNodeWithText("Beeper n'est pas relié").assertExists()
        compose.onNodeWithText("2 non lus · Léa, Tom").assertDoesNotExist()

        compose.openSheet()
        compose.onNodeWithContentDescription("Léa, 1 non lu").assertDoesNotExist()
        compose.onNodeWithText("Relier Beeper").performClick()

        assertThat(opened).containsExactly("demande d'accès Messages")
    }

    @Test
    fun `toucher une commande maison la bascule sur place`() {
        show()
        compose.openSheet()

        compose.onNodeWithContentDescription("Volets").performClick()

        compose.onNodeWithContentDescription("Volets")
            .assertIsOff()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "fermés"))
    }

    @Test
    fun `la scène Cinéma met le Salon à 10 pour cent et ferme les volets d'un seul toucher`() {
        show()
        compose.openSheet()

        compose.onNodeWithContentDescription("Cinéma").performClick()

        compose.onNodeWithContentDescription("Salon")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "10 %"))
        compose.onNodeWithContentDescription("Volets").assertIsOff()
        compose.onNodeWithContentDescription("Fermer la feuille").performClick()
        compose.onNodeWithText("Salon 10 %, Cinéma active").assertExists()
    }

    @Test
    fun `« Tout » demande l'app de la maison`() {
        show()
        compose.openSheet()

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
    fun `toucher la pastille ouvre l'écran Écoute et sa télécommande`() {
        show()

        compose.onNodeWithText("Looped · Kiasmos").performClick()

        compose.onNodeWithContentDescription("Pochette de Looped").assertExists()
        compose.onNodeWithText("Looped").assertExists()
        compose.onNodeWithText("Kiasmos · Blurred EP").assertExists()
        compose.onNodeWithText("Ouvrir dans Spotify").assertExists()
        compose.onNodeWithText("1:12").assertExists()
        compose.onNodeWithText("4:05").assertExists()
        listOf("Précédent", "Pause", "Suivant").forEach { compose.onNodeWithContentDescription(it).assertExists() }
    }

    @Test
    fun `glisser vers la gauche au milieu ouvre l'écran Écoute`() {
        show()

        compose.onNodeWithTag(MIDDLE_TAG).performTouchInput { swipeLeft() }

        compose.onNodeWithContentDescription("Pause").assertExists()
        compose.onNodeWithText("Looped · Kiasmos").assertDoesNotExist()
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

        compose.onNodeWithText("Ouvrir dans Spotify").assertExists()
    }

    @Test
    fun `sur l'écran Écoute, le lecteur est seul, sans en-tête, ni ligne du temps, ni bandeau, ni pastille`() {
        show()

        compose.onNodeWithText("Looped · Kiasmos").performClick()

        compose.onNodeWithText("Stand-up").assertDoesNotExist()
        compose.onNodeWithTag(TIMELINE_TAG).assertDoesNotExist()
        compose.onNode(OPENS_THE_SHEET).assertDoesNotExist()
        compose.onNodeWithText("2 non lus · Léa, Tom").assertDoesNotExist()
        compose.onNodeWithContentDescription("Chercher").assertDoesNotExist()
        compose.onNodeWithText("Looped · Kiasmos").assertDoesNotExist()
        compose.onNodeWithContentDescription("Écran Accueil").assertDoesNotExist()
    }

    @Test
    fun `le geste retour sur l'écran Écoute revient à l'Accueil`() {
        show()
        compose.onNodeWithText("Looped · Kiasmos").performClick()
        compose.waitForIdle()

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()

        compose.onNodeWithText("Looped · Kiasmos").assertExists()
        compose.onHeaderTitle("Stand-up").assertExists()
        compose.onNodeWithContentDescription("Pause").assertDoesNotExist()
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
        compose.onNodeWithText("Nils Frahm · Spaces").assertExists()
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
    fun `« Ouvrir dans Spotify » montre le contenu dans son app`() {
        val world = show()
        compose.onNodeWithText("Looped · Kiasmos").performClick()

        compose.onNodeWithText("Ouvrir dans Spotify").performClick()

        assertThat(world.media.appOpenings).isEqualTo(1)
    }

    @Test
    fun `la télécommande de YouTube n'a pas de bouton Précédent, les autres restent`() {
        val youtube = FakeMediaSource(
            Samples.youtube,
            playing = true,
            actions = setOf(MediaAction.Next, MediaAction.Seek)
        )
        show(media = youtube)
        compose.onNodeWithText("Ramen maison en 20 minutes · Cuisine du soir").performClick()

        compose.onNodeWithContentDescription("Précédent").assertDoesNotExist()
        compose.onNodeWithContentDescription("Pause").assertExists()
        compose.onNodeWithContentDescription("Suivant").assertExists()
    }

    @Test
    fun `un direct n'a pas de fin, sa télécommande dit « En direct » au lieu des temps`() {
        val live = MediaContent("Le 7/9", "France Inter", "Radio France", duration = Duration.ZERO)
        show(media = FakeMediaSource(listOf(live), playing = true))
        compose.onNodeWithText("Le 7/9 · France Inter").performClick()

        compose.onNodeWithText("En direct").assertExists()
        compose.onNodeWithText("0:00").assertDoesNotExist()
        compose.onNodeWithContentDescription("Pause").assertExists()
    }

    @Test
    fun `une vidéo sans artiste connu s'affiche avec son seul titre`() {
        val video = MediaContent("Tiny Desk Concert", artist = "", app = "YouTube", duration = 1080.seconds)
        show(media = FakeMediaSource(listOf(video), playing = true))

        compose.onNodeWithText("Tiny Desk Concert").performClick()

        compose.onNodeWithText("Ouvrir dans YouTube").assertExists()
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
    fun `la feuille retrouve son état en revenant de l'écran Écoute`() {
        show()
        compose.openSheet()
        compose.onNodeWithContentDescription("Écran Écoute").performClick()
        compose.waitForIdle()

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Fermer la feuille").assertExists()
        compose.onNodeWithContentDescription("Léa, 1 non lu").assertExists()
    }

    // ---------- background and battery ----------

    @Test
    fun `avec un contenu chargé, Brume couvre l'écran et cache le fond d'écran du téléphone`() {
        compose.activity.window.addFlags(FLAG_SHOW_WALLPAPER)
        show()

        compose.onNodeWithTag(BRUME_TAG).assertExists()
        compose.onNodeWithTag(VEIL_TAG).assertDoesNotExist()
        assertThat(compose.activity.window.attributes.flags and FLAG_SHOW_WALLPAPER).isEqualTo(0)
    }

    @Test
    fun `sans contenu, le fond d'écran du téléphone reste visible, sous un voile qui garde l'agenda lisible`() {
        compose.activity.window.addFlags(FLAG_SHOW_WALLPAPER)
        show(silent())

        compose.onNodeWithTag(BRUME_TAG).assertDoesNotExist()
        compose.onNodeWithTag(VEIL_TAG).assertExists()
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
    fun `juste après l'installation, l'accueil montre ses états vides, jamais de données inventées`() {
        val home = unconnectedHomeViewModel(hour = 9)
        compose.setContent { BcTheme { HomeRoute(home) } }

        compose.onNodeWithText("Agenda pas encore relié").assertExists()
        compose.onNodeWithText("Relier l'agenda").assertExists()
        compose.onNodeWithText("Beeper n'est pas relié").assertExists()
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
        assertThat(world.weather.forecast.subscriptionCount.value).isEqualTo(0)
    }

    private companion object {
        const val MANY_EVENTS = 20
        const val EVENT_STEP = 40L
    }
}
