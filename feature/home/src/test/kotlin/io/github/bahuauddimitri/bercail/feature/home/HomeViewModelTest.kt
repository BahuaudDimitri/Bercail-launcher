package io.github.bahuauddimitri.bercail.feature.home

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import io.github.bahuauddimitri.bercail.core.domain.screen.HomeBackground
import io.github.bahuauddimitri.bercail.core.domain.screen.HomePage
import io.github.bahuauddimitri.bercail.core.domain.settings.IdleBackground
import io.github.bahuauddimitri.bercail.core.domain.settings.TimelineOrientation
import io.github.bahuauddimitri.bercail.core.domain.time.DayPart
import io.github.bahuauddimitri.bercail.core.domain.weather.WeatherCondition
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Moment
import io.github.bahuauddimitri.bercail.core.testing.Samples
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/** What the home screen shows, depending on what its sources say. Every source is a fake. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun homeOf(world: FakeWorld) = world.homeViewModel()

    /** The home screen is visible: someone listens to its state. */
    private fun TestScope.show(home: HomeViewModel): Job = backgroundScope.launch(dispatcher) { home.state.collect {} }

    @Test
    fun `avant la première lecture des sources, l'accueil n'a encore rien à montrer`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Matin))
        assertThat(home.state.value.loaded).isFalse()

        show(home)

        assertThat(home.state.value.loaded).isTrue()
    }

    // ---------- top of the screen ----------

    @Test
    fun `le matin, l'en-tête annonce le Stand-up dans 1 h 20 puis le déjeuner`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Matin))
        show(home)

        val glance = home.state.value.glance

        assertThat(glance.countdown).isEqualTo("Dans 1 h 20 · 09:30")
        assertThat(glance.title).isEqualTo("Stand-up")
        assertThat(glance.after).isEqualTo("Ensuite Déjeuner avec Léa à 12:30")
    }

    @Test
    fun `l'en-tête se met à jour quand l'heure avance`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        val home = homeOf(world)
        show(home)

        world.clock.advanceMinutes(35)

        assertThat(home.state.value.glance.countdown).isEqualTo("Dans 45 min · 09:30")
    }

    @Test
    fun `le soir, il ne reste que le ciné, et rien d'autre`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Soir))
        show(home)

        assertThat(home.state.value.glance.title).isEqualTo("Ciné à la maison")
        assertThat(home.state.value.glance.after).isEqualTo("Rien d'autre aujourd'hui")
    }

    @Test
    fun `la ligne météo suit l'heure et le temps qu'il fait`() = runTest(dispatcher) {
        val morning = homeOf(FakeWorld(Moment.Matin))
        val evening = homeOf(FakeWorld(Moment.Soir))
        show(morning)
        show(evening)

        assertThat(morning.state.value.weatherLine).isEqualTo("11° · Ciel dégagé ce matin")
        assertThat(evening.state.value.weatherLine).isEqualTo("12° · Vent cette nuit")
        assertThat(evening.state.value.weather).isEqualTo(WeatherCondition.Wind)
    }

    @Test
    fun `sans météo connue, la ligne météo est absente`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        world.weather.set(null)
        val home = homeOf(world)
        show(home)

        assertThat(home.state.value.weatherLine).isNull()
    }

    @Test
    fun `la ligne du temps porte les cinq rendez-vous du jour, le prochain entouré`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Trajet))
        show(home)

        val marks = home.state.value.dayLine.marks

        assertThat(marks.map { it.time }).containsExactly("09:30", "12:30", "14:00", "18:30", "21:00")
        assertThat(marks.single { it.next }.time).isEqualTo("14:00")
    }

    @Test
    fun `la ligne du temps suit le réglage vertical ou horizontal`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        val home = homeOf(world)
        show(home)
        assertThat(home.state.value.timeline).isEqualTo(TimelineOrientation.Vertical)

        world.settings.update { it.copy(timeline = TimelineOrientation.Horizontal) }

        assertThat(home.state.value.timeline).isEqualTo(TimelineOrientation.Horizontal)
    }

    // ---------- drawer ----------

    @Test
    fun `le tiroir replié résume les messages et la maison`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Matin))
        show(home)

        val state = home.state.value

        assertThat(state.messages.headline).isEqualTo("2 non lus")
        assertThat(state.messages.detail).isEqualTo("Léa, Tom")
        assertThat(state.homeSummary).isEqualTo("Salon 60 %, Volets ouverts")
    }

    @Test
    fun `un message qui arrive met le résumé à jour`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        val home = homeOf(world)
        show(home)

        world.messages.receive("mum")

        assertThat(home.state.value.messages.headline).isEqualTo("3 non lus")
        assertThat(home.state.value.messages.detail).isEqualTo("Léa, Tom, Maman")
    }

    @Test
    fun `le tiroir ouvert montre les 4 favoris, et « Tous » compte les non-lus des autres`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Soir))
        show(home)

        val state = home.state.value

        assertThat(state.favorites.map { it.name }).containsExactly("Léa", "Tom", "Équipe", "Maman")
        assertThat(state.favorites.map { it.unread }).containsExactly(0, 0, 0, 1)
        assertThat(state.otherUnread).isEqualTo(2)
    }

    @Test
    fun `le tiroir démarre replié, et retient ce qu'on en fait`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Matin))
        show(home)
        assertThat(home.state.value.drawerExpanded).isFalse()

        home.onDrawerExpandedChange(true)

        assertThat(home.state.value.drawerExpanded).isTrue()
    }

    @Test
    fun `le tiroir démarre ouvert si le réglage le demande`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        world.settings.update { it.copy(drawerExpandedAtStart = true) }
        val home = homeOf(world)
        show(home)

        assertThat(home.state.value.drawerExpanded).isTrue()
    }

    @Test
    fun `l'état du tiroir est conservé quand on revient de l'écran Écoute`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Matin))
        show(home)
        home.onDrawerExpandedChange(true)

        home.onPageWanted(HomePage.Listen)
        home.onPageWanted(HomePage.Home)

        assertThat(home.state.value.drawerExpanded).isTrue()
    }

    // ---------- house ----------

    @Test
    fun `toucher une commande maison la bascule, et le résumé suit`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Matin))
        show(home)

        home.onToggleControl("volets")

        assertThat(home.state.value.controls.map { it.label })
            .containsExactly("Salon 60 %", "Cinéma prête", "Volets fermés", "Chauffage 18 °C")
        assertThat(home.state.value.homeSummary).isEqualTo("Salon 60 %")
    }

    @Test
    fun `la scène Cinéma met le Salon à 10 pour cent et ferme les volets d'un seul toucher`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Matin))
        show(home)

        home.onToggleControl("cine")

        assertThat(home.state.value.homeSummary).isEqualTo("Salon 10 %, Cinéma active")
    }

    @Test
    fun `tout éteint, le résumé dit « Maison au repos »`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Matin))
        show(home)

        home.onToggleControl("salon")
        home.onToggleControl("volets")

        assertThat(home.state.value.homeSummary).isEqualTo("Maison au repos")
    }

    // ---------- music and screens ----------

    @Test
    fun `en lecture, la pastille porte le titre et l'artiste`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Matin))
        show(home)

        val media = home.state.value.media as MediaState.Loaded

        assertThat(media.content.title).isEqualTo("Looped")
        assertThat(media.content.artist).isEqualTo("Kiasmos")
        assertThat(media.playing).isTrue()
    }

    @Test
    fun `l'accueil démarre sur l'écran Accueil`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Matin))
        show(home)

        assertThat(home.state.value.page).isEqualTo(HomePage.Home)
    }

    @Test
    fun `avec un contenu chargé, on peut passer à l'écran Écoute et en revenir`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Matin))
        show(home)

        home.onPageWanted(HomePage.Listen)
        assertThat(home.state.value.page).isEqualTo(HomePage.Listen)

        home.onPageWanted(HomePage.Home)
        assertThat(home.state.value.page).isEqualTo(HomePage.Home)
    }

    @Test
    fun `l'écran Écoute reste accessible en pause`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Matin, playing = false))
        show(home)

        home.onPageWanted(HomePage.Listen)

        assertThat(home.state.value.page).isEqualTo(HomePage.Listen)
    }

    @Test
    fun `sans contenu, demander l'écran Écoute ne fait rien`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        world.media.stop()
        val home = homeOf(world)
        show(home)

        home.onPageWanted(HomePage.Listen)

        assertThat(home.state.value.page).isEqualTo(HomePage.Home)
    }

    @Test
    fun `quand la musique s'arrête sur l'écran Écoute, on revient à l'Accueil`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        val home = homeOf(world)
        show(home)
        home.onPageWanted(HomePage.Listen)

        world.media.stop()

        assertThat(home.state.value.page).isEqualTo(HomePage.Home)
    }

    @Test
    fun `quand une musique reprend ensuite, on reste sur l'Accueil`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        val home = homeOf(world)
        show(home)
        home.onPageWanted(HomePage.Listen)
        world.media.stop()

        world.media.load(Samples.youtube)

        assertThat(home.state.value.page).isEqualTo(HomePage.Home)
        assertThat(home.state.value.media.isPlaying).isTrue()
    }

    @Test
    fun `le bouton lecture met en pause, puis relance`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Matin))
        show(home)

        home.onPlayPause()
        assertThat(home.state.value.media.isPlaying).isFalse()

        home.onPlayPause()
        assertThat(home.state.value.media.isPlaying).isTrue()
    }

    @Test
    fun `Suivant passe au morceau d'après`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Matin))
        show(home)

        home.onNext()

        assertThat((home.state.value.media as MediaState.Loaded).content.title).isEqualTo("Says")
    }

    @Test
    fun `Précédent revient au début du morceau si la position dépasse 5 secondes`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        val home = homeOf(world)
        show(home)

        home.onPrevious()

        assertThat((home.state.value.media as MediaState.Loaded).content.title).isEqualTo("Looped")
        assertThat(world.media.position.first()).isEqualTo(Duration.ZERO)
    }

    @Test
    fun `Précédent passe au morceau d'avant dans les 5 premières secondes`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        val home = homeOf(world)
        show(home)
        world.media.seekTo(3.seconds)

        home.onPrevious()

        assertThat((home.state.value.media as MediaState.Loaded).content.title).isEqualTo("Avril 14th")
    }

    @Test
    fun `toucher le milieu de la barre de progression place le morceau à sa moitié`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        val home = homeOf(world)
        show(home)

        home.onSeek(0.5f)

        assertThat(world.media.position.first()).isEqualTo(122.seconds)
    }

    @Test
    fun `la position du morceau n'est suivie que si on la regarde`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        val home = homeOf(world)
        show(home)
        assertThat(home.position.value).isEqualTo(Duration.ZERO)

        backgroundScope.launch(dispatcher) { home.position.collect {} }

        assertThat(home.position.value).isEqualTo(72.seconds)
    }

    // ---------- background ----------

    @Test
    fun `avec un contenu chargé, le fond est Brume aux couleurs de la pochette`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Matin))
        show(home)

        assertThat(home.state.value.background).isEqualTo(HomeBackground.Brume(Samples.spotify.first().colors))
    }

    @Test
    fun `quand la musique s'arrête, l'image de l'utilisateur revient`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        val home = homeOf(world)
        show(home)

        world.media.stop()

        assertThat(home.state.value.background).isEqualTo(HomeBackground.Wallpaper)
    }

    @Test
    fun `sans musique, Brume reste aux couleurs du moment si le réglage le demande`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Soir)
        world.media.stop()
        world.settings.update { it.copy(idleBackground = IdleBackground.Brume) }
        val home = homeOf(world)
        show(home)

        assertThat(home.state.value.background).isEqualTo(HomeBackground.Brume(cover = null))
        assertThat(home.state.value.dayPart).isEqualTo(DayPart.Night)
    }

    // ---------- battery ----------

    @Test
    fun `tant que l'accueil n'est pas affiché, aucune source n'est écoutée`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        homeOf(world)

        assertThat(world.listeners()).isEqualTo(0)
    }

    @Test
    fun `quand l'accueil n'est plus affiché, plus aucune source n'est écoutée`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        val home = homeOf(world)
        val visible = show(home)
        assertThat(world.listeners()).isEqualTo(4)

        visible.cancel()

        assertThat(world.listeners()).isEqualTo(0)
    }

    /** How many of the four observable fake sources (weather, messages, house, settings) are being listened to. */
    private fun FakeWorld.listeners(): Int = listOf(
        weather.weather.subscriptionCount.value,
        messages.conversations.subscriptionCount.value,
        home.controls.subscriptionCount.value,
        settings.settings.subscriptionCount.value
    ).sum()
}
