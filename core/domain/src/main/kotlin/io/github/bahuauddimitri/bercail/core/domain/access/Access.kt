package io.github.bahuauddimitri.bercail.core.domain.access

import kotlinx.coroutines.flow.Flow

/** What Bercail reads on the phone only once the user has agreed, on the screen Android shows for it. */
enum class Access { Calendar, Location }

interface AccessSource {
    /** What the user has agreed to so far. It changes when they agree, or change their mind in Android's settings. */
    val granted: Flow<Set<Access>>
}

/** One source in the settings: what it is, where it stands, and whether a touch does something. */
data class SourceLine(val title: String, val detail: String, val touchable: Boolean)

fun agendaSourceLine(allowed: Boolean): SourceLine = if (allowed) {
    SourceLine(AGENDA, "Bercail lit tes rendez-vous, sans rien y changer", touchable = false)
} else {
    SourceLine(AGENDA, "Toucher pour autoriser Bercail à lire tes rendez-vous", touchable = true)
}

private const val AGENDA = "Agenda du téléphone"

/**
 * The weather in the settings: the forecasts are those of [city] when one was chosen, of the place where the
 * phone is otherwise. The line always leads to the choice of the place.
 */
fun weatherSourceLine(allowed: Boolean, city: String? = null): SourceLine {
    val place = city ?: "Ta position approximative".takeIf { allowed }
    val detail = place?.let { "$it · Open-Meteo, une fois par heure au plus" }
    return SourceLine(
        WEATHER,
        detail ?: "Toucher pour choisir : ta position approximative ou une ville",
        touchable = true
    )
}

private const val WEATHER = "Lieu des prévisions"
