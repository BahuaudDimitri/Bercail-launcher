package io.github.bahuauddimitri.bercail.data.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams
import io.github.bahuauddimitri.bercail.core.domain.update.InstallAnswer
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Hands a package to Android's installer, as a replacement of Bercail and of nothing else, and waits for its
 * answer. Android decides alone whether it needs the user's agreement.
 */
internal class AndroidInstaller(private val context: Context) {
    private val installer get() = context.packageManager.packageInstaller

    suspend fun install(file: File, askUser: Boolean): InstallAnswer = try {
        // A request left behind by an installation that never finished would stay in Android's list.
        installer.mySessions.forEach { withdraw(it.sessionId) }
        val id = installer.createSession(replacingBercail(file.length()))
        installer.openSession(id).use { session ->
            session.write(file)
            answerTo(session, id, askUser)
        }
    } catch (_: IOException) {
        InstallAnswer.Refused
    }

    private fun replacingBercail(size: Long) = SessionParams(SessionParams.MODE_FULL_INSTALL).apply {
        // Android refuses the package if it is anything else than Bercail.
        setAppPackageName(context.packageName)
        setSize(size)
        // An app that replaces itself needs no question each time; Android still decides.
        setRequireUserAction(SessionParams.USER_ACTION_NOT_REQUIRED)
    }

    private fun PackageInstaller.Session.write(file: File) {
        openWrite(file.name, 0, file.length()).use { output ->
            file.inputStream().use { it.copyTo(output) }
            fsync(output)
        }
    }

    private suspend fun answerTo(session: PackageInstaller.Session, id: Int, askUser: Boolean): InstallAnswer {
        val status = withTimeoutOrNull(PATIENCE) {
            session.committed(id).mapNotNull { answer ->
                val status = answer.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
                if (status == PackageInstaller.STATUS_PENDING_USER_ACTION && askUser) {
                    showConfirmation(answer)
                    null
                } else {
                    status
                }
            }.first()
        }
        return when (status) {
            PackageInstaller.STATUS_SUCCESS -> InstallAnswer.Installed

            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                withdraw(id)
                InstallAnswer.NeedsUser
            }

            // Nobody answered Android's screen: the package stays for another time.
            null -> {
                withdraw(id)
                InstallAnswer.Declined
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> InstallAnswer.Declined

            else -> InstallAnswer.Refused
        }
    }

    /** Hands the package to Android and listens to its answers about this request, until nobody waits anymore. */
    private fun PackageInstaller.Session.committed(id: Int): Flow<Intent> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, answer: Intent) {
                if (answer.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, NO_SESSION) == id) trySend(answer)
            }
        }
        context.registerReceiver(receiver, IntentFilter(ANSWER), Context.RECEIVER_NOT_EXPORTED)
        val answerHere = PendingIntent.getBroadcast(
            context,
            id,
            Intent(ANSWER).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        commit(answerHere.intentSender)
        awaitClose { context.unregisterReceiver(receiver) }
    }

    /** Android's own screen, where the user agrees to the installation. */
    private fun showConfirmation(answer: Intent) {
        val screen = answer.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java) ?: return
        context.startActivity(screen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun withdraw(sessionId: Int) {
        try {
            installer.abandonSession(sessionId)
        } catch (_: SecurityException) {
            // Already gone.
        }
    }

    companion object {
        /** Android's answers come back to Bercail alone, under this name. */
        const val ANSWER = "io.github.bahuauddimitri.bercail.data.update.INSTALL_ANSWER"

        /** How long Android's confirmation screen may stay unanswered. */
        private val PATIENCE = 5.minutes
        private const val NO_SESSION = -1
    }
}
