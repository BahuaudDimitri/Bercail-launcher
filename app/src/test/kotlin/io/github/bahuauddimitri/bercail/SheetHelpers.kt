package io.github.bahuauddimitri.bercail

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick

/** The summary line of the band at the bottom of the home: touching it opens the sheet of people and house. */
internal val OPENS_THE_SHEET = SemanticsMatcher("ouvre la feuille") {
    it.config.getOrNull(SemanticsActions.OnClick)?.label == "Ouvrir la feuille"
}

/** Waits for the summary line of the band, which says the home screen is there. */
internal fun ComposeTestRule.awaitBand(timeoutMillis: Long) =
    waitUntil(timeoutMillis) { onAllNodes(OPENS_THE_SHEET).fetchSemanticsNodes().isNotEmpty() }

/** Touches the summary line of the band, which opens the sheet. */
internal fun ComposeTestRule.openSheet() {
    onNode(OPENS_THE_SHEET).performClick()
}
