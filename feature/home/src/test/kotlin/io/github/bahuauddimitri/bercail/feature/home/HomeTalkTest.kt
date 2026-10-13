package io.github.bahuauddimitri.bercail.feature.home

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.messages.via
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Moment
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/** The conversation sheet of the home screen: reading, answering, taking an answer back. Every source is a fake. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeTalkTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** The home screen is visible: someone listens to its state. */
    private fun TestScope.show(home: HomeViewModel): Job = backgroundScope.launch(dispatcher) { home.state.collect {} }

    private fun TestScope.homeOf(world: FakeWorld): HomeViewModel = world.homeViewModel().also { show(it) }

    private val HomeViewModel.talk get() = checkNotNull(state.value.talk) { "No conversation is open." }

    private val HomeViewModel.texts get() = talk.bubbles.map { it.text }

    /** Writes [text] in the field and sends it. */
    private fun HomeViewModel.write(text: String) {
        onDraftChange(text)
        onSend()
    }

    // ---------- reading ----------

    @Test
    fun `tant qu'aucune conversation n'est ouverte, la feuille est fermée et aucun message n'est lu`() =
        runTest(dispatcher) {
            val world = FakeWorld(Moment.Trajet)
            val home = homeOf(world)

            assertThat(home.state.value.talk).isNull()
            assertThat(world.messages.read).isEmpty()
        }

    @Test
    fun `ouvrir une conversation montre qui, par quel réseau, et ses messages, le dernier en bas`() =
        runTest(dispatcher) {
            val home = homeOf(FakeWorld(Moment.Trajet))

            home.onOpenConversation("lea")

            assertThat(home.talk.person.name).isEqualTo("Léa")
            assertThat(home.talk.person.via).isEqualTo("WhatsApp via Beeper")
            assertThat(home.texts).containsExactly("Je suis en terrasse", "Merci pour le déj !")
            assertThat(home.talk.bubbles.map { it.header }).containsExactly("12:58", "13:31")
        }

    @Test
    fun `ouvrir une conversation marque ses messages comme lus, le badge et le résumé se mettent à jour`() =
        runTest(dispatcher) {
            val home = homeOf(FakeWorld(Moment.Matin))
            assertThat(home.state.value.messages.detail).isEqualTo("Léa, Tom")

            home.onOpenConversation("lea")

            assertThat(home.state.value.favorites.first { it.id == "lea" }.unread).isEqualTo(0)
            assertThat(home.state.value.messages.headline).isEqualTo("1 non lu")
            assertThat(home.state.value.messages.detail).isEqualTo("Tom")
        }

    @Test
    fun `un message qui arrive pendant que la conversation est ouverte s'affiche et compte comme lu`() =
        runTest(dispatcher) {
            val world = FakeWorld(Moment.Trajet)
            val home = homeOf(world)
            home.onOpenConversation("lea")

            world.messages.receive("lea", "Tu es où ?")

            assertThat(home.texts.last()).isEqualTo("Tu es où ?")
            assertThat(home.state.value.favorites.first { it.id == "lea" }.unread).isEqualTo(0)
        }

    @Test
    fun `tant que les messages ne sont pas lus dans Beeper, la feuille dit qu'elle les lit`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Trajet).apply { messages.notReadYet("lea") }
        val home = homeOf(world)

        home.onOpenConversation("lea")

        assertThat(home.talk.loading).isTrue()
        assertThat(home.talk.bubbles).isEmpty()
    }

    @Test
    fun `une conversation qui n'existe pas n'ouvre rien`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Matin))

        home.onOpenConversation("inconnue")

        assertThat(home.state.value.talk).isNull()
    }

    @Test
    fun `sans l'accès à Beeper, aucune conversation ne s'ouvre`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Matin).apply { access.refuse(Access.Messages) })

        home.onOpenConversation("lea")

        assertThat(home.state.value.talk).isNull()
    }

    @Test
    fun `fermer la feuille cesse de lire la conversation`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Trajet))
        home.onOpenConversation("lea")

        home.onCloseConversation()

        assertThat(home.state.value.talk).isNull()
    }

    // ---------- answering ----------

    @Test
    fun `sans rien dans le champ, il n'y a rien à envoyer`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Trajet)
        val home = homeOf(world)
        home.onOpenConversation("lea")
        assertThat(home.talk.canSend).isFalse()

        home.onDraftChange("   ")
        home.onSend()
        advanceTimeBy(7.seconds)

        assertThat(home.talk.canSend).isFalse()
        assertThat(world.messages.sent).isEmpty()
    }

    @Test
    fun `un message envoyé s'affiche tout de suite, signé de moi, et le champ se vide`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Trajet))
        home.onOpenConversation("lea")
        home.onDraftChange("  De rien ! ")
        assertThat(home.talk.canSend).isTrue()

        home.onSend()

        assertThat(home.texts.last()).isEqualTo("De rien !")
        assertThat(home.talk.bubbles.last().mine).isTrue()
        assertThat(home.talk.bubbles.last().author).isEqualTo("Moi")
        assertThat(home.talk.draft).isEqualTo("")
    }

    @Test
    fun `pendant six secondes, le message n'est pas encore parti et peut être annulé`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Trajet)
        val home = homeOf(world)
        home.onOpenConversation("lea")
        home.onDraftChange("De rien !")

        home.onSend()
        advanceTimeBy(5.seconds)

        assertThat(world.messages.sent).isEmpty()
        assertThat(home.talk.undoable).isEqualTo(home.talk.bubbles.last().id)
    }

    @Test
    fun `après six secondes, le message part vraiment et ne peut plus être annulé`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Trajet)
        val home = homeOf(world)
        home.onOpenConversation("lea")
        home.onDraftChange("De rien !")

        home.onSend()
        advanceTimeBy(7.seconds)

        assertThat(world.messages.sent).containsExactly("lea: De rien !")
        assertThat(home.talk.undoable).isNull()
        assertThat(home.texts).containsExactly("Je suis en terrasse", "Merci pour le déj !", "De rien !")
    }

    @Test
    fun `quand l'envoi est annulé, rien ne part, la bulle disparaît et le texte revient dans le champ`() =
        runTest(dispatcher) {
            val world = FakeWorld(Moment.Trajet)
            val home = homeOf(world)
            home.onOpenConversation("lea")
            home.onDraftChange("De rien !")
            home.onSend()

            home.onUndoSend()
            advanceTimeBy(7.seconds)

            assertThat(world.messages.sent).isEmpty()
            assertThat(home.texts).containsExactly("Je suis en terrasse", "Merci pour le déj !")
            assertThat(home.talk.draft).isEqualTo("De rien !")
            assertThat(home.talk.undoable).isNull()
        }

    @Test
    fun `un second envoi fait partir le premier tout de suite, et lui seul peut encore être annulé`() =
        runTest(dispatcher) {
            val world = FakeWorld(Moment.Trajet)
            val home = homeOf(world)
            home.onOpenConversation("lea")
            home.write("D'accord")

            home.write("J'arrive")

            assertThat(world.messages.sent).containsExactly("lea: D'accord")
            assertThat(home.talk.undoable).isEqualTo(home.talk.bubbles.last().id)
            assertThat(home.texts.takeLast(2)).containsExactly("D'accord", "J'arrive")
        }

    @Test
    fun `fermer la feuille fait partir tout de suite le message en attente`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Trajet)
        val home = homeOf(world)
        home.onOpenConversation("lea")
        home.write("J'arrive")

        home.onCloseConversation()

        assertThat(world.messages.sent).containsExactly("lea: J'arrive")
    }

    @Test
    fun `tant que Beeper n'a pas rendu le message envoyé, il reste affiché, une seule fois`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Trajet).apply { messages.givesBack = false }
        val home = homeOf(world)
        home.onOpenConversation("lea")
        home.write("J'arrive")

        advanceTimeBy(7.seconds)

        assertThat(world.messages.sent).containsExactly("lea: J'arrive")
        assertThat(home.texts).containsExactly("Je suis en terrasse", "Merci pour le déj !", "J'arrive")
    }

    @Test
    fun `si Beeper ne prend pas le message, la bulle disparaît, le texte revient dans le champ et l'écran le dit`() =
        runTest(dispatcher) {
            val world = FakeWorld(Moment.Trajet).apply { messages.refusing = true }
            val home = homeOf(world)
            home.onOpenConversation("lea")
            home.onDraftChange("De rien !")
            home.onSend()
            assertThat(home.talk.notSent).isFalse()

            advanceTimeBy(7.seconds)

            assertThat(home.texts).containsExactly("Je suis en terrasse", "Merci pour le déj !")
            assertThat(home.talk.draft).isEqualTo("De rien !")
            assertThat(home.talk.notSent).isTrue()
        }

    @Test
    fun `l'avertissement d'un envoi raté s'efface dès qu'on écrit de nouveau`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Trajet).apply { messages.refusing = true }
        val home = homeOf(world)
        home.onOpenConversation("lea")
        home.write("J'arrive")
        advanceTimeBy(7.seconds)
        assertThat(home.talk.notSent).isTrue()

        home.onDraftChange("J")

        assertThat(home.talk.notSent).isFalse()
    }

    // ---------- drafts ----------

    @Test
    fun `le brouillon est gardé par personne à la fermeture`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Matin))
        home.onOpenConversation("lea")
        home.onDraftChange("Oui, 12h30 c'est")
        home.onCloseConversation()

        home.onOpenConversation("tom")
        assertThat(home.talk.draft).isEqualTo("")
        home.onDraftChange("Pas vu")
        home.onCloseConversation()

        home.onOpenConversation("lea")
        assertThat(home.talk.draft).isEqualTo("Oui, 12h30 c'est")
        assertThat(home.state.value.talk).isNotNull()
    }

    @Test
    fun `ce qui est dicté s'ajoute à ce qui est déjà écrit`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Matin))
        home.onOpenConversation("lea")
        home.onDraftChange("Oui")

        home.onDictated("à tout à l'heure")

        assertThat(home.talk.draft).isEqualTo("Oui à tout à l'heure")
    }
}
