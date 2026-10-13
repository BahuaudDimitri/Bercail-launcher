package io.github.bahuauddimitri.bercail.data.media

import android.content.Context
import android.service.notification.NotificationListenerService
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

/**
 * What Android's notifications tell Bercail: that an app just showed or removed one, and nothing else. Only the
 * name of the app is looked at, never what the notification says. The listener is connected to Android only
 * while someone waits for a signal, that is while the home screen is shown; the rest of the time it stays
 * disconnected, so nothing is delivered to Bercail and nothing keeps it alive.
 */
object NotificationSignals {
    private val shown =
        MutableSharedFlow<String>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var waiting = 0
    private var connected: MediaListener? = null

    /**
     * One signal each time the app named [packageName] shows or removes a notification, as long as someone
     * listens. Without the "notification access", there is never any.
     */
    fun from(context: Context, packageName: String): Flow<Unit> {
        val phone = context.applicationContext
        return shown.filter { it == packageName }.map { }.onStart { attach(phone) }.onCompletion { detach() }
    }

    @Synchronized
    private fun attach(context: Context) {
        val first = waiting++ == 0
        if (first && connected == null && MediaAccess.granted(context)) {
            NotificationListenerService.requestRebind(MediaAccess.listener(context))
        }
    }

    @Synchronized
    private fun detach() {
        if (--waiting > 0) return
        connected?.requestUnbind()
        connected = null
    }

    /** Android just connected the listener: it only stays connected if someone waits for a signal. */
    @Synchronized
    internal fun onConnected(listener: MediaListener) {
        if (waiting > 0) connected = listener else listener.requestUnbind()
    }

    @Synchronized
    internal fun onDisconnected(listener: MediaListener) {
        if (connected === listener) connected = null
    }

    internal fun tell(packageName: String) {
        shown.tryEmit(packageName)
    }
}
