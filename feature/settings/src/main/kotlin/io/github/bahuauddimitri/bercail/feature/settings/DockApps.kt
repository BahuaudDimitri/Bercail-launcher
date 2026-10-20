package io.github.bahuauddimitri.bercail.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcLeading
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcListRow
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcPanel
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRowEmphasis
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSectionLabel
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSwitch
import io.github.bahuauddimitri.bercail.core.domain.apps.App
import io.github.bahuauddimitri.bercail.core.domain.apps.MAX_DOCK_APPS

/**
 * The apps of the dock first, each with its place, then every other app from A to Z, each with a switch. Touching
 * an app of the dock takes it out; touching another one puts it at the end of the dock. Five at most: that is the
 * room the band has. To change the order, take an app out and put it back.
 */
@Composable
fun DockScreen(state: SettingsUiState, onToggle: (String) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val dock = state.dock
    val full = dock.size >= MAX_DOCK_APPS
    BcPanel("Dock", onBack, backDescription = "Retour aux réglages", modifier = modifier) {
        BcSectionLabel(if (full) FULL else "Cinq au plus, dans l'ordre où tu les touches")
        dock.forEachIndexed { index, app -> AppRow(app, onToggle, place = index + 1, enabled = true) }
        BcSectionLabel("Autres apps")
        state.apps.filterNot { it in dock }.forEach { app -> AppRow(app, onToggle, place = null, enabled = !full) }
    }
}

/** An app with its switch; in the dock, its place there. */
@Composable
private fun AppRow(app: App, onToggle: (String) -> Unit, place: Int?, enabled: Boolean) {
    BcListRow(
        title = app.name,
        onClick = { if (enabled) onToggle(app.id) },
        subtitle = place?.let { "Place $it" },
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
