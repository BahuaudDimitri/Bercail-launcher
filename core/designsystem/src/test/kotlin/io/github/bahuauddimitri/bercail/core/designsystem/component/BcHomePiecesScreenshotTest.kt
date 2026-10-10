package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.bahuauddimitri.bercail.core.designsystem.PIXEL_9
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcWeather
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
class BcHomePiecesScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `les lignes de résumé, la météo et les points de pagination correspondent à leur capture`() =
        compose.captureComponent("home_pieces") {
            Column(Modifier.width(348.dp), verticalArrangement = Arrangement.spacedBy(BcSpacing.s)) {
                BcWeatherLine("11° · Ciel dégagé ce matin", BcWeather.Clear)
                BcWeatherLine("14° · Pluie cet après-midi", BcWeather.Rain)
                BcWeatherLine("12° · Vent ce soir", BcWeather.Wind)
                BcWeatherLine("0° · Neige cette nuit", BcWeather.Snow)
                BcSummaryRow(
                    text = "Léa, Tom",
                    strong = "2 non lus",
                    leading = BcSummaryLeading.Dots(listOf(BcPastel.Coral, BcPastel.Blue)),
                    onClick = {}
                )
                BcSummaryRow("Aucun message", leading = BcSummaryLeading.Dots(emptyList()), onClick = {})
                BcSummaryRow("Salon 60 %, Volets ouverts", leading = BcSummaryLeading.Icon(BcIcons.House), onClick = {})
                BcPageDots(listOf("Accueil", "Écoute"), selected = 0, onSelect = {})
                BcPageDots(listOf("Accueil", "Écoute"), selected = 1, onSelect = {})
            }
        }

    @Test
    fun `la grande pochette et la télécommande correspondent à leur capture`() =
        compose.captureComponent("cover_remote") {
            Column(
                Modifier.width(348.dp).padding(vertical = 40.dp),
                verticalArrangement = Arrangement.spacedBy(60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BcCover(image = null, colors = CORAL to LAVENDER, contentDescription = null)
                Row(
                    Modifier.width(300.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val ghost = BcButtonVariant.Ghost
                    BcIconButton(BcIcons.Previous, "Précédent", onClick = {}, variant = ghost, size = 52.dp)
                    BcIconButton(BcIcons.Pause, "Pause", onClick = {}, variant = BcButtonVariant.Light, size = 66.dp)
                    BcIconButton(BcIcons.Next, "Suivant", onClick = {}, variant = ghost, size = 52.dp)
                }
                BcMusicPill(
                    title = "Looped",
                    artist = "Kiasmos",
                    cover = null,
                    playing = false,
                    onClick = {},
                    coverColors = CORAL to LAVENDER
                )
            }
        }

    private companion object {
        val CORAL = Color(0xFFF2A49A)
        val LAVENDER = Color(0xFFB4A7F0)
    }
}
