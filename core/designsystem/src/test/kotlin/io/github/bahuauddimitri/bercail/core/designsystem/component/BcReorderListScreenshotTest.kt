package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.bahuauddimitri.bercail.core.designsystem.PIXEL_9
import io.github.bahuauddimitri.bercail.core.designsystem.captureComponent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class BcReorderListScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `une liste à remettre en ordre, chaque ligne avec sa poignée, correspond à sa capture`() =
        compose.captureComponent("reorder_list") {
            BcReorderList(
                rows = listOf("Chrome", "Téléphone", "Beeper").mapIndexed { index, name ->
                    BcReorderRow(name) {
                        BcListRow(
                            name,
                            onClick = {},
                            modifier = it,
                            subtitle = "Place ${index + 1}",
                            leading = BcLeading.Initials(name.take(1))
                        )
                    }
                },
                onMove = { _, _ -> },
                modifier = Modifier.width(360.dp)
            )
        }
}
