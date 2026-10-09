package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
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
class BcDrawerScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `le tiroir replié correspond à sa capture`() = compose.captureComponent("drawer_collapsed") {
        BcDrawer(expanded = false, onExpandedChange = {}, modifier = Modifier.width(380.dp)) {
            BcText("2 non lus · Léa, Tom", style = BcTextStyle.BodySmall, color = BcTextColor.Muted)
            BcText("Salon 60 %, Volets ouverts", style = BcTextStyle.BodySmall, color = BcTextColor.Muted)
            BcSearchField(value = "", onValueChange = {})
        }
    }

    @Test
    fun `le tiroir ouvert correspond à sa capture`() = compose.captureComponent("drawer_expanded") {
        BcDrawer(expanded = true, onExpandedChange = {}, modifier = Modifier.width(380.dp)) {
            Row(Modifier.width(348.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                BcPersonTile("Léa", "L", BcPastel.Coral, unread = 2, onClick = {})
                BcPersonTile("Tom", "T", BcPastel.Blue, unread = 0, onClick = {})
                BcPersonTile("Équipe", "É", BcPastel.Mint, unread = 0, onClick = {})
                BcPersonTile("Maman", "M", BcPastel.Lavender, unread = 0, onClick = {})
                BcPersonTile("Tous", "+", pastel = null, unread = 3, onClick = {})
            }
            Column(verticalArrangement = Arrangement.spacedBy(BcSpacing.s)) {
                BcSearchField(value = "", onValueChange = {})
            }
        }
    }
}
