package io.github.bahuauddimitri.bercail.data.media

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * The listener Android asks for before it gives the "notification access", which is the only way to see the media
 * sessions. It reads no notification. While nobody waits for a signal it asks to be disconnected as soon as
 * Android connects it; while the home screen waits to know that a message arrived, it stays connected and only
 * passes on the name of the app that showed or removed a notification: see [NotificationSignals].
 */
class MediaListener : NotificationListenerService() {
    override fun onListenerConnected() = NotificationSignals.onConnected(this)

    override fun onListenerDisconnected() = NotificationSignals.onDisconnected(this)

    override fun onNotificationPosted(shown: StatusBarNotification) = NotificationSignals.tell(shown.packageName)

    override fun onNotificationRemoved(removed: StatusBarNotification) = NotificationSignals.tell(removed.packageName)
}

/** The "notification access" of Bercail: whether the user gave it, and where it is given. */
object MediaAccess {
    fun listener(context: Context) = ComponentName(context, MediaListener::class.java)

    fun granted(context: Context): Boolean = checkNotNull(context.getSystemService(NotificationManager::class.java))
        .isNotificationListenerAccessGranted(listener(context))

    /** The page of Android's settings where the user gives that access to Bercail, or takes it back. */
    fun question(context: Context): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
        .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, listener(context).flattenToString())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
