package io.github.bahuauddimitri.bercail.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcLeading
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcListRow
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcPanel
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSectionLabel
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSettingRow
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSwitch
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.messages.MAX_FAVORITE_PEOPLE

/** The messages: the access Beeper asks for, and the people whose tiles stand in the open drawer. */
@Composable
internal fun MessagesRows(state: SettingsUiState, access: AccessLinks, onPickPeople: () -> Unit) {
    BcSectionLabel("Messages")
    SourceRow(state.messages, Access.Messages, access)
    BcSettingRow(
        title = "Personnes favorites",
        subtitle = state.favoritePeopleNames,
        onClick = if (state.people.isNotEmpty()) onPickPeople else null
    )
}

/**
 * Every conversation, the most recent first, with a switch: the favorites have their tile in the open drawer, in
 * the order chosen. Four at most: that is the room the drawer has.
 */
@Composable
fun FavoritePeopleScreen(
    state: SettingsUiState,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chosen = state.settings.favoritePeople
    val full = chosen.size >= MAX_FAVORITE_PEOPLE
    BcPanel("Personnes favorites", onBack, backDescription = "Retour aux réglages", modifier = modifier) {
        BcSectionLabel(if (full) FULL else "Quatre au plus, dans l'ordre où tu les choisis")
        state.people.forEach { person ->
            val favorite = person.id in chosen
            BcListRow(
                title = person.name,
                onClick = { onToggle(person.id) },
                subtitle = person.network,
                leading = BcLeading.Initials(person.initials),
                trailing = {
                    BcSwitch(
                        checked = favorite,
                        onCheckedChange = { onToggle(person.id) },
                        contentDescription = null,
                        enabled = favorite || !full
                    )
                }
            )
        }
    }
}

private const val FULL = "Quatre au plus : retire une personne pour en ajouter une autre"
