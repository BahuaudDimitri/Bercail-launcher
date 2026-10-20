package io.github.bahuauddimitri.bercail.feature.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcLeading
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcListRow
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcPanel
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcReorderList
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcReorderRow
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRowEmphasis
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSectionLabel
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSwitch
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import io.github.bahuauddimitri.bercail.core.domain.apps.App
import io.github.bahuauddimitri.bercail.core.domain.apps.MAX_DOCK_APPS

/**
 * The apps of the dock first, each with its place and a handle, then every other app from A to Z, each with a
 * switch. Touching an app of the dock opens the choice of its icon ([onPickIcon]), its switch takes it out; touching
 * another app puts it at the end of the dock. Five at most: that is the room the band has. Dragging an app of the
 * dock by its handle puts it at another place; screen readers move it with « Monter » and « Descendre ». The other
 * apps have no handle, only its room: every switch stands in the same column.
 */
@Composable
fun DockScreen(
    state: SettingsUiState,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onMove: (from: Int, to: Int) -> Unit = { _, _ -> },
    onPickIcon: (String) -> Unit = {}
) {
    val dock = state.dock
    val full = dock.size >= MAX_DOCK_APPS
    BcPanel("Dock", onBack, backDescription = "Retour aux réglages", modifier = modifier) {
        BcSectionLabel(if (full) FULL else "Cinq au plus · glisse une poignée pour changer l'ordre")
        BcReorderList(
            rows = dock.mapIndexed { index, app ->
                BcReorderRow(app.id) {
                    AppRow(app, onToggle, place = index + 1, enabled = true, modifier = it) { onPickIcon(app.id) }
                }
            },
            onMove = onMove
        )
        BcSectionLabel("Autres apps")
        // An empty room as wide as the handle of the dock's rows: every switch stands in the same column.
        val besideNoHandle = Modifier.padding(end = BcSizes.minTouch)
        state.apps.filterNot { it in dock }.forEach { app ->
            AppRow(app, onToggle, place = null, enabled = !full, modifier = besideNoHandle)
        }
    }
}

/**
 * An app with its switch; in the dock, its place there and that touching it changes its icon, on two lines if the
 * screen is narrow. Touching the row calls [onClick], or else toggles the app.
 */
@Composable
private fun AppRow(
    app: App,
    onToggle: (String) -> Unit,
    place: Int?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    BcListRow(
        title = app.name,
        onClick = onClick ?: { if (enabled) onToggle(app.id) },
        modifier = modifier,
        subtitle = place?.let { "Place $it · toucher pour changer l'icône" },
        subtitleLines = 2,
        leading = BcLeading.Initials(app.name.take(1).uppercase()),
        trailing = {
            BcSwitch(
                checked = place != null,
                onCheckedChange = { onToggle(app.id) },
                contentDescription = null,
                enabled = enabled
            )
        }
    )
}

private const val FULL = "Cinq au plus : retire une app pour en ajouter une autre"

/**
 * « Aucun », then the icon packs installed, each by the name of its app; the one the dock draws with stands out.
 * Touching one chooses it for the dock only: the search, the « Écouter » sheet and the shortcuts keep their icons.
 */
@Composable
fun IconPackScreen(
    state: SettingsUiState,
    onChoose: (String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chosen = state.iconPack
    BcPanel("Pack d'icônes", onBack, backDescription = "Retour aux réglages", modifier = modifier) {
        BcSectionLabel("Pour les apps du dock")
        PackRow(NO_PACK, chosen == null) { onChoose(null) }
        state.iconPacks.forEach { pack -> PackRow(pack.name, pack == chosen) { onChoose(pack.id) } }
    }
}

@Composable
private fun PackRow(name: String, chosen: Boolean, onClick: () -> Unit) {
    BcListRow(
        title = name,
        onClick = onClick,
        leading = BcLeading.Initials(name.take(1).uppercase()),
        emphasis = if (chosen) BcRowEmphasis.Accent else BcRowEmphasis.Normal
    )
}

private const val NO_PACK = "Aucun"
