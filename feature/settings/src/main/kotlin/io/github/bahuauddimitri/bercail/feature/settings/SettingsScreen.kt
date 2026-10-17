package io.github.bahuauddimitri.bercail.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcButton
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcButtonVariant
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcLeading
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcListRow
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcPanel
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRowEmphasis
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSectionLabel
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSegmented
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSettingRow
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcText
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextColor
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextStyle
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.access.SourceLine
import io.github.bahuauddimitri.bercail.core.domain.settings.IdleBackground
import io.github.bahuauddimitri.bercail.core.domain.update.UpdateLine

/**
 * « Réglages de Bercail » plugged into the stored settings, with the two lists it opens: the dock and the
 * place of the weather.
 */
@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onAskAccess: (Access) -> Unit = {},
    onOpenAccessPage: (Access) -> Unit = {}
) {
    val access = AccessLinks(ask = onAskAccess, openPage = onOpenAccessPage)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val place by viewModel.place.state.collectAsStateWithLifecycle()
    var shown by rememberSaveable { mutableStateOf(Shown.Settings) }
    val backToSettings = { shown = Shown.Settings }
    when (shown) {
        Shown.Dock -> DockScreen(state, viewModel::onToggleDockApp, backToSettings, modifier)

        Shown.People -> FavoritePeopleScreen(state, viewModel::onToggleFavoritePerson, backToSettings, modifier)

        Shown.MediaApp -> MediaAppScreen(
            state = state,
            onChoose = {
                viewModel.onMainMediaApp(it)
                backToSettings()
            },
            onBack = backToSettings,
            modifier = modifier
        )

        Shown.Place -> PlaceScreen(
            state = state,
            place = place,
            actions = viewModel.place,
            onAskLocation = { onAskAccess(Access.Location) },
            onOpenLocationPage = { onOpenAccessPage(Access.Location) },
            onBack = backToSettings,
            modifier = modifier
        )

        Shown.Settings -> SettingsScreen(
            state = state,
            actions = viewModel,
            onBack = onBack,
            onPickDock = { shown = Shown.Dock },
            modifier = modifier,
            access = access,
            onPickPlace = { shown = Shown.Place },
            onPickMediaApp = { shown = Shown.MediaApp },
            onPickPeople = { shown = Shown.People }
        )
    }
}

/** What the settings panel shows: itself, or one of the lists it opens. */
private enum class Shown { Settings, Dock, Place, MediaApp, People }

/** What the settings ask of Android about an access: the question while it is missing, its page once given. */
class AccessLinks(val ask: (Access) -> Unit = {}, val openPage: (Access) -> Unit = {})

/**
 * What Bercail reads only once allowed: its row says where it stands. While the access is missing, « Autoriser »
 * on the right asks Android for it; once given, « Autorisé » in grey, and a touch on the row opens Android's page
 * where it is taken back.
 */
@Composable
internal fun SourceRow(line: SourceLine, access: Access, links: AccessLinks) {
    val missing = line.touchable
    BcSettingRow(
        title = line.title,
        subtitle = line.detail,
        onClick = if (missing) null else ({ links.openPage(access) }),
        trailing = { AccessState(missing, onAsk = { links.ask(access) }) }
    )
}

/** Where an access stands, on the right of its row: a button to ask for it, or the word that it is given. */
@Composable
internal fun AccessState(missing: Boolean, onAsk: () -> Unit) {
    if (missing) {
        BcButton("Autoriser", onClick = onAsk)
    } else {
        BcText("Autorisé", style = BcTextStyle.BodySmall, color = BcTextColor.Muted, maxLines = 1)
    }
}

/**
 * Bercail's settings: only what already works, the sources to allow and the update of Bercail itself. The home
 * controls will join them with their wave.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    onBack: () -> Unit,
    onPickDock: () -> Unit,
    modifier: Modifier = Modifier,
    access: AccessLinks = AccessLinks(),
    onPickPlace: () -> Unit = {},
    onPickMediaApp: () -> Unit = {},
    onPickPeople: () -> Unit = {}
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
        ListenRows(state, access, onPickMediaApp)
        MessagesRows(state, access, onPickPeople)
        BcSectionLabel("Agenda")
        SourceRow(state.agenda, Access.Calendar, access)
        BcSectionLabel("Météo")
        BcSettingRow(
            title = state.weather.title,
            subtitle = state.weather.detail,
            onClick = onPickPlace
        )
        BcSectionLabel("Bandeau")
        BcSettingRow("Dock", subtitle = state.dockNames, onClick = onPickDock)
        BcSectionLabel("Feuille")
        // A short choice stands on the right of its row.
        BcSettingRow(
            "Gens et maison",
            subtitle = "État de départ de la feuille",
            trailing = {
                BcSegmented(
                    options = listOf("Fermée", "Ouverte"),
                    selected = if (settings.drawerExpandedAtStart) 1 else 0,
                    onSelect = { actions.onDrawerExpandedAtStart(it == 1) }
                )
            }
        )
        BcSectionLabel("Mise à jour")
        UpdateRow(state.update, actions::onUpdateTouched)
        BcSectionLabel("Android")
        BcSettingRow(
            title = "Écran d'accueil par défaut",
            subtitle = "Choisir Bercail, ou revenir à l'ancien",
            onClick = actions::onOpenHomeScreenSettings
        )
        BcSettingRow("Paramètres du téléphone", onClick = actions::onOpenPhoneSettings)
    }
}

/**
 * The update of Bercail: a touch on the row looks again or fetches; a ready version waits for its « Installer »
 * button, filled with the accent, and only that button installs it.
 */
@Composable
private fun UpdateRow(line: UpdateLine, onProceed: () -> Unit) {
    BcSettingRow(
        title = line.title,
        subtitle = line.detail,
        onClick = if (line.touchable) onProceed else null,
        trailing = if (line.installable) {
            { BcButton("Installer", onClick = onProceed, variant = BcButtonVariant.Accent) }
        } else {
            null
        }
    )
}

/** What plays: the access Android asks for, and the media app the « Écouter » sheet shows first. */
@Composable
private fun ListenRows(state: SettingsUiState, access: AccessLinks, onPickMediaApp: () -> Unit) {
    val main = state.mainMediaApp
    BcSectionLabel("Écoute")
    SourceRow(state.media, Access.Notifications, access)
    BcSettingRow(
        title = "App média principale",
        subtitle = main?.let { "${it.name} · en tête quand tu touches « Écouter… »" } ?: NO_MEDIA_APP,
        onClick = if (main != null) onPickMediaApp else null
    )
}

/** The apps that can play sound or video: touching one makes it the main media app. */
@Composable
fun MediaAppScreen(
    state: SettingsUiState,
    onChoose: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BcPanel("App média principale", onBack, backDescription = "Retour aux réglages", modifier = modifier) {
        state.mediaApps.forEach { app ->
            BcListRow(
                title = app.name,
                onClick = { onChoose(app.id) },
                leading = BcLeading.Initials(app.name.take(1).uppercase()),
                emphasis = if (app == state.mainMediaApp) BcRowEmphasis.Accent else BcRowEmphasis.Normal
            )
        }
    }
}

private const val NO_MEDIA_APP = "Aucune app de musique ou de vidéo installée"
