package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.bahuauddimitri.bercail.core.designsystem.PIXEL_9
import io.github.bahuauddimitri.bercail.core.designsystem.captureComponent
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class BcNavigationScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val alphabet = listOf("A", "B", "C", "D", "G", "M", "N", "S", "W", "Y")

    private val day = listOf(
        BcTimelineMark(position = 0.06f, label = "08:00"),
        BcTimelineMark(position = 0.15f, label = "09:30", next = true),
        BcTimelineMark(position = 0.47f, label = "15:00"),
        BcTimelineMark(position = 0.76f, label = "20:00")
    )

    @Test
    fun `l'alphabet et la ligne du temps verticale correspondent à leur capture`() =
        compose.captureComponent("alphabet_timeline") {
            Row(horizontalArrangement = Arrangement.spacedBy(BcSpacing.xl)) {
                BcAlphabetRail(alphabet, onLetter = {}, modifier = Modifier.height(360.dp))
                BcTimeline(now = 0.1f, marks = day, modifier = Modifier.height(360.dp).width(64.dp))
            }
        }

    @Test
    fun `la ligne du temps horizontale correspond à sa capture`() = compose.captureComponent("timeline_horizontal") {
        BcTimeline(
            now = 0.1f,
            marks = day,
            orientation = BcTimelineOrientation.Horizontal,
            modifier = Modifier.width(360.dp).height(44.dp)
        )
    }
}
