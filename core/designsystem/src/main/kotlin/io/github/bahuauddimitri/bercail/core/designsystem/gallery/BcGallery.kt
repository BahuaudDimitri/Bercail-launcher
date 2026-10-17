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
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcWallpaperVeil
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcWeather
import io.github.bahuauddimitri.bercail.core.designsystem.brume.rememberBcBrumeState
import io.github.bahuauddimitri.bercail.core.designsystem.brume.wakesBrume
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcAlphabetRail
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcArcProgress
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcArcProgressCurveCentre
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcArchCover
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcArchCoverCurveCentre
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcBand
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcBandSheet
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcBubble
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcButton
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcButtonVariant
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcDayEntry
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcDayList
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcDayMark
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcDock
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcDockApp
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcDockMenu
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIcon
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIconButton
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIconButtonVariant
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIcons
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcLeading
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcListRow
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcMusicPill
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcPageDots
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcPanel
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcPanelHeader
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcPill
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcProgress
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcReplyField
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRing
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRingMark
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRingTone
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRowEmphasis
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRowSwipe
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSearchField
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSectionLabel
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSegmented
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSettingRow
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSheet
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSheetHeader
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcShortcut
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcShortcutMenu
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSummaryLeading
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSummaryRow
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSwipeBlock
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSwitch
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcText
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextColor
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextStyle
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTimeline
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTimelineEnd
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTimelineMark
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcUndoButton
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcUndoCountdown
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcWeatherLine
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcMoment
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcMomentScope
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
    "Dock",
    "Listes",
    "Réglages",
    "Conversation",
    "Musique",
    "Navigation",
    "Bandeau et feuilles",
    "Brume"
)

/**
 * Debug gallery: every component of the design system in its states, interactive, on the night background.
 * Reached from the "Bercail galerie" app of the debug build. Every public component must appear here.
 */
@Composable
fun BcGallery(modifier: Modifier = Modifier) {
    var sheetOpen by remember { mutableStateOf(false) }
    var panelOpen by remember { mutableStateOf(false) }
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
            Section("Dock") { Dock() }
            Section("Listes") { Lists() }
            Section("Réglages") { Settings() }
            Section("Conversation") { Conversation() }
            Section("Musique") { Music() }
            Section("Écoute") { Listen() }
            Section("Navigation") { Navigation() }
            Section("Bandeau et feuilles") {
                DrawerAndSheet(onOpenSheet = { sheetOpen = true }, onOpenPanel = { panelOpen = true })
            }
            Section("Brume") { Brume() }
        }
        BcSheet(visible = sheetOpen, onDismiss = { sheetOpen = false }) {
            BcSheetHeader("Léa", subtitle = "WhatsApp via Beeper", action = "Beeper")
            BcBubble("On se retrouve à 20 h ?", author = "Léa", mine = false, pastel = BcPastel.Coral)
        }
        if (panelOpen) SamplePanel(onBack = { panelOpen = false })
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
    BcButton("Relier Beeper", onClick = { clicks++ }, icon = BcIcons.Chat)
    BcButton("Installer", onClick = { clicks++ }, variant = BcButtonVariant.Accent)
    BcButton("Envoyer", onClick = {}, variant = BcButtonVariant.Accent, enabled = false)
    Row(horizontalArrangement = Arrangement.spacedBy(BcSpacing.s)) {
        BcIconButton(BcIcons.Mic, "Dicter", onClick = { clicks++ })
        BcIconButton(BcIcons.Send, "Envoyer", onClick = { clicks++ }, variant = BcIconButtonVariant.Accent)
        BcIconButton(BcIcons.Back, "Retour", onClick = { clicks++ }, variant = BcIconButtonVariant.Ghost)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(BcSpacing.xs)) {
        BcPill("J'arrive", onClick = { clicks++ })
        BcPill("Écouter…", onClick = { clicks++ }, icon = BcIcons.Note)
    }
}

