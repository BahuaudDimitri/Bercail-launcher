package io.github.bahuauddimitri.bercail.core.designsystem.gallery

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BcGalleryUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `la galerie présente chaque famille de composants`() {
        compose.setContent { BcTheme { BcGallery() } }

        GALLERY_SECTIONS.forEach { section -> compose.onNodeWithText(section).performScrollTo().assertExists() }
    }
}
