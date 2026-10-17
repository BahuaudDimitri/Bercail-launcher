package io.github.bahuauddimitri.bercail.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcButton
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcListRow
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcPanel
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSearchField
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSectionLabel
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSettingRow
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcText
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextColor
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextStyle
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import io.github.bahuauddimitri.bercail.core.domain.settings.SettingsSource
import io.github.bahuauddimitri.bercail.core.domain.weather.City
import io.github.bahuauddimitri.bercail.core.domain.weather.CitySearch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where a search for a city stands. */
sealed interface CityResults {
    data object Idle : CityResults

    data object Searching : CityResults

    data class Found(val cities: List<City>) : CityResults

    data object NoneFound : CityResults

    /** The search could not be made: no network, most of the time. */
    data object Unreachable : CityResults
}

/** What the choice of the weather's place shows besides the settings: the name being typed and what it finds. */
data class PlaceUiState(val query: String = "", val results: CityResults = CityResults.Idle)

/** What the choice of the weather's place does. */
interface PlaceActions {
    fun onCityQuery(text: String)

    /** Looks for the typed name. Nothing leaves the phone before. */
    fun onSearchCity()
    fun onChooseCity(city: City)

    /** Forgets the chosen city: the weather is again the one of the place where the phone is. */
    fun onUsePhonePosition()
}

/** Chooses the place of the weather: a city found by its name, or the place where the phone is. */
class PlacePicker(
    private val settings: SettingsSource,
    private val cities: CitySearch,
    private val scope: CoroutineScope
) : PlaceActions {
    private val shown = MutableStateFlow(PlaceUiState())
    val state: StateFlow<PlaceUiState> = shown

    override fun onCityQuery(text: String) = shown.update { PlaceUiState(query = text) }

    override fun onSearchCity() {
        val typed = shown.value.query.trim()
        if (typed.isEmpty()) return
        shown.update { it.copy(results = CityResults.Searching) }
        scope.launch {
            val found = cities.find(typed)
            val results = when {
                found == null -> CityResults.Unreachable
                found.isEmpty() -> CityResults.NoneFound
                else -> CityResults.Found(found)
            }
            // An answer that comes after the name was changed is no longer the answer.
            shown.update { if (it.query.trim() == typed) it.copy(results = results) else it }
        }
    }

    override fun onChooseCity(city: City) {
        shown.value = PlaceUiState()
        scope.launch { settings.update { it.copy(weatherCity = city) } }
    }

    override fun onUsePhonePosition() {
        scope.launch { settings.update { it.copy(weatherCity = null) } }
    }
}

/**
 * The place of the weather: the approximate position of the phone, or a city found by its name. The name is only
 * sent when the search is asked for.
 */
@Composable
fun PlaceScreen(
    state: SettingsUiState,
    place: PlaceUiState,
    actions: PlaceActions,
    onAskLocation: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenLocationPage: () -> Unit = {}
) {
    val city = state.settings.weatherCity
    BcPanel("Lieu des prévisions", onBack, backDescription = "Retour aux réglages", modifier = modifier) {
        PositionRow(state, actions, onAskLocation, onOpenLocationPage)
        BcSectionLabel("Ou une ville")
        if (city != null) BcSettingRow(city.name, subtitle = "${city.region} · utilisée")
        // Unlike the flat rows, the field, its button and what they find need room between them.
        Column(Modifier.padding(top = BcSpacing.s), verticalArrangement = Arrangement.spacedBy(BcSpacing.s)) {
            BcSearchField(
                value = place.query,
                onValueChange = actions::onCityQuery,
                placeholder = "Nom de la ville",
                onSearch = actions::onSearchCity
            )
            BcButton("Chercher cette ville", onClick = actions::onSearchCity, enabled = place.query.isNotBlank())
            CityResultsList(place.results, actions::onChooseCity)
            BcText(
                "Prévisions et recherche de ville : Open-Meteo.com",
                style = BcTextStyle.Label,
                color = BcTextColor.Muted
            )
        }
    }
}

/**
 * The approximate position of the phone. While it is not allowed, « Autoriser » asks Android and chooses it. Once
 * allowed, « Autorisé » in grey: a touch chooses it again in place of a city, or, when it is already used, opens
 * Android's page where it is taken back.
 */
@Composable
private fun PositionRow(
    state: SettingsUiState,
    actions: PlaceActions,
    onAskLocation: () -> Unit,
    onOpenLocationPage: () -> Unit
) {
    val city = state.settings.weatherCity
    val allowed = state.locationAllowed
    BcSettingRow(
        title = "Ma position approximative",
        subtitle = when {
            city == null && allowed -> "Utilisée · envoyée arrondie à Open-Meteo, jamais la position précise"
            city != null -> "Toucher pour l'utiliser à la place de ${city.name}"
            else -> "Toucher pour l'autoriser · elle est envoyée arrondie à Open-Meteo"
        },
        onClick = when {
            !allowed -> null
            city != null -> actions::onUsePhonePosition
            else -> onOpenLocationPage
        },
        trailing = {
            AccessState(missing = !allowed) {
                actions.onUsePhonePosition()
                onAskLocation()
            }
        }
    )
}

@Composable
private fun CityResultsList(results: CityResults, onChoose: (City) -> Unit) {
    when (results) {
        CityResults.Idle -> Unit

        CityResults.Searching -> Note("Recherche…")

        CityResults.NoneFound -> Note("Aucune ville de ce nom")

        CityResults.Unreachable -> Note("Recherche impossible pour l'instant : vérifie le réseau")

        is CityResults.Found -> results.cities.forEach { city ->
            BcListRow(title = city.name, onClick = { onChoose(city) }, subtitle = city.region)
        }
    }
}

@Composable
private fun Note(text: String) = BcText(text, style = BcTextStyle.Message, color = BcTextColor.Muted)
