package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.bahuauddimitri.bercail.core.designsystem.PIXEL_9
import io.github.bahuauddimitri.bercail.core.designsystem.captureComponent
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class BcBandScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `le bandeau replié correspond à sa capture`() = compose.captureComponent("band_folded") {
        BcBand(onOpen = {}, modifier = Modifier.width(380.dp)) {
            BcSummaryRow(
                text = "Léa, Tom",
                strong = "2 non lus",
                aside = "Salon 60 %, Volets ouverts",
                leading = BcSummaryLeading.Dots(listOf(BcPastel.Coral, BcPastel.Blue)),
                onClick = {}
            )
            BcSearchField(value = "", onValueChange = {})
        }
    }

    @Test
    fun `la feuille posée sur le bandeau correspond à sa capture`() = compose.captureComponent("band_sheet") {
        BcBandSheet(visible = true, onClose = {}, modifier = Modifier.width(380.dp)) {
            BcSectionLabel("Gens")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BcRing("Léa", BcRingMark.Initial("L"), {}, tone = BcRingTone.Pastel(BcPastel.Coral), unread = 2)
                BcRing("Tom", BcRingMark.Initial("T"), {}, tone = BcRingTone.Pastel(BcPastel.Blue))
                BcRing("Équipe", BcRingMark.Initial("É"), {}, tone = BcRingTone.Pastel(BcPastel.Mint))
                BcRing("Maman", BcRingMark.Initial("M"), {}, tone = BcRingTone.Pastel(BcPastel.Lavender))
                BcRing("Tous", BcRingMark.Icon(BcIcons.Chat), {}, unread = 3)
            }
            BcSectionLabel("Maison")
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
    fun `la recherche plein écran correspond à sa capture`() = compose.captureComponent("band_search") {
        // Two pastel blocks behind it show the glass: no corner, no edge, 80 % dark.
        Box(Modifier.size(380.dp, 560.dp)) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().weight(1f).background(BcColors.pastelCoral))
                Box(Modifier.fillMaxWidth().weight(1f).background(BcColors.pastelBlue))
            }
            BcBand(onOpen = {}, searching = true) {
                Column(Modifier.weight(1f)) {
                    BcPanelHeader("Toutes les apps", onBack = {}, backDescription = "Fermer la recherche")
                    BcSectionLabel("Dock")
                    BcListRow("Chrome", onClick = {}, leading = BcLeading.Icon(BcIcons.Web))
                    BcListRow("Beeper", onClick = {}, leading = BcLeading.Initials("B"))
                }
                BcSearchField(value = "", onValueChange = {})
            }
        }
    }
}
