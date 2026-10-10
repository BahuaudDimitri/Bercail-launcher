@file:Suppress("TooManyFunctions", "MagicNumber") // A showcase: one small function per family, example values.

package io.github.bahuauddimitri.bercail.core.designsystem.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcBrume
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcBrumeColors
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcWeather
import io.github.bahuauddimitri.bercail.core.designsystem.brume.rememberBcBrumeState
import io.github.bahuauddimitri.bercail.core.designsystem.brume.wakesBrume
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcAlphabetRail
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcBubble
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcButton
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcButtonVariant
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcCover
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcDrawer
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcHomeToggle
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIcon
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIconButton
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIcons
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcLeading
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcListRow
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcMusicPill
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcPageDots
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcPersonTile
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcPill
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcProgress
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRowEmphasis
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSearchField
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSegmented
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSheet
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSummaryLeading
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSummaryRow
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSwitch
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcText
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextColor
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextStyle
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTimeline
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTimelineMark
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTimelineOrientation
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcUndoButton
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcWeatherLine
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing

internal val GALLERY_SECTIONS = listOf(
    "Couleurs",
    "Texte",
    "Icônes",
    "Boutons",
    "Accueil",
    "Personnes et maison",
    "Listes",
    "Réglages",
    "Conversation",
    "Musique",
    "Navigation",
    "Tiroir et feuille",
    "Brume"
)

/**
 * Debug gallery: every component of the design system in its states, interactive, on the night background.
 * Reached from the "Bercail galerie" app of the debug build. Every public component must appear here.
 */
@Composable
fun BcGallery(modifier: Modifier = Modifier) {
    var sheetOpen by remember { mutableStateOf(false) }
    Box(modifier.fillMaxSize().background(BcColors.base)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Bars first, then scrolling: content never slides under the status bar.
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(BcSpacing.l),
            verticalArrangement = Arrangement.spacedBy(BcSpacing.xl)
        ) {
            BcText("Galerie Bercail", style = BcTextStyle.AgendaTitle)
            Section("Couleurs") { Colors() }
            Section("Texte") { BcTextStyle.entries.forEach { BcText(it.name, style = it) } }
            Section("Icônes") { Icons() }
            Section("Boutons") { Buttons() }
            Section("Accueil") { Home() }
            Section("Personnes et maison") { PeopleAndHome() }
            Section("Listes") { Lists() }
            Section("Réglages") { Settings() }
            Section("Conversation") { Conversation() }
            Section("Musique") { Music() }
            Section("Navigation") { Navigation() }
            Section("Tiroir et feuille") { DrawerAndSheet(onOpenSheet = { sheetOpen = true }) }
            Section("Brume") { Brume() }
        }
        BcSheet(visible = sheetOpen, onDismiss = { sheetOpen = false }) {
            BcText("Léa", style = BcTextStyle.Headline)
            BcText("WhatsApp via Beeper", style = BcTextStyle.LabelSmall, color = BcTextColor.Muted)
            BcBubble("On se retrouve à 20 h ?", author = "Léa", mine = false, pastel = BcPastel.Coral)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(BcSpacing.m)) {
        BcText(title, style = BcTextStyle.LabelStrong, color = BcTextColor.Muted)
        content()
    }
}

@Composable
private fun Colors() {
    val swatches = listOf(BcColors.text, BcColors.textMuted, BcColors.off, BcColors.appTile) + BcColors.pastels
    Row(horizontalArrangement = Arrangement.spacedBy(BcSpacing.s)) {
        swatches.forEach { color -> Box(Modifier.size(SWATCH).background(color, BcShapes.tile)) }
    }
}

@Composable
private fun Icons() {
    BcIcons.entries.chunked(ICONS_PER_ROW).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(BcSpacing.m)) {
            row.forEach { BcIcon(it, contentDescription = it.name) }
        }
    }
}

@Composable
private fun Buttons() {
    var clicks by remember { mutableIntStateOf(0) }
    BcButton("Ouvrir dans Spotify ($clicks)", onClick = { clicks++ })
    BcButton("Reprendre", onClick = { clicks++ }, variant = BcButtonVariant.Light, icon = BcIcons.Play)
    BcButton("Envoyer", onClick = {}, variant = BcButtonVariant.Light, enabled = false)
    Row(horizontalArrangement = Arrangement.spacedBy(BcSpacing.s)) {
        BcIconButton(BcIcons.Mic, "Dicter", onClick = { clicks++ })
        BcIconButton(BcIcons.Send, "Envoyer", onClick = { clicks++ }, variant = BcButtonVariant.Light)
        BcIconButton(BcIcons.Back, "Retour", onClick = { clicks++ }, variant = BcButtonVariant.Ghost)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(BcSpacing.xs)) {
        BcPill("J'arrive", onClick = { clicks++ })
        BcPill("Écouter…", onClick = { clicks++ }, icon = BcIcons.Note)
    }
}

