package io.github.bahuauddimitri.bercail.core.designsystem.theme

import android.provider.Settings
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BcThemeUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `le thème garde les animations par défaut`() {
        var motion: BcMotion? = null

        compose.setContent { BcTheme { motion = BcTheme.motion } }

        assertThat(motion?.reduced).isEqualTo(false)
        assertThat(motion?.drawerMillis).isEqualTo(400)
    }

    @Test
    fun `le thème coupe les animations quand Android les supprime`() {
        val resolver = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        var reduced: Boolean? = null

        compose.setContent { BcTheme { reduced = BcTheme.motion.reduced } }

        assertThat(reduced).isEqualTo(true)
    }

    @Test
    fun `le texte prend la couleur principale par défaut`() {
        var isDefault: Boolean? = null

        compose.setContent { BcTheme { isDefault = LocalBcContentColor.current == BcColors.text } }

        assertThat(isDefault).isEqualTo(true)
    }
}
