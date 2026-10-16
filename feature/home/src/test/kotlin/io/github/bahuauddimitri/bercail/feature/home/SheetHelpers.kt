package io.github.bahuauddimitri.bercail.feature.home

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.performClick

/** The summary line of the band: touching it opens the sheet of people and house. */
internal val OPENS_THE_SHEET = SemanticsMatcher("ouvre la feuille") {
    it.config.getOrNull(SemanticsActions.OnClick)?.label == "Ouvrir la feuille"
}

/** Touches the summary line of the band, which opens the sheet. */
internal fun SemanticsNodeInteractionsProvider.openSheet() {
    onNode(OPENS_THE_SHEET).performClick()
}
