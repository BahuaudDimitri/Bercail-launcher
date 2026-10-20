package io.github.bahuauddimitri.bercail.feature.settings

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcGridIcon
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIconGrid
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIconSection
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcLeading
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcListRow
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcPanel
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRowEmphasis
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSearchField
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcText
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextColor
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextStyle
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import io.github.bahuauddimitri.bercail.core.domain.Picture
import io.github.bahuauddimitri.bercail.core.domain.apps.App
import io.github.bahuauddimitri.bercail.core.domain.apps.AppsSource
import io.github.bahuauddimitri.bercail.core.domain.apps.DockIcon
import io.github.bahuauddimitri.bercail.core.domain.apps.IconChoices
import io.github.bahuauddimitri.bercail.core.domain.apps.PackCatalog
import io.github.bahuauddimitri.bercail.core.domain.apps.PackIcon
import io.github.bahuauddimitri.bercail.core.domain.apps.iconChoices
import io.github.bahuauddimitri.bercail.core.domain.settings.SettingsSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The icon picker of one app of the dock: the app, what is typed in its search, and what the installed packs hold,
 * nothing while they are being read.
 */
data class IconPickerUiState(val appId: String? = null, val query: String = "", val catalogs: List<PackCatalog>? = null)

/** What the icon picker does. */
interface IconPickerActions {
    fun onIconQuery(text: String)

    /** Chooses the icon of the app [appId]: kept at once, and the dock follows. */
    fun onChooseIcon(appId: String, icon: DockIcon)

    /** A drawing of a pack, small, white: read when its cell of the grid shows. */
    suspend fun packIconOf(icon: PackIcon): Picture?

    /** The glyph of the app with the [iconPack], or its own without one: for the two choices on top. */
    suspend fun appGlyphOf(appId: String, iconPack: String?): Picture?
}

/**
 * Chooses the icon of an app of the dock. The packs are read when the picker opens ([open]), never before, and
 * forgotten when it closes ([close]): nothing goes on reading once it is gone.
 */
class IconPicker(
    private val settings: SettingsSource,
    private val apps: AppsSource,
    private val scope: CoroutineScope
) : IconPickerActions {
    private val shown = MutableStateFlow(IconPickerUiState())
    val state: StateFlow<IconPickerUiState> = shown
    private var reading: Job? = null

    /** Opens the picker of the app: what every installed pack holds is read now. */
    fun open(appId: String) {
        reading?.cancel()
        shown.value = IconPickerUiState(appId)
        reading = scope.launch {
            val catalogs = apps.iconPacks.first().map { apps.iconCatalog(it) }
            shown.update { if (it.appId == appId) it.copy(catalogs = catalogs) else it }
        }
    }

    /** Closes the picker: a reading under way stops, and what was read is let go. */
    fun close() {
        reading?.cancel()
        shown.value = IconPickerUiState()
    }

    override fun onIconQuery(text: String) = shown.update { it.copy(query = text) }

    override fun onChooseIcon(appId: String, icon: DockIcon) {
        scope.launch { settings.update { it.withDockIcon(appId, icon) } }
    }

    override suspend fun packIconOf(icon: PackIcon): Picture? = apps.packIconGlyph(icon)

    override suspend fun appGlyphOf(appId: String, iconPack: String?): Picture? = apps.glyph(appId, iconPack)
}

/**
 * The picker of an app's icon, open while it is shown: it reads the packs on opening and lets them go on closing.
 * Choosing an icon calls [onChosen].
 */
@Composable
internal fun DockIconRoute(
    appId: String,
    state: SettingsUiState,
    picker: IconPicker,
    onChosen: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    DisposableEffect(appId) {
        picker.open(appId)
        onDispose { picker.close() }
    }
    val shown by picker.state.collectAsStateWithLifecycle()
    DockIconScreen(state, shown, picker, onChosen, onBack, modifier)
}

/**
 * The icon of one app of the dock: a search by name, « Icône du pack » (the default) and « Dessin d'origine », then
 * the drawings proposed for the app, then every drawing of every installed pack, pack by pack, from A to Z. The
 * current choice stands out in the accent; touching one chooses it and calls [onChosen].
 */