@Composable
private fun PeopleAndHome() {
    var unread by remember { mutableIntStateOf(2) }
    Row {
        BcPersonTile("Léa", "L", BcPastel.Coral, unread = unread, onClick = { unread = 0 })
        BcPersonTile("Tom", "T", BcPastel.Blue, unread = 0, onClick = { unread++ })
        BcPersonTile("Équipe", "É", BcPastel.Mint, unread = 0, onClick = {})
        BcPersonTile("Maman", "M", BcPastel.Lavender, unread = 140, onClick = {})
        BcPersonTile("Tous", "", pastel = null, unread = 5, onClick = {}, icon = BcIcons.Chat)
    }
    val lit = remember { mutableStateListOf(true, false, true, false) }
    Row {
        BcHomeToggle("Salon", if (lit[0]) "60 %" else "éteint", BcIcons.Lamp, BcPastel.Apricot, lit[0], {
            lit[0] =
                !lit[0]
        })
        BcHomeToggle("Cinéma", if (lit[1]) "active" else "prête", BcIcons.Film, BcPastel.Lavender, lit[1], {
            lit[1] =
                !lit[1]
        })
        BcHomeToggle("Volets", if (lit[2]) "ouverts" else "fermés", BcIcons.Shutters, BcPastel.Blue, lit[2], {
            lit[2] =
                !lit[2]
        })
        BcHomeToggle("Chauffage", if (lit[3]) "21 °C" else "18 °C", BcIcons.Flame, BcPastel.Coral, lit[3], {
            lit[3] =
                !lit[3]
        })
    }
}

@Composable
private fun Lists() {
    BcListRow("Réglages de Bercail", onClick = {}, leading = BcLeading.Icon(BcIcons.Settings))
    BcListRow("Spotify", onClick = {}, subtitle = "Reprendre « Nekketsu »", leading = BcLeading.Initials("S"))
    BcListRow("Léa", onClick = {}, subtitle = "WhatsApp", leading = BcLeading.Initials("L", BcPastel.Coral))
    BcListRow("Stand-up", onClick = {}, leading = BcLeading.Time("08:00"), emphasis = BcRowEmphasis.Dimmed)
    BcListRow("Café avec Tom", onClick = {}, leading = BcLeading.Time("09:30"), emphasis = BcRowEmphasis.Highlighted)
}

@Composable
private fun Settings() {
    var choice by remember { mutableIntStateOf(0) }
    var on by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(PROGRESS) }
    BcSegmented(listOf("Verticale", "Horizontale"), selected = choice, onSelect = { choice = it })
    BcSwitch(on, onCheckedChange = { on = it }, contentDescription = "Écoute automatique")
    BcProgress(progress, onSeek = { progress = it })
}

@Composable
private fun Conversation() {
    var query by remember { mutableStateOf("") }
    BcSearchField(value = query, onValueChange = { query = it })
    BcBubble(
        "Tu es où ? On commence sans toi",
        author = "Léa",
        mine = false,
        pastel = BcPastel.Coral,
        header = "Aujourd'hui"
    )
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(BcSpacing.s)
    ) {
        BcBubble("J'arrive dans 10 minutes", author = "Léa", mine = true, pastel = BcPastel.Coral)
        BcUndoButton(remaining = UNDO_REMAINING, onUndo = {})
    }
}

@Composable
private fun Home() {
    var page by remember { mutableIntStateOf(0) }
    BcWeatherLine("11° · Ciel dégagé ce matin", BcWeather.Clear)
    BcWeatherLine("14° · Pluie cet après-midi", BcWeather.Rain)
    BcSummaryRow(
        text = "Léa, Tom",
        strong = "2 non lus",
        leading = BcSummaryLeading.Dots(listOf(BcPastel.Coral, BcPastel.Blue)),
        onClick = {}
    )
    BcSummaryRow("Salon 60 %, Volets ouverts", leading = BcSummaryLeading.Icon(BcIcons.House), onClick = {})
    BcPageDots(listOf("Accueil", "Écoute"), selected = page, onSelect = { page = it })
}

