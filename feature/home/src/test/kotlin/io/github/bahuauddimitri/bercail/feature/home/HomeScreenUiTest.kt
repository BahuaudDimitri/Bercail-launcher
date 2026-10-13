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
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.media.MediaAction
import io.github.bahuauddimitri.bercail.core.domain.media.MediaContent
import io.github.bahuauddimitri.bercail.core.domain.media.MediaSource
import io.github.bahuauddimitri.bercail.core.domain.settings.TimelineOrientation
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
        override fun onDictate(onHeard: (String) -> Unit) {
            opened.add("dictée")
            onHeard("à tout à l'heure")
        }
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
    private val inTheListenSheet = hasAnyAncestor(hasTestTag(LISTEN_TAG))
    private val inTheTalkSheet = hasAnyAncestor(hasTestTag(TALK_TAG))

    private fun searchBarBottom() = compose.onNodeWithContentDescription("Chercher").getUnclippedBoundsInRoot().bottom

    // ---------- top of the screen ----------

    @Test
    fun `le matin, l'accueil annonce la météo et le prochain rendez-vous`() {
        show()

        compose.onNodeWithText("11° · Ciel dégagé, 17° cet après-midi").assertExists()
        compose.onNodeWithText("Dans 1 h 20 · 09:30").assertExists()
        compose.onNodeWithText("Stand-up").assertExists()
        compose.onNodeWithText("Ensuite Déjeuner avec Léa à 12:30").assertExists()
    }

    @Test
    fun `le soir, l'accueil annonce le vent et le dernier rendez-vous`() {
        show(FakeWorld(Moment.Soir))

        compose.onNodeWithText("12° · Vent 35 km/h cette nuit").assertExists()
        compose.onNodeWithText("Ciné à la maison").assertExists()
        compose.onNodeWithText("Rien d'autre aujourd'hui").assertExists()
    }

    // ---------- the sheet of the day ----------

    @Test
    fun `toucher l'en-tête ouvre la feuille « Aujourd'hui », avec la date et les rendez-vous du jour`() {
        show()

        compose.onNodeWithText("Stand-up").performClick()

        compose.onNodeWithText("Aujourd'hui").assertExists()
        compose.onNodeWithText("vendredi 2 octobre").assertExists()
        listOf("Stand-up", "Déjeuner avec Léa", "Point design", "Courses", "Ciné à la maison").forEach {
            compose.onNode(hasText(it) and inTheDaySheet).assertExists()
        }
    }

    @Test
    fun `sous la feuille de la journée, l'accueil n'est plus lu`() {
        show()
        compose.onNodeWithText("Stand-up").performClick()

        compose.onNodeWithText("Dans 1 h 20 · 09:30").assertDoesNotExist()
        compose.onNodeWithContentDescription("Chercher").assertDoesNotExist()
    }

    @Test
    fun `la feuille de la journée se referme par sa poignée, et l'accueil revient`() {
        show()
        compose.onNodeWithText("Stand-up").performClick()

        compose.onNodeWithContentDescription("Fermer la feuille").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Aujourd'hui").assertDoesNotExist()
        compose.onNodeWithText("Dans 1 h 20 · 09:30").assertExists()
    }

    @Test
    fun `le geste retour referme la feuille de la journée`() {
        show()
        compose.onNodeWithText("Stand-up").performClick()

        compose.waitForIdle()

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()

        compose.onNodeWithText("Aujourd'hui").assertDoesNotExist()
        compose.onNodeWithText("Dans 1 h 20 · 09:30").assertExists()
    }

    @Test
    fun `toucher un rendez-vous de la feuille l'ouvre dans l'app agenda du téléphone`() {
        val world = show()
        compose.onNodeWithText("Stand-up").performClick()

        compose.onNode(hasText("Point design") and inTheDaySheet).performClick()

        assertThat(world.agenda.opened).containsExactly("event: Point design")
    }

    @Test
    fun `le bouton Agenda de la feuille ouvre l'app agenda du téléphone à la date du jour`() {
        val world = show()
        compose.onNodeWithText("Stand-up").performClick()

        compose.onNodeWithText("Agenda").performClick()

        assertThat(world.agenda.opened).containsExactly("calendar: 2026-10-02")
    }

    @Test
    fun `une journée sans rendez-vous le dit dans la feuille`() {
        show(FakeWorld(Moment.Matin).apply { agenda.set(emptyList()) })

        compose.onNodeWithText("Journée libre").performClick()

        compose.onNodeWithText("Rien de prévu aujourd'hui").assertExists()
    }

    @Test
    fun `sans autorisation, l'accueil propose de relier l'agenda, et la feuille de la demander à Android`() {
        show(FakeWorld(Moment.Matin).apply { access.refuse(Access.Calendar) })
        compose.onNodeWithText("Agenda pas encore relié").assertExists()
        compose.onNodeWithText("Stand-up").assertDoesNotExist()

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
        world.messages.readNow("lea")
        world.messages.readNow("tom")
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

    // ---------- conversation ----------

    /** The time during which a message can be taken back goes by, on the phone's own clock. */
    private fun sixSecondsPass() {
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(6_500))
        compose.waitForIdle()
    }

    private fun openTalk(world: FakeWorld = FakeWorld(Moment.Trajet), person: String = "Léa, 1 non lu"): FakeWorld {
        show(world)
        compose.onNodeWithContentDescription("Déplier le tiroir").performClick()
        compose.onNodeWithContentDescription(person).performClick()
        return world
    }

    @Test
    fun `toucher une personne ouvre sa conversation, avec son réseau et ses messages`() {
        openTalk()

        compose.onNodeWithTag(TALK_TAG).assertExists()
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
        compose.onNodeWithText("J'arrive").performClick()
        compose.onNodeWithText("Annuler l'envoi").assertExists()

        sixSecondsPass()

        compose.onNodeWithText("Annuler l'envoi").assertDoesNotExist()
        assertThat(world.messages.sent).containsExactly("lea: J'arrive")
    }

    @Test
    fun `une réponse rapide part d'un toucher et quitte la liste`() {
        openTalk()

        compose.onNodeWithText("Je te rappelle").performClick()

        compose.onNodeWithContentDescription("Moi : Je te rappelle").assertExists()
        compose.onNode(hasText("Je te rappelle") and hasClickAction()).assertDoesNotExist()
        compose.onNodeWithText("D'accord").assertExists()
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
    fun `le micro demande la dictée, et ce qui est entendu s'ajoute dans le champ`() {
        openTalk()
        compose.onNodeWithContentDescription("Répondre").performTextInput("Oui")

        compose.onNodeWithContentDescription("Dicter").performClick()

        assertThat(opened).containsExactly("dictée")
        compose.onNode(hasSetTextAction() and hasText("Oui à tout à l'heure")).assertExists()
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
        compose.onNodeWithContentDescription("Déplier le tiroir").performClick()

        compose.onNodeWithContentDescription("Tous les messages dans Beeper, 2 non lus").performClick()

        assertThat(opened).containsExactly("app de messages")
    }

    @Test
    fun `sans l'accès à Beeper, la ligne des messages le dit et le tiroir ouvert propose de le relier`() {
        show(FakeWorld(Moment.Matin).apply { access.refuse(Access.Messages) })
        compose.onNodeWithText("Beeper n'est pas relié").assertExists()
        compose.onNodeWithText("2 non lus · Léa, Tom").assertDoesNotExist()

        compose.onNodeWithContentDescription("Déplier le tiroir").performClick()
        compose.onNodeWithContentDescription("Léa, 1 non lu").assertDoesNotExist()
        compose.onNodeWithText("Relier Beeper").performClick()

        assertThat(opened).containsExactly("demande d'accès Messages")
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
    fun `sans contenu, la pastille propose « Écouter… » et ouvre la feuille Écouter`() {
        show(silent())

        compose.onNodeWithText("Écouter…").performClick()

        compose.onNode(hasText("Écouter") and inTheListenSheet).assertExists()
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
    fun `un direct n'a pas de fin, sa télécommande n'a pas de barre de progression`() {
        val live = MediaContent("Le 7/9", "France Inter", "Radio France", duration = Duration.ZERO)
        show(media = FakeMediaSource(listOf(live), playing = true))
        compose.onNodeWithText("Le 7/9 · France Inter").performClick()

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
}