@Composable
fun DockIconScreen(
    state: SettingsUiState,
    picker: IconPickerUiState,
    actions: IconPickerActions,
    onChosen: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = state.apps.firstOrNull { it.id == picker.appId }
    BcPanel("Icône · ${app?.name.orEmpty()}", onBack, "Retour au dock", modifier, scrolls = false) {
        if (app == null) return@BcPanel
        val choose = { icon: DockIcon ->
            actions.onChooseIcon(app.id, icon)
            onChosen()
        }
        val chosen = shownChoice(state, app)
        val choices = remember(app, picker.catalogs, picker.query) {
            picker.catalogs?.let { iconChoices(app, it, picker.query) }
        }
        BcSearchField(
            value = picker.query,
            onValueChange = actions::onIconQuery,
            modifier = Modifier.padding(vertical = BcSpacing.s),
            placeholder = "Chercher une icône"
        )
        BcIconGrid(
            sections = sectionsOf(choices, state, chosen, choose),
            image = { key -> actions.packIconOf(iconOf(key))?.asImage() },
            modifier = Modifier.fillMaxWidth().weight(1f),
            header = {
                DefaultChoices(app, state, chosen, actions, choose)
                PickerStatus(picker, choices)
            }
        )
    }
}

/** The choice that shows: one whose pack is gone stands for « Icône du pack », the dock falling back on it. */
private fun shownChoice(state: SettingsUiState, app: App): DockIcon {
    val chosen = state.settings.dockIconOf(app.id)
    val gone = chosen is DockIcon.Drawing && state.iconPacks.none { it.id == chosen.icon.pack }
    return if (gone) DockIcon.FromPack else chosen
}

/** « Icône du pack », then « Dessin d'origine », each with the glyph it gives the app. */
@Composable
private fun DefaultChoices(
    app: App,
    state: SettingsUiState,
    chosen: DockIcon,
    actions: IconPickerActions,
    choose: (DockIcon) -> Unit
) {
    val pack = state.iconPack
    ChoiceRow(
        title = "Icône du pack",
        subtitle =
            pack?.let { "Celle de ${it.name}, sinon le dessin de l'app" } ?: "Aucun pack choisi : le dessin de l'app",
        glyph = { actions.appGlyphOf(app.id, pack?.id) },
        glyphKey = pack?.id,
        chosen = chosen == DockIcon.FromPack
    ) { choose(DockIcon.FromPack) }
    ChoiceRow(
        title = "Dessin d'origine",
        subtitle = "Le dessin de l'app, sans pack",
        glyph = { actions.appGlyphOf(app.id, null) },
        glyphKey = null,
        chosen = chosen == DockIcon.Origin
    ) { choose(DockIcon.Origin) }
}

@Composable
private fun ChoiceRow(
    title: String,
    subtitle: String,
    glyph: suspend () -> Picture?,
    glyphKey: String?,
    chosen: Boolean,
    onClick: () -> Unit
) {
    val drawn by produceState<ImageBitmap?>(null, glyphKey) { value = glyph()?.asImage() }
    BcListRow(
        title = title,
        onClick = onClick,
        subtitle = subtitle,
        leading = drawn?.let(BcLeading::Glyph) ?: BcLeading.Initials(title.take(1)),
        emphasis = if (chosen) BcRowEmphasis.Accent else BcRowEmphasis.Normal
    )
}

/** What the grid cannot show yet, or at all: the packs being read, none installed, no drawing by that name. */
@Composable
private fun PickerStatus(picker: IconPickerUiState, choices: IconChoices?) {
    val status = when {
        choices == null -> "Lecture des packs…"
        picker.catalogs?.isEmpty() == true -> "Aucun pack d'icônes installé"
        choices.packs.isEmpty() && choices.proposed.isEmpty() -> "Aucune icône ne porte ce nom"
        else -> null
    }
    if (status != null) {
        BcText(
            status,
            modifier = Modifier.padding(top = BcSpacing.l),
            style = BcTextStyle.Label,
            color = BcTextColor.Muted
        )
    }
}

/** « Proposées », then each pack under its name; the chosen drawing marked wherever it stands. */
private fun sectionsOf(
    choices: IconChoices?,
    state: SettingsUiState,
    chosen: DockIcon,
    choose: (DockIcon) -> Unit
): List<BcIconSection> {
    if (choices == null) return emptyList()
    fun cell(icon: PackIcon) = BcGridIcon(keyOf(icon), icon.label, chosen = chosen == DockIcon.Drawing(icon)) {
        choose(DockIcon.Drawing(icon))
    }
    val proposed = choices.proposed.takeIf { it.isNotEmpty() }?.let { listOf(BcIconSection(PROPOSED, it.map(::cell))) }
    val packs = choices.packs.map { pack ->
        val name = state.iconPacks.firstOrNull { it.id == pack.pack.id }?.name ?: pack.pack.name
        BcIconSection(name, pack.icons.map(::cell))
    }
    return proposed.orEmpty() + packs
}

/** The key of a drawing in the grid: its pack, then its name; a package never holds a slash. */
private fun keyOf(icon: PackIcon) = "${icon.pack}/${icon.name}"

private fun iconOf(key: String) = PackIcon(key.substringBefore('/'), key.substringAfter('/'))

private fun Picture.asImage(): ImageBitmap =
    Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()

private const val PROPOSED = "Proposées"