@Composable
private fun Music() {
    var playing by remember { mutableStateOf(true) }
    val colors = BcColors.pastelCoral to BcColors.pastelLavender
    BcMusicPill(title = "Nekketsu", artist = "Nekfeu", cover = null, playing = playing, onClick = {
        playing = !playing
    })
    BcMusicPill("Looped", "Kiasmos", cover = null, playing = playing, onClick = {
        playing = !playing
    }, coverColors = colors)
    Column(
        Modifier.fillMaxWidth().padding(vertical = BcSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(COVER_GAP)
    ) {
        BcCover(image = null, colors = colors, contentDescription = "Pochette de Looped")
        Row(
            horizontalArrangement = Arrangement.spacedBy(BcSpacing.xl),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BcIconButton(BcIcons.Previous, "Précédent", onClick = {}, variant = BcButtonVariant.Ghost, size = REMOTE)
            BcIconButton(
                icon = if (playing) BcIcons.Pause else BcIcons.Play,
                contentDescription = if (playing) "Pause" else "Lecture",
                onClick = { playing = !playing },
                variant = BcButtonVariant.Light,
                size = REMOTE_MAIN
            )
            BcIconButton(BcIcons.Next, "Suivant", onClick = {}, variant = BcButtonVariant.Ghost, size = REMOTE)
        }
    }
}

@Composable
private fun Navigation() {
    val reached = remember { mutableStateOf("—") }
    val day = listOf(
        BcTimelineMark(position = 0.06f, label = "08:00"),
        BcTimelineMark(position = 0.15f, label = "09:30", next = true),
        BcTimelineMark(position = 0.47f, label = "15:00")
    )
    BcText("Lettre atteinte : ${reached.value}", style = BcTextStyle.Caption, color = BcTextColor.Muted)
    Row(horizontalArrangement = Arrangement.spacedBy(BcSpacing.xl)) {
        BcAlphabetRail(listOf("A", "B", "D", "G", "M", "S", "W"), onLetter = {
            reached.value = it
        }, modifier = Modifier.height(TALL))
        BcTimeline(now = NOW, marks = day, modifier = Modifier.height(TALL).width(TIMELINE_WIDTH))
    }
    BcTimeline(
        now = NOW,
        marks = day,
        orientation = BcTimelineOrientation.Horizontal,
        modifier = Modifier.fillMaxWidth().height(LINE)
    )
}

@Composable
private fun DrawerAndSheet(onOpenSheet: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    BcDrawer(expanded = expanded, onExpandedChange = { expanded = it }) {
        if (expanded) {
            BcText("Favoris et maison", style = BcTextStyle.BodySmall)
        } else {
            BcText("2 non lus · Léa, Tom", style = BcTextStyle.BodySmall, color = BcTextColor.Muted)
        }
        BcSearchField(value = "", onValueChange = {})
    }
    BcButton("Ouvrir une feuille", onClick = onOpenSheet)
}

@Composable
private fun Brume() {
    var weather by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(true) }
    val state = rememberBcBrumeState()
    BcSegmented(listOf("Clair", "Pluie", "Vent", "Neige"), selected = weather, onSelect = { weather = it })
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BcSpacing.s)) {
        BcSwitch(playing, onCheckedChange = { playing = it }, contentDescription = "Lecture")
        BcText(if (state.isAnimating) "Brume s'anime" else "Brume est immobile", style = BcTextStyle.Caption)
    }
    BcBrume(
        colors = BcBrumeColors.Evening,
        playing = playing,
        state = state,
        weather = BcWeather.entries[weather],
        night = weather == 0,
        modifier = Modifier.fillMaxWidth().height(BRUME_HEIGHT).wakesBrume(state)
    )
}

private const val ICONS_PER_ROW = 7
private const val PROGRESS = 0.35f
private const val UNDO_REMAINING = 0.6f
private const val NOW = 0.1f
private val SWATCH = 28.dp
private val TALL = 260.dp
private val TIMELINE_WIDTH = 64.dp
private val LINE = 44.dp
private val BRUME_HEIGHT = 420.dp
private val COVER_GAP = 48.dp
private val REMOTE = 52.dp
private val REMOTE_MAIN = 66.dp
