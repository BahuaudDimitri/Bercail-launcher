package io.github.bahuauddimitri.bercail

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.access.AccessSource
import io.github.bahuauddimitri.bercail.core.domain.apps.Phone
import io.github.bahuauddimitri.bercail.data.media.MediaAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The Android permission behind each thing Bercail asks to read. The "notification access" has none: Android
 * gives it on a page of its own settings.
 */
internal val Access.permission: String?
    get() = when (this) {
        Access.Calendar -> Manifest.permission.READ_CALENDAR
        Access.Location -> Manifest.permission.ACCESS_COARSE_LOCATION
        Access.Notifications -> null
    }

/**
 * What the user has let Bercail read. Android does not announce a change of mind: Bercail looks again each time
 * the home screen comes back, and after each question it asks.
 */
class DeviceAccess(private val context: Context) : AccessSource {
    private val known = MutableStateFlow(read())

    override val granted: StateFlow<Set<Access>> = known

    fun refresh() {
        known.value = read()
    }

    private fun read(): Set<Access> = Access.entries.filter(::has).toSet()

    private fun has(access: Access): Boolean {
        val permission = access.permission ?: return MediaAccess.granted(context)
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Asks the user, on Android's own screen, to let Bercail read something. After two refusals Android answers "no"
 * by itself, at once and without showing anything: the page of Bercail in the phone settings is then the only
 * place left to agree, so Bercail opens it. [android] is where the question goes; the tests answer it themselves.
 */
internal class AccessQuestions(
    private val activity: ComponentActivity,
    private val access: () -> DeviceAccess,
    private val phone: () -> Phone,
    android: ActivityResultRegistry = activity.activityResultRegistry
) {
    private var askedAt = 0L

    private val question =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission(), android) { agreed ->
            access().refresh()
            val unasked = SystemClock.elapsedRealtime() - askedAt < SHORTEST_READING_MS
            if (!agreed && unasked) phone().openAppSettings()
        }

    fun ask(what: Access) {
        val permission = what.permission ?: return openNotificationAccess()
        askedAt = SystemClock.elapsedRealtime()
        question.launch(permission)
    }

    /**
     * The "notification access" is given on a page of Android's settings: Bercail opens the one that is about
     * itself. The answer is read when the home screen comes back.
     */
    private fun openNotificationAccess() {
        try {
            activity.startActivity(MediaAccess.question(activity))
        } catch (_: ActivityNotFoundException) {
            // This phone has no such page: the page of Bercail is the closest place.
            phone().openAppSettings()
        }
    }

    private companion object {
        /** Nobody reads and answers Android's question faster than this. */
        const val SHORTEST_READING_MS = 400L
    }
}
