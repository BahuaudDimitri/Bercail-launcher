package io.github.bahuauddimitri.bercail.feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.designsystem.component.BC_REORDER_HANDLE_TAG
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.apps.IconPack
import io.github.bahuauddimitri.bercail.core.domain.apps.MAX_DOCK_APPS
import io.github.bahuauddimitri.bercail.core.domain.settings.IdleBackground
import io.github.bahuauddimitri.bercail.core.domain.settings.Settings
import io.github.bahuauddimitri.bercail.core.domain.update.UpdateStatus
import io.github.bahuauddimitri.bercail.core.domain.update.Version
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Samples
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** « Réglages de Bercail » on a simulated phone: each choice changes the stored settings at once. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PIXEL_9)
class SettingsUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val world = FakeWorld()
    private var closed = 0
    private val asked = mutableListOf<Access>()
    private val pages = mutableListOf<Access>()

    private fun show() {
        val settings =
            SettingsViewModel(
                world.settings,
                world.apps,
                world.phone,
                world.updates,
                world.access,
                world.cities,
                world.mediaApps,
                world.messages
            )
        compose.setContent {
            BcTheme {
                SettingsRoute(
                    settings,
                    onBack = { closed++ },
                    onAskAccess = { asked += it },
                    onOpenAccessPage = { pages += it }
                )
            }
        }
    }

    private val stored: Settings get() = world.settings.settings.value

    @Test
    fun `les réglages s'appellent « Réglages de Bercail » et se ferment par leur flèche`() {
        show()

        compose.onNodeWithText("Réglages de Bercail").assertIsDisplayed()
        compose.onNodeWithContentDescription("Retour à la recherche").performClick()

        assertThat(closed).isEqualTo(1)
    }

    @Test
    fun `le fond sans musique est mon image par défaut, on peut choisir la brume du moment`() {
        show()
        assertThat(stored.idleBackground).isEqualTo(IdleBackground.Wallpaper)

        compose.onNodeWithText("Brume du moment").performClick()

        assertThat(stored.idleBackground).isEqualTo(IdleBackground.Brume)
    }

    @Test
    fun `les Réglages ne proposent plus de coucher la ligne du temps`() {
        show()

        compose.onNodeWithText("Ligne du temps").assertDoesNotExist()
        compose.onNodeWithText("Horizontale").assertDoesNotExist()
    }

    @Test
    fun `sans autorisation, la ligne « Agenda du téléphone » offre « Autoriser », qui la demande à Android`() {
        world.access.refuse(Access.Calendar)
        show()
        compose.onNodeWithText("Toucher pour autoriser Bercail à lire tes rendez-vous").assertExists()
        compose.onNodeWithText("Agenda du téléphone").performScrollTo().assertHasNoClickAction()

        compose.onNodeWithText("Autoriser").performScrollTo().assertHeightIsAtLeast(40.dp).performClick()

        assertThat(asked).containsExactly(Access.Calendar)
        assertThat(pages).isEmpty()
    }

    @Test
    fun `une fois l'agenda autorisé, sa ligne dit « Autorisé » et ouvre la page d'Android pour le retirer`() {
        show()

        compose.onNodeWithText("Bercail lit tes rendez-vous, sans rien y changer").assertExists()
        compose.onNode(hasText("Agenda du téléphone") and hasText("Autorisé")).performScrollTo().performClick()

        assertThat(pages).containsExactly(Access.Calendar)
        assertThat(asked).isEmpty()
    }

    @Test
    fun `une fois tout autorisé, aucune ligne n'offre plus « Autoriser » ni d'interrupteur`() {
        show()

        compose.onAllNodesWithText("Autoriser").assertCountEquals(0)
        compose.onAllNodes(isToggleable()).assertCountEquals(0)
    }

    @Test
    fun `la ligne de l'agenda suit l'autorisation dès qu'elle est donnée`() {
        world.access.refuse(Access.Calendar)
        show()

        world.access.grant(Access.Calendar)

        compose.onNodeWithText("Bercail lit tes rendez-vous, sans rien y changer").assertExists()
    }

    // ---------- messages ----------

    @Test
    fun `sans l'accès à Beeper, la ligne « Messages de Beeper » offre « Autoriser », qui le demande à Android`() {
        world.access.refuse(Access.Messages)
        show()
        compose.onNodeWithText("Toucher pour autoriser Bercail à lire tes conversations et à y répondre")
            .assertExists()

        compose.onNodeWithText("Autoriser").performScrollTo().performClick()

        assertThat(asked).containsExactly(Access.Messages)
    }

    @Test
    fun `Beeper relié, sa ligne dit « Autorisé » et ouvre la page d'Android pour le retirer`() {
        show()

        compose.onNode(hasText("Messages de Beeper") and hasText("Autorisé")).performScrollTo().performClick()

        assertThat(pages).containsExactly(Access.Messages)
    }

    @Test
    fun `Beeper relié sans l'accès aux notifications, la ligne Messages renvoie à celle de la musique`() {
        world.access.refuse(Access.Notifications)
        show()

        compose
            .onNodeWithText(
                "Relié · pour voir un message dès qu'il arrive, autorise aussi « Musique en cours », plus haut"
            )
            .assertExists()
    }

    @Test
    fun `tant que Beeper n'est pas relié, il n'y a pas de favoris à choisir`() {
        world.access.refuse(Access.Messages)
        show()

        compose.onNodeWithText("Personnes favorites").performScrollTo().assertHasNoClickAction()
        compose.onNodeWithText("À choisir une fois Beeper relié").assertExists()
    }

    @Test
    fun `la ligne « Personnes favorites » nomme celles qui sont choisies`() {
        show()

        compose.onNodeWithText("Personnes favorites").performScrollTo().assertExists()
        compose.onNodeWithText("Léa, Tom, Équipe, Maman").assertExists()
    }

    @Test
    fun `sans favori choisi, la ligne dit que ce sont les dernières conversations`() {
        world.settings.change { it.copy(favoritePeople = emptyList()) }
        show()

        compose.onNodeWithText("Les quatre dernières conversations, tant que tu n'as pas choisi")
            .performScrollTo()
            .assertExists()
    }

    @Test
    fun `on choisit ses personnes favorites parmi ses conversations, chacune avec son réseau`() {
        show()
        compose.onNodeWithText("Personnes favorites").performScrollTo().performClick()
        compose.onNodeWithText("Instagram").assertExists()

        compose.onNodeWithText("Tom").performScrollTo().performClick()
        compose.onNodeWithText("Julien").performScrollTo().performClick()

        assertThat(stored.favoritePeople).containsExactly("lea", "team", "mum", "julien")
    }

    @Test
    fun `avec quatre favoris, une cinquième personne ne s'ajoute pas`() {
        show()
        compose.onNodeWithText("Personnes favorites").performScrollTo().performClick()
        compose.onNodeWithText("Quatre au plus : retire une personne pour en ajouter une autre").assertExists()

        compose.onNodeWithText("Julien").performScrollTo().performClick()

        assertThat(stored.favoritePeople).containsExactly("lea", "tom", "team", "mum")
    }

    // ---------- what plays ----------

    @Test
    fun `sans l'accès aux notifications, la ligne « Musique en cours » le demande à Android d'un toucher`() {
        world.access.refuse(Access.Notifications)
        show()
        compose.onNodeWithText("Toucher pour autoriser · Android range ce droit sous « Accès aux notifications »")
            .assertExists()

        compose.onNodeWithText("Musique en cours").performScrollTo().assertHasNoClickAction()
        compose.onNodeWithText("Autoriser").performScrollTo().performClick()

        assertThat(asked).containsExactly(Access.Notifications)
    }

    @Test
    fun `une fois l'accès donné, la ligne « Musique en cours » dit « Autorisé » et ouvre la page pour le retirer`() {
        show()

        compose.onNodeWithText("Bercail voit ce qui joue et sait qu'un message arrive, sans lire aucune notification")
            .assertExists()
        compose.onNode(hasText("Musique en cours") and hasText("Autorisé")).performScrollTo().performClick()

        assertThat(pages).containsExactly(Access.Notifications)
        assertThat(asked).isEmpty()
    }

    @Test
    fun `l'app média principale est Spotify tant qu'on n'en choisit pas d'autre`() {
        show()

        compose.onNodeWithText("App média principale").performScrollTo().assertExists()
        compose.onNodeWithText("Spotify · en tête quand tu touches « Écouter… »").assertExists()
    }

    @Test
    fun `choisir une autre app média principale l'enregistre et ramène aux réglages`() {
        show()
        compose.onNodeWithText("App média principale").performScrollTo().performClick()
        listOf("Netflix", "Pocket Casts", "Spotify", "YouTube").forEach { compose.onNodeWithText(it).assertExists() }

        compose.onNodeWithText("YouTube").performClick()

        assertThat(stored.mainMediaApp).isEqualTo("youtube")
        compose.onNodeWithText("YouTube · en tête quand tu touches « Écouter… »").assertExists()
    }

    @Test
    fun `sans app de musique ou de vidéo installée, la ligne le dit et ne se touche pas`() {
        world.mediaApps.apps.value = emptyList()
        show()

        compose.onNodeWithText("Aucune app de musique ou de vidéo installée").assertExists()
        compose.onNodeWithText("App média principale").performScrollTo().assertHasNoClickAction()
    }

    // ---------- the icon pack of the dock ----------

    private val lines = IconPack("com.natewren.linesfree", "Lines Free")
    private val whicons = IconPack("com.whicons.iconpack", "Whicons")

    private fun openIconPacks() {
        compose.onNodeWithText("Pack d'icônes").performScrollTo().performClick()
    }

    @Test
    fun `la ligne « Pack d'icônes » dit « Aucun » tant qu'aucun pack n'est choisi`() {
        world.apps.iconPacks.value = listOf(lines, whicons)
        show()

        compose.onNodeWithText("Pack d'icônes").performScrollTo().assertExists()
        compose.onNodeWithText("Aucun · le dock garde ses dessins").assertExists()
    }

    @Test
    fun `la liste des packs propose « Aucun » puis chaque pack installé, par son nom`() {
        world.apps.iconPacks.value = listOf(lines, whicons)
        show()

        openIconPacks()

        listOf("Aucun", "Lines Free", "Whicons").forEach { compose.onNodeWithText(it).assertExists() }
    }

    @Test
    fun `choisir un pack l'enregistre et ramène aux réglages, qui le nomment`() {
        world.apps.iconPacks.value = listOf(lines, whicons)
        show()
        openIconPacks()

        compose.onNodeWithText("Lines Free").performClick()

        assertThat(stored.iconPack).isEqualTo("com.natewren.linesfree")
        compose.onNodeWithText("Lines Free · pour les apps du dock").assertExists()
    }

    @Test
    fun `choisir « Aucun » rend au dock ses propres dessins`() {
        world.apps.iconPacks.value = listOf(lines)
        world.settings.change { it.copy(iconPack = lines.id) }
        show()
        openIconPacks()

        compose.onNodeWithText("Aucun").performClick()

        assertThat(stored.iconPack).isNull()
        compose.onNodeWithText("Aucun · le dock garde ses dessins").assertExists()
    }

    @Test
    fun `un pack choisi puis désinstallé laisse la ligne sur « Aucun »`() {
        world.apps.iconPacks.value = listOf(whicons)
        world.settings.change { it.copy(iconPack = lines.id) }
        show()

        compose.onNodeWithText("Pack d'icônes").performScrollTo()
        compose.onNodeWithText("Aucun · le dock garde ses dessins").assertExists()
    }

    // ---------- the place of the weather ----------

    private fun openPlace() {
        show()
        compose.onNodeWithText("Lieu des prévisions").performScrollTo().performClick()
        compose.onNodeWithText("Ma position approximative").assertIsDisplayed()
    }

    private fun searchCity(name: String) {
        compose.onNodeWithContentDescription("Nom de la ville").performTextInput(name)
        compose.onNodeWithText("Chercher cette ville").performClick()
    }

    @Test
    fun `sans position autorisée ni ville, la ligne du lieu invite à choisir`() {
        world.access.refuse(Access.Location)
        show()

        compose.onNodeWithText("Toucher pour choisir : ta position approximative ou une ville").assertExists()
    }

    @Test
    fun `la ligne du lieu ouvre le choix du lieu, sa flèche ramène aux réglages`() {
        openPlace()

        compose.onNodeWithContentDescription("Retour aux réglages").performClick()

        compose.onNodeWithText("Réglages de Bercail").assertIsDisplayed()
        assertThat(closed).isEqualTo(0)
    }

    @Test
    fun `position autorisée et sans ville, sa ligne dit « Autorisé » et ouvre la page d'Android`() {
        openPlace()

        compose.onNodeWithText("Utilisée · envoyée arrondie à Open-Meteo, jamais la position précise").assertExists()
        compose.onNode(hasText("Ma position approximative") and hasText("Autorisé")).performClick()

        assertThat(pages).containsExactly(Access.Location)
        assertThat(asked).isEmpty()
    }

    @Test
    fun `sans autorisation, « Autoriser » sur « Ma position approximative » la demande à Android et la choisit`() {
        world.settings.change { it.copy(weatherCity = Samples.cities.first()) }
        world.access.refuse(Access.Location)
        openPlace()
        compose.onNodeWithText("Ma position approximative").assertHasNoClickAction()

        compose.onNodeWithText("Autoriser").performClick()

        assertThat(asked).containsExactly(Access.Location)
        assertThat(stored.weatherCity).isNull()
    }

    @Test
    fun `chercher une ville la propose avec sa région, la toucher la choisit`() {
        openPlace()

        searchCity("Nantes")
        compose.onNodeWithText("São Paulo, Brésil").assertExists()
        compose.onNodeWithText("Pays de la Loire, France").performClick()

        assertThat(stored.weatherCity).isEqualTo(Samples.cities.first())
        compose.onNodeWithText("Pays de la Loire, France · utilisée").assertExists()
        compose.onNodeWithText("São Paulo, Brésil").assertDoesNotExist()
    }

    @Test
    fun `rien ne part sur le réseau tant que la recherche n'est pas demandée`() {
        openPlace()

        compose.onNodeWithContentDescription("Nom de la ville").performTextInput("Nan")

        assertThat(world.cities.asked).isEqualTo(emptyList())
        compose.onNodeWithText("Chercher cette ville").performClick()
        assertThat(world.cities.asked).containsExactly("Nan")
    }

    @Test
    fun `un nom que personne ne porte le dit`() {
        openPlace()

        searchCity("Zzyzx")

        compose.onNodeWithText("Aucune ville de ce nom").assertExists()
    }

    @Test
    fun `sans réseau, la recherche le dit au lieu de tourner sans fin`() {
        world.cities.reachable = false
        openPlace()

        searchCity("Nantes")

        compose.onNodeWithText("Recherche impossible pour l'instant : vérifie le réseau").assertExists()
    }

    @Test
    fun `une fois la ville choisie, les réglages la nomment sur la ligne du lieu`() {
        world.settings.change { it.copy(weatherCity = Samples.cities.first()) }
        show()

        compose.onNodeWithText("Nantes · Open-Meteo, une fois par heure au plus").assertExists()
    }

    @Test
    fun `revenir à ma position oublie la ville`() {
        world.settings.change { it.copy(weatherCity = Samples.cities.first()) }
        openPlace()
        compose.onNodeWithText("Toucher pour l'utiliser à la place de Nantes").assertExists()

        compose.onNodeWithText("Ma position approximative").performClick()

        assertThat(stored.weatherCity).isNull()
        assertThat(asked).isEqualTo(emptyList())
    }

    @Test
    fun `la feuille des gens et de la maison démarre fermée, on peut la faire démarrer ouverte`() {
        show()
        assertThat(stored.drawerExpandedAtStart).isFalse()

        compose.onNodeWithText("Ouverte").performScrollTo().performClick()

        assertThat(stored.drawerExpandedAtStart).isTrue()
    }

    @Test
    fun `la ligne « Dock » nomme ses apps dans leur ordre et rappelle qu'il en tient cinq`() {
        show()

        compose.onNodeWithText("Dock").performScrollTo().assertExists()
        compose.onNodeWithText("Chrome, Téléphone, Beeper, Appareil photo, Agenda · cinq au plus").assertExists()
    }

    @Test
    fun `sans app dans le dock, la ligne le dit`() {
        world.settings.change { it.copy(dockApps = emptyList()) }
        show()

        compose.onNodeWithText("Aucune pour l'instant · cinq au plus").assertExists()
    }

    @Test
    fun `la liste du dock montre d'abord ses apps avec leur place, puis les autres`() {
        show()

        compose.onNodeWithText("Dock").performScrollTo().performClick()

        compose.onNode(hasText("Chrome") and hasText("Place 1")).assertExists()
        compose.onNode(hasText("Agenda") and hasText("Place 5")).assertExists()
        compose.onNodeWithText("Autres apps").performScrollTo().assertExists()
        compose.onAllNodesWithText("Beeper").assertCountEquals(1)
    }

    @Test
    fun `dans la liste du dock, tous les interrupteurs s'alignent, ceux des apps sans poignée aussi`() {
        show()
        compose.onNodeWithText("Dock").performScrollTo().performClick()

        val switches = compose.onAllNodes(isToggleable(), useUnmergedTree = true).fetchSemanticsNodes()
        val rightEdges = switches.map { it.positionInRoot.x + it.size.width }.distinct()
        // The five apps of the dock and at least one other app.
        assertThat(switches.size > MAX_DOCK_APPS).isTrue()
        assertThat(rightEdges.size).isEqualTo(1)
    }

    @Test
    fun `l'interrupteur d'une app du dock l'en retire, toucher une autre app la met au bout`() {
        show()
        compose.onNodeWithText("Dock").performScrollTo().performClick()

        compose.onNode(isToggleable() and hasAnySibling(hasText("Beeper")), useUnmergedTree = true).performClick()
        compose.onNodeWithText("Spotify").performScrollTo().performClick()

        assertThat(stored.dockApps).containsExactly("chrome", "téléphone", "appareil photo", "agenda", "spotify")
        compose.onNode(hasText("Spotify") and hasText("Place 5")).assertExists()
    }

    @Test
    fun `avec cinq apps dans le dock, une sixième ne s'ajoute pas`() {
        show()
        compose.onNodeWithText("Dock").performScrollTo().performClick()
        compose.onNodeWithText("Cinq au plus : retire une app pour en ajouter une autre").assertExists()

        compose.onNodeWithText("Spotify").performScrollTo().performClick()

        assertThat(stored.dockApps).containsExactly("chrome", "téléphone", "beeper", "appareil photo", "agenda")
    }

    private fun openDock() = compose.onNodeWithText("Dock").performScrollTo().performClick()

    @Test
    fun `glisser une app du dock par sa poignée change sa place, enregistrée tout de suite`() {
        show()
        openDock()
        val row = compose.onNodeWithText("Chrome").fetchSemanticsNode().size.height

        compose.onNodeWithTag(BC_REORDER_HANDLE_TAG + "chrome").performTouchInput {
            down(center)
            moveBy(Offset(0f, viewConfiguration.touchSlop + 1f))
            moveBy(Offset(0f, 2f * row))
            up()
        }

        assertThat(stored.dockApps).containsExactly("téléphone", "beeper", "chrome", "appareil photo", "agenda")
        compose.onNode(hasText("Chrome") and hasText("Place 3")).assertExists()
    }

    @Test
    fun `le nouvel ordre du dock est celui que sa ligne des réglages nomme, et que l'accueil suit`() {
        show()
        openDock()

        compose.runOnIdle { customAction("Agenda", "Monter")() }
        compose.onNodeWithContentDescription("Retour aux réglages").performClick()

        assertThat(stored.dockApps).containsExactly("chrome", "téléphone", "beeper", "agenda", "appareil photo")
        compose.onNodeWithText("Chrome, Téléphone, Beeper, Agenda, Appareil photo · cinq au plus")
            .performScrollTo()
            .assertExists()
    }

    @Test
    fun `chaque app du dock offre « Monter » et « Descendre » au lecteur d'écran, sauf au bord`() {
        show()
        openDock()

        assertThat(customActionsOf("Chrome")).containsExactly("Descendre")
        assertThat(customActionsOf("Beeper")).containsExactly("Monter", "Descendre")
        assertThat(customActionsOf("Agenda")).containsExactly("Monter")

        compose.runOnIdle { customAction("Beeper", "Descendre")() }

        assertThat(stored.dockApps).containsExactly("chrome", "téléphone", "appareil photo", "beeper", "agenda")
    }

    @Test
    fun `les apps hors du dock n'ont ni poignée ni actions pour changer de place`() {
        world.settings.change { it.copy(dockApps = listOf("chrome")) }
        show()
        openDock()

        compose.onNodeWithTag(BC_REORDER_HANDLE_TAG + "chrome").assertExists()
        compose.onNodeWithText("Spotify").performScrollTo()
        compose.onNodeWithTag(BC_REORDER_HANDLE_TAG + "spotify").assertDoesNotExist()
        assertThat(customActionsOf("Spotify")).isEmpty()
    }

    private fun customActionsOf(name: String): List<String> = compose.onNodeWithText(name).fetchSemanticsNode()
        .config.getOrNull(SemanticsActions.CustomActions).orEmpty().map { it.label }

    private fun customAction(name: String, label: String): () -> Boolean = compose.onNodeWithText(name)
        .fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions).orEmpty()
        .first { it.label == label }.action

    @Test
    fun `le retour de la liste du dock ramène aux réglages`() {
        show()
        compose.onNodeWithText("Dock").performScrollTo().performClick()

        compose.onNodeWithContentDescription("Retour aux réglages").performClick()

        compose.onNodeWithText("Réglages de Bercail").assertIsDisplayed()
        assertThat(closed).isEqualTo(0)
    }

    @Test
    fun `les réglages renvoient vers le choix de l'écran d'accueil et les Paramètres du téléphone`() {
        show()

        compose.onNodeWithText("Écran d'accueil par défaut").performScrollTo().performClick()
        compose.onNodeWithText("Paramètres du téléphone").performScrollTo().performClick()

        assertThat(world.phone.visits).containsExactly("home screen settings", "settings")
    }

    @Test
    fun `tant que les réglages ne sont pas affichés, la liste des apps n'est pas écoutée`() {
        SettingsViewModel(
            world.settings,
            world.apps,
            world.phone,
            world.updates,
            world.access,
            world.cities,
            world.mediaApps,
            world.messages
        )

        assertThat(world.apps.apps.subscriptionCount.value).isEqualTo(0)
    }

    @Test
    fun `la ligne de mise à jour donne la version installée et propose de vérifier`() {
        show()

        compose.onNodeWithText("Bercail 0.8.1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("À jour · toucher pour vérifier").assertExists()
    }

    @Test
    fun `toucher la ligne de mise à jour lance la vérification`() {
        show()

        compose.onNodeWithText("Bercail 0.8.1").performScrollTo().performClick()

        assertThat(world.updates.proceeded).isEqualTo(1)
    }

    @Test
    fun `la ligne suit la mise à jour, jusqu'à la version prête à installer`() {
        show()

        world.updates.status.value = UpdateStatus.Downloading(Version(0, 9, 0))
        compose.onNodeWithText("Téléchargement de la version 0.9.0…").assertExists()

        world.updates.status.value = UpdateStatus.Ready(Version(0, 9, 0), needsUser = true)
        compose.onNodeWithText("Version 0.9.0 prête").assertExists()
        compose.onNodeWithText("Installer").assertExists()
    }

    @Test
    fun `une version prête s'installe par son bouton « Installer », plus en touchant la ligne`() {
        world.updates.status.value = UpdateStatus.Ready(Version(0, 9, 0), needsUser = true)
        show()

        compose.onNodeWithText("Bercail 0.8.1").performScrollTo().assertHasNoClickAction()
        assertThat(world.updates.proceeded).isEqualTo(0)

        compose.onNodeWithText("Installer").performScrollTo().assertHeightIsAtLeast(40.dp).performClick()
        assertThat(world.updates.proceeded).isEqualTo(1)
    }

    @Test
    fun `tant qu'aucune version n'est prête, la ligne de mise à jour n'offre pas « Installer »`() {
        world.updates.status.value = UpdateStatus.Waiting(Version(0, 9, 0))
        show()

        compose.onNodeWithText("Version 0.9.0 disponible · toucher pour la télécharger").performScrollTo()
        compose.onNodeWithText("Installer").assertDoesNotExist()
    }

    @Test
    fun `pendant un téléchargement, toucher la ligne ne relance rien`() {
        world.updates.status.value = UpdateStatus.Downloading(Version(0, 9, 0))
        show()

        compose.onNodeWithText("Bercail 0.8.1").performScrollTo().performClick()

        assertThat(world.updates.proceeded).isEqualTo(0)
    }

    @Test
    fun `tant que les réglages ne sont pas affichés, la mise à jour n'est pas écoutée`() {
        SettingsViewModel(
            world.settings,
            world.apps,
            world.phone,
            world.updates,
            world.access,
            world.cities,
            world.mediaApps,
            world.messages
        )

        assertThat(world.updates.status.subscriptionCount.value).isEqualTo(0)
    }
}
