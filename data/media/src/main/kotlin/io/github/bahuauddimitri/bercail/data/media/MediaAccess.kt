package io.github.bahuauddimitri.bercail.data.media

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService

/**
 * Exists only so that Android lets Bercail see the media sessions: that right comes with "notification access",
 * which needs a listener. This one reads no notification: as soon as Android connects it, it asks to be
 * disconnected, so nothing is delivered to it and nothing keeps Bercail alive in the background.
 */
class MediaListener : NotificationListenerService() {
    override fun onListenerConnected() = requestUnbind()
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