@Composable
private fun PeopleAndHome() {
    var unread by remember { mutableIntStateOf(2) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        BcRing("Léa", BcRingMark.Initial("L"), {
            unread = 0
        }, tone = BcRingTone.Pastel(BcPastel.Coral), unread = unread)
        BcRing("Tom", BcRingMark.Initial("T"), { unread++ }, tone = BcRingTone.Pastel(BcPastel.Blue))
        BcRing("Équipe", BcRingMark.Initial("É"), {}, tone = BcRingTone.Pastel(BcPastel.Mint))
        BcRing("Maman", BcRingMark.Initial("M"), {}, tone = BcRingTone.Pastel(BcPastel.Lavender), unread = 140)
        BcRing("Tous", BcRingMark.Icon(BcIcons.Chat), {}, unread = 5)
    }
    val lit = remember { mutableStateListOf(true, false, true, false) }
    val controls = listOf(
        Triple("Salon", BcIcons.Lamp, BcPastel.Apricot) to ("60 %" to "éteint"),
        Triple("Cinéma", BcIcons.Film, BcPastel.Lavender) to ("active" to "prête"),
        Triple("Volets", BcIcons.Shutters, BcPastel.Blue) to ("ouverts" to "fermés"),
        Triple("Chauffage", BcIcons.Flame, BcPastel.Coral) to ("21 °C" to "18 °C")
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        controls.forEachIndexed { index, (control, states) ->
            val (name, icon, pastel) = control
            val state = if (lit[index]) states.first else states.second
            BcRing(name, state, BcRingMark.Icon(icon), pastel, on = lit[index], onToggle = { lit[index] = !lit[index] })
        }
        BcRing("Tout", BcRingMark.Icon(BcIcons.House), {}, tone = BcRingTone.Off)
    }
}

@Composable
private fun Dock() {
    var opened by remember { mutableStateOf("") }
    var menuOf by remember { mutableStateOf<String?>(null) }
    val names = listOf("Chrome", "Téléphone", "Beeper", "Appareil photo", "Agenda")
    val shortcuts = listOf("Nouvel onglet", "Navigation privée").map { label ->
        BcShortcut(label, glyph = null) {
            opened = label
            menuOf = null
        }
    }
    val apps = names.map { name -> BcDockApp(name, glyph = null, onLongClick = { menuOf = name }) { opened = name } }
    BcDock(apps, menu = menuOf?.let { BcDockMenu(it, shortcuts) { menuOf = null } })
    BcText(
        opened.ifEmpty { "Toucher une app du dock, ou la garder sous le doigt" },
        style = BcTextStyle.LabelSmall,
        color = BcTextColor.Muted
    )
    BcShortcutMenu(shortcuts)
}

@Composable
private fun Lists() {
    BcListRow("Réglages de Bercail", onClick = {}, leading = BcLeading.Icon(BcIcons.Settings))
    BcListRow(
        "Maps",
        onClick = {},
        leading = BcLeading.Initials("M"),
        emphasis = BcRowEmphasis.Accent,
        found = 0..1,
        trailing = { BcText("Entrée", style = BcTextStyle.ListSubtitle, color = BcTextColor.Muted) }
    )
    BcListRow("Spotify", onClick = {}, subtitle = "Reprendre « Nekketsu »", leading = BcLeading.Initials("S"))
    BcListRow("Léa", onClick = {}, subtitle = "WhatsApp", leading = BcLeading.Initials("L", BcPastel.Coral))
    BcListRow("Stand-up", onClick = {}, leading = BcLeading.Time("08:00"), emphasis = BcRowEmphasis.Dimmed)
    BcListRow("Café avec Tom", onClick = {}, leading = BcLeading.Time("09:30"), emphasis = BcRowEmphasis.Accent)
    BcDayList(
        entries = listOf(
            BcDayEntry("anniversaire", "Journée", "Anniversaire de Léa", BcDayMark.AllDay),
            BcDayEntry("standup", "08:00", "Stand-up", BcDayMark.Past),
            BcDayEntry("cafe", "09:30", "Café avec Tom", BcDayMark.Next),
            BcDayEntry("courses", "18:30", "Courses", BcDayMark.Coming)
        ),
        onOpen = {},
        // The list scrolls by itself: within the scrolling gallery, it needs a height.
        modifier = Modifier.height(DAY_LIST_HEIGHT)
    )
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
        BcUndoCountdown(millis = UNDO_MILLIS, onUndo = {})
    }
    var reply by remember { mutableStateOf("") }
    BcReplyField(value = reply, onValueChange = { reply = it }, onSend = { reply = "" })
}

