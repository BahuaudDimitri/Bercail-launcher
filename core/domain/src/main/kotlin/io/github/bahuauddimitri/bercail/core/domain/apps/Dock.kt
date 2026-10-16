package io.github.bahuauddimitri.bercail.core.domain.apps

/** The room of the dock, in the band of the home screen. */
const val MAX_DOCK_APPS = 5

/** The apps of the dock: those chosen, in the order they were touched, still installed, five at most. */
fun dockOf(installed: List<App>, chosen: List<String>): List<App> =
    chosen.mapNotNull { id -> installed.firstOrNull { it.id == id } }.take(MAX_DOCK_APPS)
