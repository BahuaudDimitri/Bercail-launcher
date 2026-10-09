package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
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

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class BcConversationScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `la barre Chercher correspond à sa capture`() = compose.captureComponent("search_field") {
        Column(Modifier.width(360.dp), verticalArrangement = Arrangement.spacedBy(BcSpacing.s)) {
            BcSearchField(value = "", onValueChange = {})
            BcSearchField(value = "spotify", onValueChange = {})
        }
    }

    @Test
    fun `une conversation correspond à sa capture`() = compose.captureComponent("conversation") {
        Column(Modifier.width(360.dp), verticalArrangement = Arrangement.spacedBy(BcSpacing.s)) {
            BcBubble(
                "Tu es où ? On commence sans toi 😄",
                author = "Léa",
                mine = false,
                pastel = BcPastel.Coral,
                header = "Aujourd'hui"
            )
            BcBubble(
                "J'arrive dans 10 minutes",
                author = "Léa",
                mine = true,
                pastel = BcPastel.Coral,
                modifier = Modifier.align(Alignment.End)
            )
            BcUndoButton(remaining = 0.6f, onUndo = {}, modifier = Modifier.align(Alignment.End))
        }
    }

    @Test
    fun `la pastille musique correspond à sa capture`() = compose.captureComponent("music_pill") {
        Column(verticalArrangement = Arrangement.spacedBy(BcSpacing.s)) {
            BcMusicPill(title = "Nekketsu", artist = "Nekfeu", cover = null, playing = true, onClick = {})
            BcMusicPill(title = "Nekketsu", artist = "Nekfeu", cover = null, playing = false, onClick = {})
        }
    }
}
