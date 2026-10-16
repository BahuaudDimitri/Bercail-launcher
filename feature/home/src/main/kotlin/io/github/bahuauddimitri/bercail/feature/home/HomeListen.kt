package io.github.bahuauddimitri.bercail.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcButton
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcButtonVariant
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcFrost
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIconButton
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIcons
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcLeading
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcListRow
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRowEmphasis
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSheet
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcText
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextColor
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextStyle
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import io.github.bahuauddimitri.bercail.core.domain.Picture
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.media.MediaAppRow

/**
 * The « Écouter » sheet, where the music pill leads while nothing is loaded: the main media app first, which
 * offers to play again what it last played, then the other apps that can play. Until Bercail may see what the
 * media apps play, the sheet says why it asks and offers to allow it: Bercail never asks by itself.
 */
@Composable
internal fun ListenSheet(state: HomeUiState, actions: HomeActions, links: HomeLinks, frost: BcFrost?) {
    BcSheet(
        visible = state.listenOpen,
        onDismiss = { actions.onListenOpenChange(false) },
        modifier = Modifier.testTag(LISTEN_TAG),
        frost = frost
    ) {
        BcText("Écouter", style = BcTextStyle.Headline, maxLines = 1)
        when {
            !state.mediaAllowed -> AskToSeeWhatPlays(
                onAsk = { links.onAskAccess(Access.Notifications) },
                onOpenAppSettings = links::onOpenAppSettings
            )

            state.mediaApps.isEmpty() -> BcText(NO_MEDIA_APP, style = BcTextStyle.Message, color = BcTextColor.Muted)

            else -> MediaApps(state.mediaApps, actions)
        }
    }
}

@Composable
private fun ColumnScope.MediaApps(rows: List<MediaAppRow>, actions: ListenActions) {
    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
        rows.forEach { row ->
            // Drawn once, when the line first shows; until then, and for an app without icon, its initial.
            val icon by produceState<Picture?>(null, row.app.id) { value = actions.iconOf(row.app.id) }
            val resume: (@Composable () -> Unit)? = row.resume?.let {
                {
                    BcIconButton(
                        icon = BcIcons.Play,
                        contentDescription = "Reprendre dans ${row.app.name}",
                        onClick = { actions.onResume(row.app.id) },
                        variant = BcButtonVariant.Light
                    )
                }
            }
            BcListRow(
                title = row.app.name,
                onClick = { actions.onOpenApp(row.app.id) },
                subtitle = row.resume,
                leading =
                    icon.asImage()?.let(BcLeading::Picture) ?: BcLeading.Initials(row.app.name.take(1).uppercase()),
                emphasis = if (row.main) BcRowEmphasis.Highlighted else BcRowEmphasis.Normal,
                trailing = resume
            )
        }
    }
}

@Composable
private fun AskToSeeWhatPlays(onAsk: () -> Unit, onOpenAppSettings: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(BcSpacing.m)) {
        BcText(WHY_THE_ACCESS, style = BcTextStyle.Message, color = BcTextColor.Muted)
        BcButton("Autoriser dans Android", onClick = onAsk, variant = BcButtonVariant.Light)
        BcText(WHEN_ANDROID_REFUSES, style = BcTextStyle.Label, color = BcTextColor.Muted)
        BcButton("Page de Bercail dans les Paramètres", onClick = onOpenAppSettings)
    }
}

private const val NO_MEDIA_APP =
    "Aucune app de musique ou de vidéo n'est installée. Dès qu'une app joue quelque chose, elle apparaît ici."
private const val WHY_THE_ACCESS =
    "Pour afficher ce que tu écoutes et le piloter d'ici, Bercail a besoin de ton accord. Android range ce droit " +
        "sous « Accès aux notifications », mais Bercail ne lit aucune notification : il regarde seulement ce qui " +
        "joue, et quelle app vient d'en afficher une, pour savoir qu'un message est arrivé."
private const val WHEN_ANDROID_REFUSES =
    "Si Android répond « Paramètre restreint » : ouvre la page de Bercail, puis le menu aux trois points en haut, " +
        "« Autoriser les paramètres restreints », et reviens ici."
