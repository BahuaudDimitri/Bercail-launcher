package io.github.bahuauddimitri.bercail

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import io.github.bahuauddimitri.bercail.core.domain.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * The phone's clock. Android itself announces each new minute (and every change of time or time zone) while the
 * screen is on: no timer, no loop, and nothing registered while nobody listens.
 */
class DeviceClock(private val context: Context, private val now: () -> LocalDateTime = LocalDateTime::now) : Clock {
    override fun now(): LocalDateTime = now.invoke()

    override val minutes: Flow<LocalDateTime> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(now())
            }
        }
        val changes = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        // These broadcasts only come from the system.
        ContextCompat.registerReceiver(context, receiver, changes, ContextCompat.RECEIVER_NOT_EXPORTED)
        trySend(now())
        awaitClose { context.unregisterReceiver(receiver) }
    }
}
