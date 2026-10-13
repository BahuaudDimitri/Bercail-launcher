package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
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

/** What being touched looks like: a soft veil with room around a bare line, and the shape of what has one. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class BcPressScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val touched = listOf("line", "tile", "pill", "button", "row")

    @Test
    fun `une ligne, une tuile, une pastille, un bouton et une rangée, tous touchés, correspondent à leur capture`() =
        compose.captureComponent(
            name = "pressed",
            before = {
                // One finger on each of them, all held down.
                val fingers = touched.map { onNodeWithTag(it).fetchSemanticsNode().boundsInRoot.center }
                onRoot().performTouchInput { fingers.forEachIndexed { finger, where -> down(finger, where) } }
            }
        ) {
            Column(Modifier.width(320.dp), verticalArrangement = Arrangement.spacedBy(BcSpacing.m)) {
                BcSummaryRow(
                    text = "Léa, Tom",
                    strong = "2 non lus",
                    leading = BcSummaryLeading.Dots(listOf(BcPastel.Coral, BcPastel.Blue)),
                    onClick = {},
                    modifier = Modifier.testTag("line")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(BcSpacing.l)) {
                    BcPersonTile("Léa", "L", BcPastel.Coral, unread = 1, onClick = {
                    }, modifier = Modifier.testTag("tile"))
                    BcPersonTile("Tom", "T", BcPastel.Blue, unread = 0, onClick = {})
                }
                BcMusicPill("Nekketsu", "Nekfeu", cover = null, playing = false, onClick = {}, Modifier.testTag("pill"))
                BcButton("Reprendre", onClick = {}, Modifier.testTag("button"), variant = BcButtonVariant.Light)
                BcListRow("Spotify", onClick = {}, Modifier.testTag("row"), leading = BcLeading.Initials("S"))
            }
        }

    @Test
    fun `une ligne du résumé en train de glisser, assez loin pour ouvrir, correspond à sa capture`() =
        compose.captureComponent(
            name = "summary_row_sliding",
            before = {
                onNodeWithTag("line").performTouchInput {
                    down(Offset(40f, centerY))
                    moveBy(Offset(40.dp.toPx(), 0f))
                    moveBy(Offset(70.dp.toPx(), 0f))
                }
            }
        ) {
            Column(Modifier.width(320.dp).padding(start = 120.dp)) {
                BcSummaryRow(
                    text = "Léa, Tom",
                    strong = "2 non lus",
                    leading = BcSummaryLeading.Dots(listOf(BcPastel.Coral, BcPastel.Blue)),
                    onClick = {},
                    modifier = Modifier.testTag("line"),
                    swipe = BcRowSwipe("Ouvrir Beeper") {}
                )
            }
        }
}
