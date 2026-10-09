package io.github.bahuauddimitri.bercail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

const val HOME_TAG = "home"

/** Empty home screen over the wallpaper; the real one arrives with the design system (wave 3). */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().testTag(HOME_TAG))
}
