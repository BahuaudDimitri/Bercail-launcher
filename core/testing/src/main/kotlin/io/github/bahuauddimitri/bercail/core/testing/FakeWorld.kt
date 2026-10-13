@file:Suppress("MagicNumber") // Sample data copied from the prototype: hours, temperatures, durations.

package io.github.bahuauddimitri.bercail.core.testing

import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaEvent
import io.github.bahuauddimitri.bercail.core.domain.apps.App
import io.github.bahuauddimitri.bercail.core.domain.home.HomeControl
import io.github.bahuauddimitri.bercail.core.domain.media.CoverColors
import io.github.bahuauddimitri.bercail.core.domain.media.MediaContent
import io.github.bahuauddimitri.bercail.core.domain.messages.Conversation
import io.github.bahuauddimitri.bercail.core.domain.messages.Message
import io.github.bahuauddimitri.bercail.core.domain.settings.Settings
import io.github.bahuauddimitri.bercail.core.domain.weather.City
import io.github.bahuauddimitri.bercail.core.domain.weather.Forecast
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration.Companion.seconds

/** The three moments of the prototype: each one is a data set (time, weather, who wrote). */
enum class Moment(
    internal val time: LocalTime,
    internal val forecast: Forecast,
    internal val unread: Map<String, Int>,
    internal val playlist: List<MediaContent>
) {
    Matin(LocalTime.of(8, 10), Skies.clear, mapOf("lea" to 1, "tom" to 1), Samples.spotify),
    Trajet(LocalTime.of(13, 35), Skies.rain, mapOf("lea" to 1, "team" to 1), Samples.spotify),
    Soir(LocalTime.of(20, 40), Skies.wind, mapOf("mum" to 1, "julien" to 2), Samples.youtube)
}

/** Every source of the home screen, faked and filled with the prototype's data at a given [moment]. */
class FakeWorld(moment: Moment = Moment.Matin, playing: Boolean = true) {
    val clock = FakeClock(Samples.day.atTime(moment.time))
    val access = FakeAccess()
    val agenda = FakeAgendaSource(Samples.eventsOn(Samples.day))
    val weather = FakeWeatherSource(moment.forecast)
    val messages = FakeMessagesSource(
        conversations = Samples.conversations(moment.unread),
        messages = Samples.messagesAt(moment),
        now = { clock.now() }
    )
    val home = FakeHomeSource()
    val media = FakeMediaSource(moment.playlist, playing = playing, position = Samples.position)
    val settings = FakeSettingsSource(Samples.settings)
    val apps = FakeAppsSource(Samples.apps)
    val mediaApps = FakeMediaAppsSource(Samples.mediaApps)
    val phone = FakePhone()
    val updates = FakeUpdates()
    val cities = FakeCitySearch(Samples.cities)
}

/** The prototype's sample data. */
object Samples {
    /** Friday 2 October 2026. */
    val day: LocalDate = LocalDate.of(2026, 10, 2)

    val position = 72.seconds

    /** Two cities of the same name, as a search by name finds them. */
    val cities = listOf(
        City("Nantes", "Pays de la Loire, France", 47.21725, -1.55336),
        City("Nantes", "São Paulo, Brésil", -22.62056, -51.23694),
        City("Lyon", "Auvergne-Rhône-Alpes, France", 45.74846, 4.84671)
    )

    val people = listOf(
        Conversation("lea", "Léa", "WhatsApp"),
        Conversation("tom", "Tom", "Signal"),
        Conversation("team", "Équipe", "Slack", group = true),
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

    /** The apps of the prototype's phone that can play sound or video. */
    val mediaApps: List<App> = apps.filter { it.name in setOf("Netflix", "Pocket Casts", "Spotify", "YouTube") }

    val spotify = listOf(
        MediaContent("Looped", "Kiasmos", "Spotify", 245.seconds, CoverColors(CORAL, LAVENDER), appId = "spotify"),
        MediaContent("Says", "Nils Frahm", "Spotify", 528.seconds, CoverColors(BLUE, INDIGO), appId = "spotify"),
        MediaContent("Avril 14th", "Aphex Twin", "Spotify", 125.seconds, CoverColors(MINT, APRICOT), appId = "spotify")
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

    /** What was written at each moment of the prototype, by conversation. */
    fun messagesAt(moment: Moment): Map<String, List<Message>> = when (moment) {
        Moment.Matin -> listOf(
            got("tom", "Tu as vu le match hier ?", 7, 20),
            got("lea", "On se retrouve à 12h30 ?", 7, 52)
        )

        Moment.Trajet -> listOf(
            got("lea", "Je suis en terrasse", 12, 58),
            got("team", "Slides pour 14h dans le drive", 13, 10, author = "Camille"),
            got("lea", "Merci pour le déj !", 13, 31)
        )

        Moment.Soir -> listOf(
            got("team", "Bonne soirée à tous", 17, 45, author = "Camille"),
            got("mum", "Bisous, à dimanche", 19, 55),
            got("julien", "On joue demain ?", 20, 31),
            got("julien", "Vers 15h ?", 20, 33)
        )
    }.groupBy({ it.first }, { it.second })

    private fun got(from: String, text: String, hour: Int, minute: Int, author: String = "") =
        from to Message("$from-$hour-$minute", text, mine = false, author = author, at = day.atTime(hour, minute))

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
