package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.bahuauddimitri.bercail.core.designsystem.PIXEL_9
import io.github.bahuauddimitri.bercail.core.designsystem.captureComponent
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class BcControlsScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `les anneaux des gens et de la maison correspondent à leur capture`() = compose.captureComponent("rings") {
        Column(Modifier.width(363.dp), verticalArrangement = Arrangement.spacedBy(BcSpacing.l)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BcRing("Léa", BcRingMark.Initial("L"), {}, tone = BcRingTone.Pastel(BcPastel.Coral), unread = 2)
                BcRing("Tom", BcRingMark.Initial("T"), {}, tone = BcRingTone.Pastel(BcPastel.Blue))
                BcRing("Équipe", BcRingMark.Initial("É"), {}, tone = BcRingTone.Pastel(BcPastel.Mint))
                BcRing("Maman", BcRingMark.Initial("M"), {}, tone = BcRingTone.Pastel(BcPastel.Lavender), unread = 140)
                BcRing("Tous", BcRingMark.Icon(BcIcons.Chat), {}, unread = 5)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BcRing("Salon", "60 %", BcRingMark.Icon(BcIcons.Lamp), BcPastel.Apricot, on = true, onToggle = {})
                BcRing("Cinéma", "prête", BcRingMark.Icon(BcIcons.Film), BcPastel.Lavender, on = false, onToggle = {})
                BcRing("Volets", "ouverts", BcRingMark.Icon(BcIcons.Shutters), BcPastel.Blue, on = true, onToggle = {})
                BcRing("Chauffage", "18 °C", BcRingMark.Icon(BcIcons.Flame), BcPastel.Coral, on = false, onToggle = {})
                BcRing("Tout", BcRingMark.Icon(BcIcons.House), {}, tone = BcRingTone.Off)
            }
        }
    }

    @Test
    fun `les lignes de liste correspondent à leur capture`() = compose.captureComponent("list_rows") {
        Column(Modifier.width(360.dp)) {
            BcListRow("Réglages de Bercail", onClick = {}, leading = BcLeading.Icon(BcIcons.Settings))
            BcListRow("Spotify", onClick = {}, subtitle = "Reprendre « Nekketsu »", leading = BcLeading.Initials("S"))
            BcListRow("Léa", onClick = {}, subtitle = "WhatsApp", leading = BcLeading.Initials("L", BcPastel.Coral))
            BcListRow(
                "Café avec Tom",
                onClick = {},
                leading = BcLeading.Time("09:30"),
                emphasis = BcRowEmphasis.Highlighted
            )
            BcListRow("Stand-up", onClick = {}, leading = BcLeading.Time("08:00"), emphasis = BcRowEmphasis.Dimmed)
        }
    }

    @Test
    fun `les réglages correspondent à leur capture`() = compose.captureComponent("settings_controls") {
        Column(verticalArrangement = Arrangement.spacedBy(BcSpacing.m)) {
            BcProgress(0.35f, onSeek = {}, modifier = Modifier.width(280.dp))
            BcSegmented(listOf("Verticale", "Horizontale"), selected = 0, onSelect = {})
            Row(horizontalArrangement = Arrangement.spacedBy(BcSpacing.m)) {
                BcSwitch(checked = true, onCheckedChange = {}, contentDescription = "Activé")
                BcSwitch(checked = false, onCheckedChange = {}, contentDescription = "Désactivé")
            }
        }
    }
}
