package io.github.bahuauddimitri.bercail.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcButton
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcButtonVariant
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSheet
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcText
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextColor
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextStyle
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import io.github.bahuauddimitri.bercail.core.domain.access.Access

/**
 * The « Écouter » sheet, where the music pill leads while nothing is loaded. Until Bercail may see what the
 * media apps play, it says why it asks and offers to allow it: Bercail never asks by itself.
 */
@Composable
internal fun ListenSheet(state: HomeUiState, actions: HomeActions, links: HomeLinks) {
    BcSheet(
        visible = state.listenOpen,
        onDismiss = { actions.onListenOpenChange(false) },
        modifier = Modifier.testTag(LISTEN_TAG)
    ) {
        BcText("Écouter", style = BcTextStyle.Headline, maxLines = 1)
        if (state.mediaAllowed) {
            BcText(NOTHING_PLAYS, style = BcTextStyle.Message, color = BcTextColor.Muted)
        } else {
            AskToSeeWhatPlays(
                onAsk = { links.onAskAccess(Access.Notifications) },
                onOpenAppSettings = links::onOpenAppSettings
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

private const val NOTHING_PLAYS =
    "Rien ne joue pour l'instant. Lance une musique ou une vidéo dans son app : elle apparaîtra ici."
private const val WHY_THE_ACCESS =
    "Pour afficher ce que tu écoutes et le piloter d'ici, Bercail a besoin de ton accord. Android range ce droit " +
        "sous « Accès aux notifications », mais Bercail ne lit aucune notification : il regarde seulement ce qui joue."
private const val WHEN_ANDROID_REFUSES =
    "Si Android répond « Paramètre restreint » : ouvre la page de Bercail, puis le menu aux trois points en haut, " +
        "« Autoriser les paramètres restreints », et reviens ici."
