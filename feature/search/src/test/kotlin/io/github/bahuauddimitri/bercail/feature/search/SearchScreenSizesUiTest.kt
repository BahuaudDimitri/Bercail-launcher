package io.github.bahuauddimitri.bercail.feature.search

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.apps.App
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Moment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

/**
 * The search on the Pixel 9 and on smaller screens: the whole alphabet fits along the edge, and nothing written
 * overlaps, before typing as while typing.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
class SearchScreenSizesUiTest(size: ScreenSize) {
    @get:Rule(order = 0)
    val screen = ScreenSizeRule(size)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val world = FakeWorld(Moment.Matin)

    private fun show(typed: String = "") {
        val search = world.searchViewModel()
        search.onOpenChange(true)
        search.onQueryChange(typed)
        compose.setContent {
            BcTheme { Box(Modifier.fillMaxSize().background(BcColors.base)) { SearchRoute(search) } }
        }
    }

    @Test
    fun `avant de taper, le titre, la flèche, les Réglages et chaque lettre de l'alphabet tiennent en entier`() {
        // An app for each letter: the longest alphabet there can be.
        world.apps.apps.value = ('A'..'Z').map { App("app.$it", "$it app") }
        show()

        val letters = compose.onAllNodes(hasAnyAncestor(hasTestTag(RAIL_TAG)) and hasClickAction())
        compose.assertWholeAndApart(
            compose.onNodeWithText("Toutes les apps"),
            compose.onNodeWithContentDescription("Fermer la recherche"),
            compose.onNodeWithText("Réglages de Bercail"),
            *Array(LETTERS) { letters[it] }
        )
        compose.assertNothingOverlaps()
    }

    @Test
    fun `en tapant, les résultats par famille ne se chevauchent pas`() {
        show(typed = "l")

        compose.assertNothingOverlaps()
    }

    @Test
    fun `sans résultat, les propositions web et Play Store ne se chevauchent pas`() {
        show(typed = "ramen")

        compose.assertNothingOverlaps()
    }

    companion object {
        private const val LETTERS = 26

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun sizes() = SCREEN_SIZES.map { arrayOf<Any>(it) }
    }
}