@Composable
private fun Home() {
    var page by remember { mutableIntStateOf(0) }
    BcSwipeBlock(swipe = BcRowSwipe("Ouvrir l'agenda") {}, onClick = {}) {
        BcText("Dans 1 h 20 · 09:30", style = BcTextStyle.LabelStrong, color = BcTextColor.Muted)
        BcText("Stand-up", style = BcTextStyle.AgendaTitle)
    }
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
}

@Composable
private fun ColumnScope.Listen() {
    var progress by remember { mutableFloatStateOf(PROGRESS) }
    val seconds = (progress * TRACK_SECONDS).toInt()
    // As on the Listen screen: the arc around the centre of the arch's curve.
    Box(Modifier.align(Alignment.CenterHorizontally), contentAlignment = Alignment.TopCenter) {
        BcArchCover(
            image = null,
            colors = BcColors.pastelBlue to BcColors.pastelCoral,
            contentDescription = "Pochette de Looped"
        )
        BcArcProgress(
            progress = progress,
            elapsed = "${seconds / SECONDS_PER_MINUTE}:${"%02d".format(seconds % SECONDS_PER_MINUTE)}",
            duration = "18:00",
            onSeek = { progress = it },
            modifier = Modifier.padding(top = BcArchCoverCurveCentre - BcArcProgressCurveCentre)
        )
    }
    BcArcProgress(
        progress = 0f,
        elapsed = "",
        duration = "",
        onSeek = null,
        live = true,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
    // Under the arc: previous, play or pause filled light, next; the way to the app; the chevron that says « up ».
    var playing by remember { mutableStateOf(true) }
    Row(
        Modifier.align(Alignment.CenterHorizontally),
        horizontalArrangement = Arrangement.spacedBy(REMOTE_GAP),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BcIconButton(BcIcons.Previous, "Précédent", onClick = {}, size = REMOTE)
        BcIconButton(
            icon = if (playing) BcIcons.Pause else BcIcons.Play,
            contentDescription = if (playing) "Pause" else "Lecture",
            onClick = { playing = !playing },
            variant = BcIconButtonVariant.Light,
            size = REMOTE_MAIN
        )
        BcIconButton(BcIcons.Next, "Suivant", onClick = {}, size = REMOTE)
    }
    BcButton("Ouvrir dans Spotify", onClick = {}, modifier = Modifier.align(Alignment.CenterHorizontally))
    BcIcon(
        BcIcons.ChevronUp,
        contentDescription = null,
        modifier = Modifier.align(Alignment.CenterHorizontally),
        size = CHEVRON,
        tint = BcTextColor.Faint
    )
}

@Composable
private fun Navigation() {
    val reached = remember { mutableStateOf("—") }
    val day = listOf(
        BcTimelineMark(position = 0.06f, label = "08:00", title = "Stand-up"),
        BcTimelineMark(position = 0.15f, label = "09:30", next = true, title = "Café avec Tom"),
        BcTimelineMark(position = 0.47f, label = "15:00", title = "Point design")
    )
    BcText("Lettre atteinte : ${reached.value}", style = BcTextStyle.Caption, color = BcTextColor.Muted)
    Row(horizontalArrangement = Arrangement.spacedBy(BcSpacing.xl)) {
        BcAlphabetRail(listOf("A", "B", "D", "G", "M", "S", "W"), onLetter = {
            reached.value = it
        }, modifier = Modifier.height(TALL))
        BcTimeline(
            now = NOW,
            marks = day,
            modifier = Modifier.height(TALL).width(TIMELINE_WIDTH),
            end = BcTimelineEnd("07:15", "réveil · 9°", "Réveil demain à 07:15"),
            onOpenDay = {}
        )
    }
    // Without an alarm in the next 24 hours: the same ring, in grey, which still opens the alarms.
    BcTimeline(
        now = NOW,
        marks = emptyList(),
        modifier = Modifier.height(TALL).width(TIMELINE_WIDTH),
        end = BcTimelineEnd("", "Aucun réveil", "Aucun réveil", set = false),
        onOpenDay = {}
    )
    // The same line at each moment of the day, in its accent.
    Row(horizontalArrangement = Arrangement.spacedBy(BcSpacing.xl)) {
        BcMoment.entries.forEach { moment ->
            BcMomentScope(moment) {
                BcTimeline(
                    now = NOW,
                    marks = day.map { it.copy(title = "") },
                    modifier = Modifier.height(TALL).width(MOMENT_WIDTH)
                )
            }
        }
    }
}

@Composable
private fun DrawerAndSheet(onOpenSheet: () -> Unit, onOpenPanel: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        BcBand(onOpen = { open = true }) {
            BcSummaryRow(
                text = "Léa, Tom",
                strong = "2 non lus",
                aside = "Salon 60 %, Volets ouverts",
                leading = BcSummaryLeading.Dots(listOf(BcPastel.Coral, BcPastel.Blue)),
                onClick = { open = true }
            )
            BcSearchField(value = "", onValueChange = {})
        }
        BcBandSheet(visible = open, onClose = { open = false }, modifier = Modifier.align(Alignment.BottomCenter)) {
            BcSectionLabel("Gens")
            BcText("Favoris et maison", style = BcTextStyle.BodySmall)
        }
    }
    BcButton("Ouvrir une feuille", onClick = onOpenSheet)
    BcButton("Ouvrir un panneau", onClick = onOpenPanel)
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
    BcText("Voile sur une image claire", style = BcTextStyle.Caption, color = BcTextColor.Muted)
    Box(Modifier.fillMaxWidth().height(BRUME_HEIGHT).background(BcColors.pastelApricot)) {
        BcWallpaperVeil(Modifier.matchParentSize())
        BcText("Journée libre", modifier = Modifier.padding(BcSpacing.l), style = BcTextStyle.AgendaTitle)
    }
}

