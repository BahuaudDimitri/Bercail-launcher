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
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcMoment
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcMomentScope
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
                BcMomentScope(BcMoment.Evening) { BcWeatherLine("12° · Nuit claire", BcWeather.Clear, night = true) }
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
    fun `les boutons d'Écoute, le chevron du bas et la pastille correspondent à leur capture`() =
        compose.captureComponent("listen_remote") {
            Column(
                Modifier.width(348.dp).padding(vertical = 40.dp),
                verticalArrangement = Arrangement.spacedBy(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(30.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BcIconButton(BcIcons.Previous, "Précédent", onClick = {}, size = 48.dp)
                    BcIconButton(BcIcons.Pause, "Pause", onClick = {
                    }, variant = BcIconButtonVariant.Light, size = 76.dp)
                    BcIconButton(BcIcons.Next, "Suivant", onClick = {}, size = 48.dp)
                }
                BcButton("Ouvrir dans Spotify", onClick = {})
                BcIcon(BcIcons.ChevronUp, contentDescription = null, size = 28.dp, tint = BcTextColor.Faint)
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

    @Test
    fun `l'en-tête de liste, les titres de section et les lignes de réglage correspondent à leur capture`() =
        compose.captureComponent("panel_rows") {
            Column(Modifier.width(348.dp)) {
                BcPanelHeader("Toutes les apps", onBack = {}, backDescription = "Fermer la recherche")
                BcSectionLabel("Apps favorites")
                BcListRow("Spotify", onClick = {}, leading = BcLeading.Initials("S", BcPastel.Mint))
                BcSectionLabel("Agenda")
                BcSettingRow("Ligne du temps", subtitle = "Comment afficher l'approche du prochain rendez-vous") {
                    BcSegmented(listOf("Horizontale", "Verticale"), selected = 1, onSelect = {})
                }
                BcSettingRow("Apps favorites", subtitle = "Spotify, Beeper", onClick = {})
                BcSettingRow(
                    "Musique en cours",
                    subtitle = "Bercail voit ce qui joue",
                    trailing = { BcButton("Autoriser", onClick = {}) }
                )
                BcSettingRow(
                    "Gens et maison",
                    trailing = { BcSegmented(listOf("Fermée", "Ouverte"), selected = 0, onSelect = {}) }
                )
                BcSettingRow(
                    "Écoute automatique",
                    trailing = { BcSwitch(checked = true, onCheckedChange = {}, contentDescription = null) }
                )
            }
        }

    private companion object {
        val CORAL = Color(0xFFF2A49A)
        val LAVENDER = Color(0xFFB4A7F0)
    }
}
