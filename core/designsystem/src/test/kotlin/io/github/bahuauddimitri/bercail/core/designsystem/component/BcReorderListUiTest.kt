package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** A list put in order by hand: each row moves by its handle, or by « Monter » and « Descendre ». */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BcReorderListUiTest {
    @get:Rule
    val compose = createComposeRule()

    private var order by mutableStateOf(listOf("Chrome", "Téléphone", "Beeper", "Agenda"))
    private val moves = mutableListOf<Pair<Int, Int>>()

    private fun show() = compose.setContent {
        BcTheme {
            BcReorderList(
                rows = order.map { name -> BcReorderRow(name) { BcListRow(name, onClick = {}, modifier = it) } },
                onMove = { from, to ->
                    moves += from to to
                    order = order.toMutableList().apply { add(to, removeAt(from)) }
                }
            )
        }
    }

    private fun handleOf(name: String) = compose.onNodeWithTag(BC_REORDER_HANDLE_TAG + name)

    /** Drags the handle by [rows] rows, down if positive. */
    private fun SemanticsNodeInteraction.dragBy(rows: Float) {
        val row = compose.onNodeWithText("Chrome").fetchSemanticsNode().size.height
        performTouchInput {
            down(center)
            moveBy(Offset(0f, viewConfiguration.touchSlop + 1f))
            moveBy(Offset(0f, rows * row))
            up()
        }
    }

    private fun actionsOf(name: String): Map<String, () -> Boolean> {
        val node = compose.onNodeWithText(name).fetchSemanticsNode()
        return node.config.getOrNull(SemanticsActions.CustomActions).orEmpty().associate { it.label to it.action }
    }

    @Test
    fun `glisser la poignée d'une ligne vers le bas la pose plus loin`() {
        show()

        handleOf("Chrome").dragBy(rows = 2f)

        assertThat(moves).containsExactly(0 to 2)
        assertThat(order).containsExactly("Téléphone", "Beeper", "Chrome", "Agenda")
    }

    @Test
    fun `glisser la poignée vers le haut remonte la ligne, jamais au-delà du début`() {
        show()

        handleOf("Agenda").dragBy(rows = -9f)

        assertThat(order).containsExactly("Agenda", "Chrome", "Téléphone", "Beeper")
    }

    @Test
    fun `un glissement trop court laisse la ligne à sa place`() {
        show()

        handleOf("Beeper").dragBy(rows = 0.3f)

        assertThat(moves).isEmpty()
    }

    @Test
    fun `le lecteur d'écran offre « Monter » et « Descendre », sauf au bord de la liste`() {
        show()

        assertThat(actionsOf("Chrome").keys.toList()).containsExactly("Descendre")
        assertThat(actionsOf("Téléphone").keys.toList()).containsExactly("Monter", "Descendre")
        assertThat(actionsOf("Agenda").keys.toList()).containsExactly("Monter")

        compose.runOnIdle { actionsOf("Téléphone").getValue("Monter")() }
        compose.runOnIdle { actionsOf("Beeper").getValue("Descendre")() }

        assertThat(moves).containsExactly(1 to 0, 2 to 3)
        assertThat(order).containsExactly("Téléphone", "Chrome", "Agenda", "Beeper")
    }
}
