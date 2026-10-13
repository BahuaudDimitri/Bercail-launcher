package io.github.bahuauddimitri.bercail

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Dictation, on Android's own screen: the phone listens, shows what it hears, and gives the text back. Bercail
 * never listens by itself and asks for no microphone right: the phone's voice input does the listening.
 * [android] is where the request goes; the tests answer it themselves.
 */
internal class Dictation(
    activity: ComponentActivity,
    android: ActivityResultRegistry = activity.activityResultRegistry
) {
    private var waiting: ((String) -> Unit)? = null

    private val listening =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult(), android) { answer ->
            val heard = answer.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (answer.resultCode == Activity.RESULT_OK && !heard.isNullOrBlank()) waiting?.invoke(heard)
            waiting = null
        }

    /** Listens once, and gives what was heard to [onHeard]. Nothing happens if the user says nothing or gives up. */
    fun listen(onHeard: (String) -> Unit) {
        waiting = onHeard
        try {
            listening.launch(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            )
        } catch (_: ActivityNotFoundException) {
            // This phone has no voice input: the microphone button does nothing.
            waiting = null
        }
    }
}
