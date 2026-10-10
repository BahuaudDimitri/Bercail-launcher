package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.designsystem.PIXEL_9
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The equalizer of the music pill only costs frames while it has a reason to move. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class BcMusicPillUiTest {
    @get:Rule
    val compose = createComposeRule()

    private fun showPill(playing: Boolean, still: Boolean) {
        // Frames are advanced by hand: the test rule would otherwise skip never-ending animations.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            BcTheme {
                BcMusicPill(
                    title = "Looped",
                    artist = "Kiasmos",
                    cover = null,
                    playing = playing,
                    onClick = {},
                    still = still,
                    modifier = Modifier.testTag("pill")
                )
            }
        }
        compose.mainClock.advanceTimeBy(200)
    }

    private fun picture() = compose.onNodeWithTag("pill").captureToImage().asAndroidBitmap()

    private fun changesWithin(millis: Long): Boolean {
        val before = picture()
        compose.mainClock.advanceTimeBy(millis)
        return !before.sameAs(picture())
    }

    @Test
    fun `en lecture, l'égaliseur bouge`() {
        showPill(playing = true, still = false)

        assertThat(changesWithin(millis = 300)).isTrue()
    }

    @Test
    fun `en pause, l'égaliseur est figé`() {
        showPill(playing = false, still = false)

        assertThat(changesWithin(millis = 300)).isFalse()
    }

    @Test
    fun `quand l'écran se met au repos, l'égaliseur se fige même en lecture`() {
        showPill(playing = true, still = true)

        assertThat(changesWithin(millis = 300)).isFalse()
    }
}
