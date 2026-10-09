package io.github.bahuauddimitri.bercail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The home screen is the bottom of the stack: back has nowhere to go.
        onBackPressedDispatcher.addCallback(this) {}
        setContent { HomeScreen() }
    }
}
