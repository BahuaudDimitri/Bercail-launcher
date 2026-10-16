package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.bahuauddimitri.bercail.core.designsystem.PIXEL_9
import io.github.bahuauddimitri.bercail.core.designsystem.captureComponent
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcMoment
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcMomentScope
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
        BcTimelineMark(position = 0.06f, label = "08:00", title = "Stand-up"),
        BcTimelineMark(position = 0.15f, label = "09:30", next = true, title = "Café avec Tom"),
        BcTimelineMark(position = 0.47f, label = "15:00", title = "Point design"),
        BcTimelineMark(position = 0.76f, label = "20:00", title = "Dîner")
    )

    @Test
    fun `l'alphabet et la ligne du temps verticale correspondent à leur capture`() =
        compose.captureComponent("alphabet_timeline") {
            Row(horizontalArrangement = Arrangement.spacedBy(BcSpacing.xl)) {
                BcAlphabetRail(alphabet, onLetter = {}, modifier = Modifier.height(360.dp))
                BcTimeline(now = 0.1f, marks = day, modifier = Modifier.height(360.dp).width(220.dp))
            }
        }

    @Test
    fun `le fil au matin, à la journée et au soir correspond à sa capture`() =
        compose.captureComponent("timeline_moments") {
            Row(horizontalArrangement = Arrangement.spacedBy(BcSpacing.l)) {
                listOf(
                    BcMoment.Morning to 0.08f,
                    BcMoment.Day to 0.46f,
                    BcMoment.Evening to 0.8f
                ).forEach { (moment, now) ->
                    BcMomentScope(moment) {
                        BcTimeline(
                            now = now,
                            // Only the times: the titles would run into the next column.
                            marks = day.map {
                                it.copy(title = "", next = it == day.firstOrNull { m -> m.position > now })
                            },
                            headroom = 24.dp,
                            modifier = Modifier.height(360.dp).width(64.dp)
                        )
                    }
                }
            }
        }

    @Test
    fun `le fil avec ses titres, un rendez-vous trop proche et le haut réservé correspond à sa capture`() =
        compose.captureComponent("timeline_titles") {
            BcTimeline(
                now = 0.46f,
                marks = listOf(
                    BcTimelineMark(0.147f, "09:30", title = "Stand-up"),
                    BcTimelineMark(0.324f, "12:30", title = "Déjeuner avec Léa"),
                    BcTimelineMark(0.471f, "15:00", next = true, title = "Point design"),
                    BcTimelineMark(0.495f, "15:25", title = "Café avec Tom", brief = true),
                    BcTimelineMark(0.676f, "18:30", title = "Courses"),
                    BcTimelineMark(0.824f, "21:00", title = "Un ciné à la maison avec toute la famille")
                ),
                headroom = 24.dp,
                titlesBelow = 80.dp,
                modifier = Modifier.height(520.dp).width(260.dp)
            )
        }

    @Test
    fun `l'alphabet touché, avec sa vague et sa bulle, correspond à sa capture`() = compose.captureComponent(
        name = "alphabet_wave",
        before = { onNodeWithTag("az").performTouchInput { down(Offset(centerX, height * 0.55f)) } }
    ) {
        Box(Modifier.width(220.dp).height(520.dp), contentAlignment = Alignment.CenterEnd) {
            BcAlphabetRail(
                letters = ('A'..'Z').map { it.toString() },
                onLetter = {},
                modifier = Modifier.fillMaxHeight().testTag("az")
            )
        }
    }

    @Test
    fun `la ligne du temps touchée, courbée autour du doigt avec sa bulle, correspond à sa capture`() =
        compose.captureComponent(
            name = "timeline_wave",
            before = { onNodeWithTag("line").performTouchInput { down(Offset(width - 20f, height * 0.47f)) } }
        ) {
            Box(Modifier.width(300.dp).height(420.dp), contentAlignment = Alignment.CenterEnd) {
                BcTimeline(
                    now = 0.1f,
                    marks = day,
                    modifier = Modifier.fillMaxHeight().width(78.dp).testTag("line"),
                    onOpen = {}
                )
            }
        }
}
