package io.github.bahuauddimitri.bercail.core.domain.access

import kotlinx.coroutines.flow.Flow

/** What Bercail reads on the phone only once the user has agreed, on the screen Android shows for it. */
enum class Access { Calendar }

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
