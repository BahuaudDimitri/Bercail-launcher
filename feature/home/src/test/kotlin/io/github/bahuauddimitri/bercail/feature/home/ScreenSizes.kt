package io.github.bahuauddimitri.bercail.feature.home

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.robolectric.RuntimeEnvironment

/** A phone screen, by its size in dp, as Android gives it to the app. */
class ScreenSize(private val name: String, val qualifiers: String) {
    override fun toString() = name
}

/**
 * The Pixel 9 as it comes, then smaller screens: a common smaller phone, two small ones, and the Pixel 9 with its
 * « Taille d'affichage » at the largest.
 */
val SCREEN_SIZES = listOf(
    ScreenSize("Pixel 9, 411 × 923", PIXEL_9),
    ScreenSize("393 × 780", "w393dp-h780dp-420dpi"),
    ScreenSize("360 × 640", "w360dp-h640dp-420dpi"),
    ScreenSize("320 × 640", "w320dp-h640dp-420dpi"),
    ScreenSize("Pixel 9 en très grande taille d'affichage, 360 × 808", "w360dp-h808dp-480dpi")
)

/** Gives the simulated phone the screen [size] before the test's activity starts: put it before the compose rule. */
class ScreenSizeRule(private val size: ScreenSize) : TestRule {
    override fun apply(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            RuntimeEnvironment.setQualifiers(size.qualifiers)
            base.evaluate()
        }
    }
}

/**
 * Checks that each of [things] is shown whole, within the screen and not cut by what holds it, and that no two of
 * them overlap.
 */
fun SemanticsNodeInteractionsProvider.assertWholeAndApart(vararg things: SemanticsNodeInteraction) {
    val screen = onRoot().getUnclippedBoundsInRoot()
    val boxes = things.map { thing ->
        val box = thing.getUnclippedBoundsInRoot()
        val name = thing.name()
        check(box.width > 0.dp && box.height > 0.dp) { "« $name » n'a pas de place : $box" }
        check(box.inside(screen)) { "« $name » sort de l'écran $screen : $box" }
        check(thing.getBoundsInRoot().covers(box)) { "« $name » est coupé : ${thing.getBoundsInRoot()} sur $box" }
        name to box
    }
    boxes.forEachIndexed { index, (name, box) ->
        boxes.drop(index + 1).forEach { (other, otherBox) ->
            check(!box.overlaps(otherBox)) { "« $name » ($box) chevauche « $other » ($otherBox)" }
        }
    }
}

/**
 * Checks that nothing written or drawn on the screen (each text, each icon) overlaps another, and that each stands
 * within the width of the screen. What a list scrolls out of sight counts too: it would overlap once scrolled to.
 */
fun SemanticsNodeInteractionsProvider.assertNothingOverlaps() {
    val screen = onRoot().getUnclippedBoundsInRoot()
    val leaves = onAllNodes(isLeaf, useUnmergedTree = true).fetchSemanticsNodes().map { node ->
        val position = node.positionInRoot
        val box = with(node.layoutInfo.density) {
            DpRect(
                position.x.toDp(),
                position.y.toDp(),
                (position.x + node.size.width).toDp(),
                (position.y + node.size.height).toDp()
            )
        }
        node.name() to box
    }.filter { (_, box) -> box.width > 0.dp && box.height > 0.dp }
    leaves.forEachIndexed { index, (name, box) ->
        check(box.left >= screen.left - SLACK && box.right <= screen.right + SLACK) {
            "« $name » sort de l'écran sur le côté : $box"
        }
        leaves.drop(index + 1).forEach { (other, otherBox) ->
            check(!box.overlaps(otherBox)) { "« $name » ($box) chevauche « $other » ($otherBox)" }
        }
    }
}

/** Something written, or an icon that says what it is, holding nothing else. */
private val isLeaf = SemanticsMatcher("écrit ou dessiné, seul") { node ->
    node.children.isEmpty() &&
        (SemanticsProperties.Text in node.config || SemanticsProperties.ContentDescription in node.config)
}

private fun SemanticsNodeInteraction.name() = fetchSemanticsNode().name()

private fun SemanticsNode.name(): String = config.getOrNull(SemanticsProperties.Text)?.joinToString()
    ?: config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
    ?: config.getOrNull(SemanticsProperties.TestTag)
    ?: "?"

private val SLACK = 0.5.dp

private fun DpRect.inside(outer: DpRect) =
    left >= outer.left - SLACK && top >= outer.top - SLACK && right <= outer.right + SLACK &&
        bottom <= outer.bottom + SLACK

private fun DpRect.covers(inner: DpRect) = inner.inside(this)

private fun DpRect.overlaps(other: DpRect) =
    left < other.right - SLACK && other.left < right - SLACK && top < other.bottom - SLACK &&
        other.top < bottom - SLACK
