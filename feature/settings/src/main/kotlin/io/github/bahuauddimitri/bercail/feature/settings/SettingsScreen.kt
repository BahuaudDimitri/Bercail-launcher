package io.github.bahuauddimitri.bercail.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcLeading
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcListRow
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcPanel
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSectionLabel
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSegmented
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSettingRow
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSwitch
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.settings.IdleBackground
import io.github.bahuauddimitri.bercail.core.domain.settings.TimelineOrientation

/**
 * « Réglages de Bercail » plugged into the stored settings, with the two lists it opens: the favorite apps and
 * the place of the weather.
 */
@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onAskAccess: (Access) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val place by viewModel.place.state.collectAsStateWithLifecycle()
    var shown by rememberSaveable { mutableStateOf(Shown.Settings) }
    val backToSettings = { shown = Shown.Settings }
    when (shown) {
        Shown.Apps -> FavoriteAppsScreen(state, viewModel::onToggleFavoriteApp, backToSettings, modifier)

        Shown.Place -> PlaceScreen(
            state = state,
            place = place,
            actions = viewModel.place,
            onAskLocation = { onAskAccess(Access.Location) },
            onBack = backToSettings,
            modifier = modifier
        )

        Shown.Settings -> SettingsScreen(
            state = state,
            actions = viewModel,
            onBack = onBack,
            onPickApps = { shown = Shown.Apps },
            modifier = modifier,
            onAskAccess = onAskAccess,
            onPickPlace = { shown = Shown.Place }
        )
    }
}

/** What the settings panel shows: itself, or one of the lists it opens. */
private enum class Shown { Settings, Apps, Place }

/**
 * Bercail's settings: only what already works, the sources to allow and the update of Bercail itself. The favorite
 * people, the home controls and the media app will join them with their waves.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    onBack: () -> Unit,
    onPickApps: () -> Unit,
    modifier: Modifier = Modifier,
    onAskAccess: (Access) -> Unit = {},
    onPickPlace: () -> Unit = {}
) {
    val settings = state.settings
    BcPanel("Réglages de Bercail", onBack, backDescription = "Retour à la recherche", modifier = modifier) {
        BcSectionLabel("Fond d'écran")
        BcSettingRow("Sans musique", subtitle = "La brume du moment, ou ton image qui revient quand le son s'arrête") {
            BcSegmented(
                options = listOf("Brume du moment", "Mon image"),
                selected = if (settings.idleBackground == IdleBackground.Brume) 0 else 1,
                onSelect = { actions.onIdleBackground(if (it == 0) IdleBackground.Brume else IdleBackground.Wallpaper) }
            )
        }
        BcSectionLabel("Agenda")
        BcSettingRow(
            title = state.agenda.title,
            subtitle = state.agenda.detail,
            onClick = if (state.agenda.touchable) ({ onAskAccess(Access.Calendar) }) else null
        )
        BcSettingRow("Ligne du temps", subtitle = "Comment afficher l'approche du prochain rendez-vous") {
            BcSegmented(
                options = listOf("Horizontale", "Verticale"),
                selected = if (settings.timeline == TimelineOrientation.Horizontal) 0 else 1,
                onSelect = {
                    actions.onTimeline(if (it == 0) TimelineOrientation.Horizontal else TimelineOrientation.Vertical)
                }
            )
        }
        BcSectionLabel("Météo")
        BcSettingRow(
            title = state.weather.title,
            subtitle = state.weather.detail,
            onClick = onPickPlace
        )
        BcSectionLabel("Tiroir")
        BcSettingRow("Tiroir", subtitle = "État de départ") {
            BcSegmented(
                options = listOf("Replié", "Ouvert"),
                selected = if (settings.drawerExpandedAtStart) 1 else 0,
                onSelect = { actions.onDrawerExpandedAtStart(it == 1) }
            )
        }
        BcSectionLabel("Recherche")
        BcSettingRow("Apps favorites", subtitle = state.favoriteAppNames, onClick = onPickApps)
        BcSectionLabel("Mise à jour")
        BcSettingRow(
            title = state.update.title,
            subtitle = state.update.detail,
            onClick = if (state.update.touchable) actions::onUpdateTouched else null
        )
        BcSectionLabel("Android")
        BcSettingRow(
            title = "Écran d'accueil par défaut",
            subtitle = "Choisir Bercail, ou revenir à l'ancien",
            onClick = actions::onOpenHomeScreenSettings
        )
        BcSettingRow("Paramètres du téléphone", onClick = actions::onOpenPhoneSettings)
    }
}

/** Every app from A to Z with a switch: the favorites show at the top of the search, in the order chosen. */
@Composable
fun FavoriteAppsScreen(
    state: SettingsUiState,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BcPanel("Apps favorites", onBack, backDescription = "Retour aux réglages", modifier = modifier) {
        state.apps.forEach { app ->
            val favorite = app.id in state.settings.favoriteApps
            BcListRow(
                title = app.name,
                onClick = { onToggle(app.id) },
                leading = BcLeading.Initials(app.name.take(1).uppercase()),
                trailing = { BcSwitch(favorite, onCheckedChange = { onToggle(app.id) }, contentDescription = null) }
            )
        }
    }
}
