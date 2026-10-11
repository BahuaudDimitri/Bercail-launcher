package io.github.bahuauddimitri.bercail

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import io.github.bahuauddimitri.bercail.core.domain.update.InstallAnswer
import io.github.bahuauddimitri.bercail.core.domain.update.Version
import io.github.bahuauddimitri.bercail.core.testing.FakeReleaseFeed
import io.github.bahuauddimitri.bercail.core.testing.FakeUpdateJournal
import io.github.bahuauddimitri.bercail.core.testing.FakeUpdatePackages
import io.github.bahuauddimitri.bercail.core.testing.release
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Bercail updates itself as the home screen comes and goes, on a simulated phone running version 0.8.1 while
 * GitHub announces the 0.9.0.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h923dp-420dpi")
class SelfUpdateUiTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app = ApplicationProvider.getApplicationContext<BercailApplication>()
    private val feed = FakeReleaseFeed(published = release("0.9.0"))
    private val packages = FakeUpdatePackages()

    private fun showHome(): ActivityScenario<MainActivity> {
        val update = UpdateParts(Version(0, 8, 1), feed, packages, FakeUpdateJournal(), Dispatchers.Unconfined)
        app.sources = Sources(app, update)
        return ActivityScenario.launch(MainActivity::class.java)
    }

    private fun ActivityScenario<MainActivity>.hideHome() = moveToState(Lifecycle.State.CREATED)

    private fun ActivityScenario<MainActivity>.comeBackHome() = moveToState(Lifecycle.State.RESUMED)

    @Test
    fun `quand l'accueil s'affiche, Bercail lit les versions publiées et télécharge la plus récente`() {
        showHome().use {
            assertThat(feed.reads).isEqualTo(1)
            assertThat(packages.downloads).containsExactly(Version(0, 9, 0))
        }
    }

    @Test
    fun `tant que l'accueil est affiché, rien ne s'installe`() {
        showHome().use {
            assertThat(packages.installs).isEmpty()
        }
    }

    @Test
    fun `quand l'accueil est caché, la version prête s'installe sans rien demander`() {
        showHome().use { home ->
            home.hideHome()

            assertThat(packages.installs).containsExactly("0.9.0 without asking")
        }
    }

    @Test
    fun `revenir à l'accueil le même jour ne relit pas les versions publiées`() {
        packages.answer = InstallAnswer.NeedsUser
        showHome().use { home ->
            home.hideHome()
            home.comeBackHome()
            home.hideHome()
            home.comeBackHome()

            assertThat(feed.reads).isEqualTo(1)
            assertThat(packages.downloads.size).isEqualTo(1)
        }
    }

    @Test
    fun `une panne de la mise à jour ne fait pas tomber l'accueil`() {
        feed.breakdown = IllegalStateException("imprévu")
        packages.breakdown = IllegalStateException("imprévu")

        showHome().use { home ->
            home.hideHome()
            home.comeBackHome()

            assertThat(home.state).isEqualTo(Lifecycle.State.RESUMED)
            await { compose.onAllNodesWithContentDescription("Chercher").fetchSemanticsNodes().isNotEmpty() }
        }
    }

    @Test
    fun `si Android veut un accord, les Réglages de Bercail proposent d'installer la version d'un toucher`() {
        packages.answer = InstallAnswer.NeedsUser
        showHome().use { home ->
            home.hideHome()
            home.comeBackHome()
            await { compose.onAllNodesWithContentDescription("Chercher").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Chercher").performClick()
            await { compose.onAllNodesWithText("Réglages de Bercail").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Réglages de Bercail").performClick()
            await { compose.onAllNodesWithText("Bercail 0.8.1").fetchSemanticsNodes().isNotEmpty() }

            compose.onNodeWithText("Version 0.9.0 prête · toucher pour l'installer").performScrollTo().performClick()

            await { packages.installs.size == 2 }
            assertThat(packages.installs).containsExactly("0.9.0 without asking", "0.9.0 asking the user")
        }
    }

    private fun await(condition: () -> Boolean) = compose.waitUntil(TIMEOUT, condition)

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
