package io.github.bahuauddimitri.bercail.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme

/** Screen of the reference phone: 1080 × 2424 px at 420 dpi. */
const val PIXEL_9 = "w411dp-h923dp-420dpi"

private const val CAPTURE = "capture"

/** Renders [content] on the night background, inside BcTheme, and compares it with its committed reference. */
fun ComposeContentTestRule.captureComponent(name: String, content: @Composable () -> Unit) {
    setContent {
        BcTheme {
            Box(Modifier.testTag(CAPTURE).background(BcColors.base).padding(BcSpacing.l)) { content() }
        }
    }
    // One more frame before capturing: an image still being drawn sometimes comes out blank.
    mainClock.advanceTimeByFrame()
    waitForIdle()
    onNodeWithTag(CAPTURE).captureRoboImage("src/test/screenshots/$name.png")
}
