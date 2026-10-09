package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.bahuauddimitri.bercail.core.designsystem.PIXEL_9
import io.github.bahuauddimitri.bercail.core.designsystem.captureComponent
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class BcBasicsScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `les styles de texte correspondent à leur capture`() = compose.captureComponent("text_styles") {
        Column {
            BcTextStyle.entries.forEach { style -> BcText(style.name, style = style) }
            BcText("Texte discret", color = BcTextColor.Muted)
        }
    }

    @Test
    fun `les icônes correspondent à leur capture`() = compose.captureComponent("icons") {
        Column(verticalArrangement = Arrangement.spacedBy(BcSpacing.s)) {
            BcIcons.entries.chunked(ICONS_PER_ROW).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(BcSpacing.m)) {
                    row.forEach { BcIcon(it, contentDescription = null) }
                }
            }
        }
    }

    @Test
    fun `les boutons correspondent à leur capture`() = compose.captureComponent("buttons") {
        Column(verticalArrangement = Arrangement.spacedBy(BcSpacing.s)) {
            BcButton("Ouvrir dans Spotify", onClick = {})
            BcButton("Reprendre", onClick = {}, variant = BcButtonVariant.Light, icon = BcIcons.Play)
            BcButton("Envoyer", onClick = {}, variant = BcButtonVariant.Light, enabled = false)
            Row(horizontalArrangement = Arrangement.spacedBy(BcSpacing.s)) {
                BcIconButton(BcIcons.Mic, contentDescription = "Dicter", onClick = {})
                BcIconButton(BcIcons.Send, "Envoyer", onClick = {}, variant = BcButtonVariant.Light)
                BcIconButton(BcIcons.Back, "Retour", onClick = {}, variant = BcButtonVariant.Ghost)
            }
        }
    }

    @Test
    fun `les pastilles correspondent à leur capture`() = compose.captureComponent("pills") {
        Row(horizontalArrangement = Arrangement.spacedBy(BcSpacing.xs)) {
            BcPill("J'arrive", onClick = {})
            BcPill("Écouter…", onClick = {}, icon = BcIcons.Note)
        }
    }

    private companion object {
        const val ICONS_PER_ROW = 8
    }
}
