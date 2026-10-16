package io.github.bahuauddimitri.bercail.core.designsystem.theme

import android.provider.Settings
import androidx.compose.foundation.LocalIndication
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle

internal val LocalBcMotion = staticCompositionLocalOf { BcMotion.forAnimatorScale(1f) }
internal val LocalBcContentColor = compositionLocalOf { BcColors.text }
internal val LocalBcTextStyle = compositionLocalOf<TextStyle> { BcType.body }
internal val LocalBcMoment = compositionLocalOf { BcMoment.Day }

/**
 * Root of every Bercail screen: provides the motion settings, the default text color and style, and the veil shown
 * over whatever is being touched.
 */
@Composable
fun BcTheme(content: @Composable () -> Unit) {
    val resolver = LocalContext.current.contentResolver
    val motion = remember(resolver) {
        BcMotion.forAnimatorScale(Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f))
    }
    CompositionLocalProvider(
        LocalBcMotion provides motion,
        LocalBcContentColor provides BcColors.text,
        LocalBcTextStyle provides BcType.body,
        LocalIndication provides BcPressVeil.OnDark,
        content = content
    )
}

/** Read access to the theme values that depend on the phone's settings. */
object BcTheme {
    val motion: BcMotion
        @Composable @ReadOnlyComposable
        get() = LocalBcMotion.current

    /** The moment of the screen, which gives it its accent. */
    val moment: BcMoment
        @Composable @ReadOnlyComposable
        get() = LocalBcMoment.current

    internal val contentColor: Color
        @Composable @ReadOnlyComposable
        get() = LocalBcContentColor.current
}
