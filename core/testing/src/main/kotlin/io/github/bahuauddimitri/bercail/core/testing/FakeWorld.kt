@file:Suppress("MagicNumber") // Sample data copied from the prototype: hours, temperatures, durations.

package io.github.bahuauddimitri.bercail.core.testing

import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaEvent
import io.github.bahuauddimitri.bercail.core.domain.apps.App
import io.github.bahuauddimitri.bercail.core.domain.home.HomeControl
import io.github.bahuauddimitri.bercail.core.domain.media.CoverColors
import io.github.bahuauddimitri.bercail.core.domain.media.MediaContent
import io.github.bahuauddimitri.bercail.core.domain.messages.Conversation
import io.github.bahuauddimitri.bercail.core.domain.settings.Settings
import io.github.bahuauddimitri.bercail.core.domain.weather.Weather
import io.github.bahuauddimitri.bercail.core.domain.weather.WeatherCondition
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration.Companion.seconds

/** The three moments of the prototype: each one is a data set (time, weather, who wrote). */
enum class Moment(
    internal val time: LocalTime,
    internal val weather: Weather,
    internal val unread: Map<String, Int>,
    internal val playlist: List<MediaContent>
) {
    Matin(LocalTime.of(8, 10), Weather(11, WeatherCondition.Clear), mapOf("lea" to 1, "tom" to 1), Samples.spotify),
    Trajet(LocalTime.of(13, 35), Weather(14, WeatherCondition.Rain), mapOf("lea" to 1, "team" to 1), Samples.spotify),
    Soir(LocalTime.of(20, 40), Weather(12, WeatherCondition.Wind), mapOf("mum" to 1, "julien" to 2), Samples.youtube)
}

/** Every source of the home screen, faked and filled with the prototype's data at a given [moment]. */
class FakeWorld(moment: Moment = Moment.Matin, playing: Boolean = true) {
    val clock = FakeClock(Samples.day.atTime(moment.time))
    val agenda = FakeAgendaSource(Samples.eventsOn(Samples.day))
    val weather = FakeWeatherSource(moment.weather)
    val messages = FakeMessagesSource(Samples.conversations(moment.unread))
    val home = FakeHomeSource()
    val media = FakeMediaSource(moment.playlist, playing = playing, position = Samples.position)
    val settings = FakeSettingsSource(Samples.settings)
    val apps = FakeAppsSource(Samples.apps)
    val phone = FakePhone()
}

/** The prototype's sample data. */
object Samples {
    /** Friday 2 October 2026. */
    val day: LocalDate = LocalDate.of(2026, 10, 2)

    val position = 72.seconds

    val people = listOf(
        Conversation("lea", "Léa", "WhatsApp"),
        Conversation("tom", "Tom", "Signal"),
        Conversation("team", "Équipe", "Slack"),
        Conversation("mum", "Maman", "SMS"),
        Conversation("julien", "Julien", "Instagram")
    )

    val settings = Settings(
        favoritePeople = listOf("lea", "tom", "team", "mum"),
        favoriteApps = listOf("spotify", "youtube", "beeper", "agenda")
    )

    /** The apps of the prototype's phone. Their id is their name, in lower case. */
    val apps: List<App> = listOf(
        "Agenda", "Appareil photo", "Banque", "Beeper", "Calculatrice", "Chrome", "Contacts", "Drive", "Fichiers",
        "Gmail", "Horloge", "Keep", "Maps", "Météo", "Netflix", "Notion", "Paramètres", "Photos", "Play Store",
        "Pocket Casts", "Spotify", "Strava", "Téléphone", "Uber", "Wallet", "Waze", "YouTube"
    ).map { App(id = it.lowercase(), name = it) }

    val home: List<HomeControl> = listOf(
        HomeControl.Light("salon", "Salon", brightness = 60, on = true),
        HomeControl.Scene("cine", "Cinéma", active = false),
        HomeControl.Shutters("volets", "Volets", open = true),
        HomeControl.Heating("chauf", "Chauffage", target = 18, comfort = false)
    )

    val spotify = listOf(
        MediaContent("Looped", "Kiasmos", "Spotify", 245.seconds, CoverColors(CORAL, LAVENDER)),
        MediaContent("Says", "Nils Frahm", "Spotify", 528.seconds, CoverColors(BLUE, INDIGO)),
        MediaContent("Avril 14th", "Aphex Twin", "Spotify", 125.seconds, CoverColors(MINT, APRICOT))
    )

    val youtube = listOf(
        MediaContent(
            "Ramen maison en 20 minutes",
            "Cuisine du soir",
            "YouTube",
            1080.seconds,
            CoverColors(APRICOT, CORAL)
        ),
        MediaContent(
            "Comment marche un capteur photo",
            "Tech posée",
            "YouTube",
            840.seconds,
            CoverColors(LAVENDER, BLUE)
        )
    )

    fun conversations(unread: Map<String, Int>): List<Conversation> = people.map {
        it.copy(unread = unread[it.id] ?: 0)
    }

    fun eventsOn(day: LocalDate): List<AgendaEvent> = listOf(
        event(day, "Stand-up", LocalTime.of(9, 30), LocalTime.of(10, 0)),
        event(day, "Déjeuner avec Léa", LocalTime.of(12, 30), LocalTime.of(13, 30)),
        event(day, "Point design", LocalTime.of(14, 0), LocalTime.of(15, 0)),
        event(day, "Courses", LocalTime.of(18, 30), LocalTime.of(19, 0)),
        event(day, "Ciné à la maison", LocalTime.of(21, 0), LocalTime.of(23, 0))
    )

    private fun event(day: LocalDate, title: String, start: LocalTime, end: LocalTime) =
        AgendaEvent(id = title, title = title, start = day.atTime(start), end = day.atTime(end))
}

// The cover colors of the prototype's tracks (ARGB).
private const val CORAL = 0xFFF2A49A.toInt()
private const val LAVENDER = 0xFFB4A7F0.toInt()
private const val BLUE = 0xFF9CC4F0.toInt()
private const val INDIGO = 0xFF5B5FC7.toInt()
private const val MINT = 0xFF9FD8C4.toInt()
private const val APRICOT = 0xFFF6C28B.toInt()
