package io.github.bahuauddimitri.bercail

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcBrume
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcBrumeColors
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcBandSheet
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIcons
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRing
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRingMark
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRingTone
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSectionLabel
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcText
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextStyle
import io.github.bahuauddimitri.bercail.core.designsystem.component.frostSource
import io.github.bahuauddimitri.bercail.core.designsystem.component.rememberBcFrost
import io.github.bahuauddimitri.bercail.core.designsystem.gallery.BcGallery
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import kotlinx.coroutines.delay

/**
 * The design system gallery. With the extra `brume` set to `playing` or `still`, shows Brume full screen with the
 * screen kept on, for energy measurements:
 * `adb shell am start --es brume playing -n <package>/io.github.bahuauddimitri.bercail.GalleryActivity`,
 * where the package is io.github.bahuauddimitri.bercail.debug.
 *
 * With the extra `sheet` as well, a sheet of people and house stands over Brume, to measure its glass:
 * `frosted` (Brume blurred behind it), `tinted` (the same sheet without the blur), `blinking` (frosted, with a
 * line that blinks twice a second, like the caret of a field: every blink redraws the screen) or `toggling`
 * (frosted, opening and closing every two seconds).
 */
class GalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val brume = intent.getStringExtra(EXTRA_BRUME)
        val sheet = intent.getStringExtra(EXTRA_SHEET)
        if (brume != null) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            BcTheme {
                if (brume == null) {
                    BcGallery()
                } else {
                    // Measurement mode: no touch can reach the screen, so Brume would freeze after 10 s;
                    // it is kept moving on purpose by restarting the wake key every 9 s.
                    MeasuredBrume(playing = brume == "playing", sheet = sheet)
                }
            }
        }
    }

    private companion object {
        const val EXTRA_BRUME = "brume"
        const val EXTRA_SHEET = "sheet"
    }
}

@Composable
private fun MeasuredBrume(playing: Boolean, sheet: String?) {
    val tick = produceState(0) {
        while (true) {
            delay(KEEP_AWAKE_MILLIS)
            value++
        }
    }
    val frost = rememberBcFrost()
    Box(Modifier.fillMaxSize()) {
        BcBrume(
            colors = BcBrumeColors.Evening,
            playing = playing,
            wakeKey = tick.value,
            modifier = Modifier.fillMaxSize().frostSource(frost)
        )
        if (sheet != null) {
            val open = if (sheet == "toggling") ticking(TOGGLE_MILLIS) else true
            BcBandSheet(
                visible = open,
                onClose = {},
                modifier = Modifier.align(Alignment.BottomCenter),
                frost = if (sheet == "tinted") null else frost
            ) {
                MeasuredSheet(blinking = sheet == "blinking")
            }
        }
    }
}

/** True and false in turn, every [millis]. */
@Composable
private fun ticking(millis: Long): Boolean = produceState(true) {
    while (true) {
        delay(millis)
        value = !value
    }
}.value

@Composable
private fun MeasuredSheet(blinking: Boolean) {
    BcSectionLabel("Gens")
    BcRing("Léa", BcRingMark.Initial("L"), {}, tone = BcRingTone.Pastel(BcPastel.Coral), unread = 2)
    BcSectionLabel("Maison")
    BcRing("Salon", "60 %", BcRingMark.Icon(BcIcons.Lamp), BcPastel.Apricot, on = true, onToggle = {})
    if (blinking) {
        val shown = ticking(BLINK_MILLIS)
        BcText(if (shown) "|" else " ", style = BcTextStyle.Body)
    }
}

private const val KEEP_AWAKE_MILLIS = 9_000L
private const val TOGGLE_MILLIS = 2_000L
private const val BLINK_MILLIS = 500L
