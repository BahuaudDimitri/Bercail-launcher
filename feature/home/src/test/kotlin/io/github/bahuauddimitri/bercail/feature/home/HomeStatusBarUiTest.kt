package io.github.bahuauddimitri.bercail.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Moment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The phone's own status bar (time, battery, network): hidden on Écoute only, given back as soon as one leaves. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PIXEL_9)
class HomeStatusBarUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val world = FakeWorld(Moment.Matin)

    private fun show() {
        val home = world.homeViewModel()
        compose.setContent { BcTheme { HomeRoute(home) } }
        compose.waitForIdle()
    }

    private fun listen() {
        show()
        compose.onNodeWithText("Looped · Kiasmos").performClick()
        compose.waitForIdle()
    }

    private fun statusBarShown(): Boolean {
        val decor = compose.activity.window.decorView
        return ViewCompat.getRootWindowInsets(decor)!!.isVisible(WindowInsetsCompat.Type.statusBars())
    }

    @Test
    fun `sur l'Accueil, la barre d'état du téléphone reste affichée`() {
        show()

        assertThat(statusBarShown()).isTrue()
    }

    @Test
    fun `sur Écoute, la barre d'état est cachée, et un glissement depuis le bord haut la montre un instant`() {
        listen()

        assertThat(statusBarShown()).isFalse()
        val bars = WindowCompat.getInsetsController(compose.activity.window, compose.activity.window.decorView)
        assertThat(
            bars.systemBarsBehavior
        ).isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)
    }

    @Test
    fun `dès que le doigt commence à remonter l'arche, la barre d'état revient`() {
        listen()

        compose.onNodeWithTag(PLAYER_TAG).performTouchInput {
            down(Offset(centerX, height * LOW_ON_SCREEN))
            moveBy(Offset(0f, -viewConfiguration.touchSlop - LIFT))
        }
        compose.waitForIdle()

        assertThat(statusBarShown()).isTrue()
    }

    @Test
    fun `revenue à l'Accueil, la barre d'état est rendue`() {
        listen()

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()

        assertThat(statusBarShown()).isTrue()
    }

    @Test
    fun `quand l'accueil n'est plus à l'écran, la barre d'état est rendue, et recachée au retour sur Écoute`() {
        listen()

        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        assertThat(statusBarShown()).isTrue()

        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()
        assertThat(statusBarShown()).isFalse()
    }

    private companion object {
        const val LOW_ON_SCREEN = 0.9f
        const val LIFT = 60f
    }
}
