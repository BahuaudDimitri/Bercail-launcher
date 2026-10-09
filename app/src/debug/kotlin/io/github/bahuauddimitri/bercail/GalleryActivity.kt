package io.github.bahuauddimitri.bercail

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcBrume
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcBrumeColors
import io.github.bahuauddimitri.bercail.core.designsystem.gallery.BcGallery
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import kotlinx.coroutines.delay

/**
 * The design system gallery. With the extra `brume` set to `playing` or `still`, shows Brume full screen with the
 * screen kept on, for energy measurements:
 * `adb shell am start --es brume playing -n <package>/io.github.bahuauddimitri.bercail.GalleryActivity`,
 * where the package is io.github.bahuauddimitri.bercail.debug.
 */
class GalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val brume = intent.getStringExtra(EXTRA_BRUME)
        if (brume != null) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            BcTheme {
                if (brume == null) {
                    BcGallery()
                } else {
                    // Measurement mode: no touch can reach the screen, so Brume would freeze after 10 s;
                    // it is kept moving on purpose by restarting the wake key every 9 s.
                    MeasuredBrume(playing = brume == "playing")
                }
            }
        }
    }

    private companion object {
        const val EXTRA_BRUME = "brume"
    }
}

@Composable
private fun MeasuredBrume(playing: Boolean) {
    val tick = produceState(0) {
        while (true) {
            delay(KEEP_AWAKE_MILLIS)
            value++
        }
    }
    BcBrume(colors = BcBrumeColors.Evening, playing = playing, wakeKey = tick.value, modifier = Modifier.fillMaxSize())
}

private const val KEEP_AWAKE_MILLIS = 9_000L
