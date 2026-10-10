package io.github.bahuauddimitri.bercail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.feature.home.HomeRoute
import io.github.bahuauddimitri.bercail.feature.home.HomeViewModel

class MainActivity : ComponentActivity() {
    private val home: HomeViewModel by viewModels {
        viewModelFactory { initializer { homeViewModel(applicationContext) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The home screen is the bottom of the stack: back has nowhere to go.
        onBackPressedDispatcher.addCallback(this) {}
        setContent { BcTheme { HomeRoute(home) } }
    }
}
