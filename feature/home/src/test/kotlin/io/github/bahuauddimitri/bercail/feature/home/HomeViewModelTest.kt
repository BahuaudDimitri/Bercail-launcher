package io.github.bahuauddimitri.bercail.feature.home

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.agenda.DayRowState
import io.github.bahuauddimitri.bercail.core.domain.media.LastHeard
import io.github.bahuauddimitri.bercail.core.domain.media.MediaAction
import io.github.bahuauddimitri.bercail.core.domain.media.MediaSource
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import io.github.bahuauddimitri.bercail.core.domain.screen.HomeBackground
import io.github.bahuauddimitri.bercail.core.domain.screen.HomePage
import io.github.bahuauddimitri.bercail.core.domain.settings.IdleBackground
import io.github.bahuauddimitri.bercail.core.domain.time.DayPart
import io.github.bahuauddimitri.bercail.core.domain.weather.WeatherCondition
import io.github.bahuauddimitri.bercail.core.testing.FakeMediaSource
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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

    private fun homeOf(world: FakeWorld, media: MediaSource = world.media) = world.homeViewModel(media)

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

        assertThat(morning.state.value.weatherLine).isEqualTo("11° · Ciel dégagé, 17° cet après-midi")
        assertThat(evening.state.value.weatherLine).isEqualTo("12° · Vent 35 km/h cette nuit")
        assertThat(evening.state.value.weather).isEqualTo(WeatherCondition.Wind)
    }

    @Test
    fun `la ligne météo suit l'heure qui avance, sans nouvelle prévision`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Trajet)
        val home = homeOf(world)
        show(home)
        assertThat(home.state.value.weatherLine).isEqualTo("14° · Pluie fine jusqu'à 16 h")
        assertThat(home.state.value.weather).isEqualTo(WeatherCondition.Rain)

        world.clock.advanceMinutes(150)

        assertThat(home.state.value.weatherLine).isEqualTo("14° · Ciel couvert cet après-midi")
        assertThat(home.state.value.weather).isEqualTo(WeatherCondition.Clear)
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

    // ---------- drawer ----------

    @Test
    fun `le bandeau résume les messages et la maison`() = runTest(dispatcher) {
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
    fun `la feuille ouverte montre les 4 favoris, et « Tous » compte les non-lus des autres`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Soir))
        show(home)

        val state = home.state.value

        assertThat(state.favorites.map { it.name }).containsExactly("Léa", "Tom", "Équipe", "Maman")
        assertThat(state.favorites.map { it.unread }).containsExactly(0, 0, 0, 1)
        assertThat(state.otherUnread).isEqualTo(2)
    }

    @Test
    fun `sans l'accès à Beeper, aucune conversation n'est montrée, quoi que dise la source`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin).apply { access.refuse(Access.Messages) }
        val home = homeOf(world)
        show(home)

        val state = home.state.value

        assertThat(state.messagesAllowed).isFalse()
        assertThat(state.messages.senders).isEmpty()
        assertThat(state.favorites).isEmpty()
        assertThat(state.otherUnread).isEqualTo(0)
    }

    @Test
    fun `dès que Beeper est relié, les conversations arrivent`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin).apply { access.refuse(Access.Messages) }
        val home = homeOf(world)
        show(home)

        world.access.grant(Access.Messages)

        assertThat(home.state.value.messagesAllowed).isTrue()
        assertThat(home.state.value.messages.detail).isEqualTo("Léa, Tom")
    }

    @Test
    fun `tant qu'aucun favori n'est choisi, les tuiles sont les quatre conversations les plus récentes`() =
        runTest(dispatcher) {
            val world = FakeWorld(Moment.Soir).apply { settings.change { it.copy(favoritePeople = emptyList()) } }
            val home = homeOf(world)
            show(home)

            val state = home.state.value

            assertThat(state.favorites.map { it.name }).containsExactly("Léa", "Tom", "Équipe", "Maman")
            // Julien, who wrote twice, is not among them: « Tous » counts his messages.
            assertThat(state.otherUnread).isEqualTo(2)
        }

    @Test
    fun `la feuille démarre fermée, et retient ce qu'on en fait`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Matin))
        show(home)
        assertThat(home.state.value.drawerExpanded).isFalse()

        home.onDrawerExpandedChange(true)

        assertThat(home.state.value.drawerExpanded).isTrue()
    }

    @Test
    fun `la feuille démarre ouverte si le réglage le demande`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        world.settings.change { it.copy(drawerExpandedAtStart = true) }
        val home = homeOf(world)
        show(home)

        assertThat(home.state.value.drawerExpanded).isTrue()
    }

    @Test
    fun `l'état de la feuille est conservé quand on revient de l'écran Écoute`() = runTest(dispatcher) {
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

    // ---------- the access to what plays, and the « Écouter » sheet ----------

    @Test
    fun `sans l'accès aux notifications, rien de ce qui joue n'est montré`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        world.access.refuse(Access.Notifications)
        val home = homeOf(world)
        show(home)

        assertThat(home.state.value.mediaAllowed).isFalse()
        assertThat(home.state.value.media).isEqualTo(MediaState.None)
        assertThat(home.state.value.background).isEqualTo(HomeBackground.Wallpaper)
    }

    @Test
    fun `dès que l'accès est donné, ce qui joue apparaît`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        world.access.refuse(Access.Notifications)
        val home = homeOf(world)
        show(home)

        world.access.grant(Access.Notifications)

        assertThat(home.state.value.mediaAllowed).isTrue()
        assertThat(home.state.value.media.isPlaying).isTrue()
    }

    @Test
    fun `la feuille Écouter est fermée au départ, s'ouvre et se referme à la demande`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        world.media.stop()
        val home = homeOf(world)
        show(home)
        assertThat(home.state.value.listenOpen).isFalse()

        home.onListenOpenChange(true)
        assertThat(home.state.value.listenOpen).isTrue()

        home.onListenOpenChange(false)
        assertThat(home.state.value.listenOpen).isFalse()
    }

    @Test
    fun `la feuille Écouter se referme d'elle-même dès qu'un contenu est chargé`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        world.media.stop()
        val home = homeOf(world)
        show(home)
        home.onListenOpenChange(true)

        world.media.load(Samples.spotify)

        assertThat(home.state.value.listenOpen).isFalse()
        assertThat(home.state.value.media.isPlaying).isTrue()
    }

    @Test
    fun `« Ouvrir dans l'app » montre le contenu dans son app`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        val home = homeOf(world)
        show(home)

        home.onOpenMediaApp()

        assertThat(world.media.appOpenings).isEqualTo(1)
    }

    @Test
    fun `Précédent passe au morceau d'avant, même après 5 secondes, si l'app ne sait pas revenir au début`() =
        runTest(dispatcher) {
            val media = FakeMediaSource(
                Samples.spotify,
                playing = true,
                position = 72.seconds,
                actions = setOf(MediaAction.Previous, MediaAction.Next)
            )
            val home = homeOf(FakeWorld(Moment.Matin), media)
            show(home)

            home.onPrevious()

            assertThat((home.state.value.media as MediaState.Loaded).content.title).isEqualTo("Avril 14th")
        }

    // ---------- the media apps of the « Écouter » sheet ----------

    @Test
    fun `la feuille Écouter liste les apps média, Spotify en tête tant qu'aucune autre n'est choisie`() =
        runTest(dispatcher) {
            val world = FakeWorld(Moment.Matin)
            world.media.stop()
            val home = homeOf(world)
            show(home)

            home.onListenOpenChange(true)

            val rows = home.state.value.mediaApps
            assertThat(rows.map { it.app.name }).containsExactly("Spotify", "Netflix", "Pocket Casts", "YouTube")
            assertThat(rows.first().main).isTrue()
        }

    @Test
    fun `l'app média principale choisie dans les réglages passe en tête de la feuille`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        world.media.stop()
        world.settings.change { it.copy(mainMediaApp = "youtube") }
        val home = homeOf(world)
        show(home)

        home.onListenOpenChange(true)

        assertThat(home.state.value.mediaApps.first().app.name).isEqualTo("YouTube")
    }

    @Test
    fun `feuille fermée, les apps média du téléphone ne sont pas écoutées`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        world.media.stop()
        val home = homeOf(world)
        show(home)
        assertThat(world.mediaApps.apps.subscriptionCount.value).isEqualTo(0)

        home.onListenOpenChange(true)
        assertThat(world.mediaApps.apps.subscriptionCount.value).isEqualTo(1)

        home.onListenOpenChange(false)
        assertThat(world.mediaApps.apps.subscriptionCount.value).isEqualTo(0)
        assertThat(home.state.value.mediaApps).isEmpty()
    }

    @Test
    fun `ce qui joue est retenu, et l'app principale propose ensuite de le reprendre`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        val home = homeOf(world)
        show(home)
        assertThat(world.mediaApps.lastHeard.value).isEqualTo(LastHeard("spotify", "Looped"))

        world.media.stop()
        home.onListenOpenChange(true)

        assertThat(home.state.value.mediaApps.first().resume).isEqualTo("Reprendre « Looped »")
    }

    @Test
    fun `choisir une app dans la feuille Écouter l'ouvre`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        world.media.stop()
        val home = homeOf(world)
        show(home)

        home.onOpenApp("youtube")

        assertThat(world.apps.opened).containsExactly("youtube")
    }

    @Test
    fun `le bouton lecture de l'app principale relance la musique sans quitter l'accueil`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        world.media.stop()
        world.media.lastPlayed = Samples.spotify
        val home = homeOf(world)
        show(home)
        home.onListenOpenChange(true)

        home.onResume("spotify")
        advanceTimeBy(5.seconds)
        runCurrent()

        assertThat(home.state.value.media.isPlaying).isTrue()
        assertThat(home.state.value.listenOpen).isFalse()
        assertThat(world.apps.opened).isEmpty()
    }

    @Test
    fun `si rien ne repart dans les deux secondes, l'app principale s'ouvre`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        world.media.stop()
        val home = homeOf(world)
        show(home)
        home.onListenOpenChange(true)

        home.onResume("spotify")
        assertThat(world.apps.opened).isEmpty()
        advanceTimeBy(2.seconds)
        runCurrent()

        assertThat(world.apps.opened).containsExactly("spotify")
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
        world.settings.change { it.copy(idleBackground = IdleBackground.Brume) }
        val home = homeOf(world)
        show(home)

        assertThat(home.state.value.background).isEqualTo(HomeBackground.Brume(cover = null))
        assertThat(home.state.value.dayPart).isEqualTo(DayPart.Night)
    }

    // ---------- the agenda and the sheet of the day ----------

    @Test
    fun `sans le droit de lire l'agenda, l'en-tête propose de le relier, sans rendez-vous ni journée libre`() =
        runTest(dispatcher) {
            val world = FakeWorld(Moment.Matin).apply { access.refuse(Access.Calendar) }
            val home = homeOf(world)
            show(home)

            assertThat(home.state.value.agendaAllowed).isFalse()
            assertThat(home.state.value.glance.title).isEqualTo("Relier l'agenda")
            assertThat(home.state.value.dayLine.marks).isEmpty()
            assertThat(home.state.value.dayRows).isEmpty()
        }

    @Test
    fun `dès que l'autorisation est donnée, l'en-tête annonce le prochain rendez-vous`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin).apply { access.refuse(Access.Calendar) }
        val home = homeOf(world)
        show(home)

        world.access.grant(Access.Calendar)

        assertThat(home.state.value.agendaAllowed).isTrue()
        assertThat(home.state.value.glance.title).isEqualTo("Stand-up")
    }

    @Test
    fun `la feuille de la journée est fermée au départ`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Matin))
        show(home)

        assertThat(home.state.value.dayOpen).isFalse()
    }

    @Test
    fun `la feuille de la journée donne la date et les rendez-vous, le prochain mis en avant`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Trajet))
        show(home)

        home.onDayOpenChange(true)

        val state = home.state.value
        assertThat(state.dayOpen).isTrue()
        assertThat(state.dayDate).isEqualTo("vendredi 2 octobre")
        assertThat(state.dayRows.map { "${it.time} ${it.title}" }).containsExactly(
            "09:30 Stand-up",
            "12:30 Déjeuner avec Léa",
            "14:00 Point design",
            "18:30 Courses",
            "21:00 Ciné à la maison"
        )
        assertThat(state.dayRows.map { it.state }).containsExactly(
            DayRowState.Past,
            DayRowState.Past,
            DayRowState.Next,
            DayRowState.Coming,
            DayRowState.Coming
        )
    }

    @Test
    fun `la feuille suit l'heure, un rendez-vous terminé s'atténue`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        val home = homeOf(world)
        show(home)
        home.onDayOpenChange(true)
        assertThat(home.state.value.dayRows.first().state).isEqualTo(DayRowState.Next)

        world.clock.advanceMinutes(120)

        assertThat(home.state.value.dayRows.first().state).isEqualTo(DayRowState.Past)
    }

    @Test
    fun `venue de la recherche, la feuille s'ouvre sur le rendez-vous choisi, l'anneau reste sur le prochain`() =
        runTest(dispatcher) {
            val home = homeOf(FakeWorld(Moment.Matin))
            show(home)

            home.onDayOpenChange(true, focus = "Courses")

            assertThat(home.state.value.dayFocus).isEqualTo("Courses")
            assertThat(home.state.value.dayRows.single { it.state == DayRowState.Next }.title).isEqualTo("Stand-up")
        }

    @Test
    fun `fermer la feuille oublie le rendez-vous choisi`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Matin))
        show(home)
        home.onDayOpenChange(true, focus = "Courses")

        home.onDayOpenChange(false)
        home.onDayOpenChange(true)

        assertThat(home.state.value.dayFocus).isNull()
        assertThat(home.state.value.dayRows.single { it.state == DayRowState.Next }.title).isEqualTo("Stand-up")
    }

    @Test
    fun `toucher un rendez-vous de la feuille l'ouvre dans l'app agenda du téléphone`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        val home = homeOf(world)
        show(home)

        home.onOpenEvent("Point design")

        assertThat(world.agenda.opened).containsExactly("event: Point design")
    }

    @Test
    fun `le bouton Agenda de la feuille ouvre l'app agenda à la date du jour`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Matin)
        val home = homeOf(world)
        show(home)

        home.onOpenCalendar()

        assertThat(world.agenda.opened).containsExactly("calendar: 2026-10-02")
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
        weather.forecast.subscriptionCount.value,
        messages.conversations.subscriptionCount.value,
        home.controls.subscriptionCount.value,
        settings.settings.subscriptionCount.value
    ).sum()
}
