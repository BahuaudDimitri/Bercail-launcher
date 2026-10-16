package io.github.bahuauddimitri.bercail

import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.AlarmClock
import androidx.core.content.ContextCompat
import io.github.bahuauddimitri.bercail.core.domain.time.AlarmSource
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * The alarm set in the phone's clock app. It is only read, never set: once when the home screen listens, then each
 * time Android announces that it changed. Nothing is registered while nobody listens.
 */
class DeviceAlarm(private val context: Context) : AlarmSource {
    override val next: Flow<LocalDateTime?> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(read())
            }
        }
        // This broadcast only comes from the system.
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        trySend(read())
        awaitClose { context.unregisterReceiver(receiver) }
    }.distinctUntilChanged()

    private fun read(): LocalDateTime? = context.getSystemService(AlarmManager::class.java)?.nextAlarmClock
        ?.let { Instant.ofEpochMilli(it.triggerTime).atZone(ZoneId.systemDefault()).toLocalDateTime() }

    override fun openAlarms() {
        try {
            context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // No clock app on this phone: the screen simply stays where it is.
        } catch (_: SecurityException) {
            // A clock app that refuses to be opened: the screen stays where it is.
        }
    }
}