private const val ICONS_PER_ROW = 7
private const val PROGRESS = 0.35f
private const val TRACK_SECONDS = 1080
private const val SECONDS_PER_MINUTE = 60
private const val UNDO_REMAINING = 0.6f
private const val UNDO_MILLIS = 6_000
private const val NOW = 0.1f
private val SWATCH = 28.dp
private val TALL = 260.dp
private val DAY_LIST_HEIGHT = 200.dp
private val TIMELINE_WIDTH = 200.dp
private val MOMENT_WIDTH = 64.dp
private val BRUME_HEIGHT = 420.dp
private val REMOTE_GAP = 30.dp
private val CHEVRON = 28.dp
private val REMOTE = 48.dp
private val REMOTE_MAIN = 76.dp

/** The settings panel with sample rows: a choice, a link, a button on the right, a section title. */
@Composable
private fun SamplePanel(onBack: () -> Unit) {
    var choice by remember { mutableIntStateOf(1) }
    BcPanel("Réglages de Bercail", onBack = onBack, backDescription = "Fermer le panneau") {
        BcPanelHeader("Toutes les apps", onBack = onBack, backDescription = "Fermer la recherche")
        BcSectionLabel("Agenda")
        BcSettingRow("Ligne du temps", subtitle = "Comment afficher l'approche du prochain rendez-vous") {
            BcSegmented(listOf("Horizontale", "Verticale"), selected = choice, onSelect = { choice = it })
        }
        BcSettingRow("Apps favorites", subtitle = "Spotify, Beeper", onClick = {})
        BcSettingRow(
            "Musique en cours",
            subtitle = "Bercail voit ce qui joue",
            trailing = { BcButton("Autoriser", onClick = {}) }
        )
    }
}
