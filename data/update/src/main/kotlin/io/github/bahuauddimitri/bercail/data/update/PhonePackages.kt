package io.github.bahuauddimitri.bercail.data.update

import android.content.Context
import io.github.bahuauddimitri.bercail.core.domain.update.InstallAnswer
import io.github.bahuauddimitri.bercail.core.domain.update.Release
import io.github.bahuauddimitri.bercail.core.domain.update.UpdatePackages
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The package of a new version of Bercail. It is fetched into [folder], kept only if it has the size and the
 * fingerprint GitHub announced, then handed to Android, which checks by itself that it is signed as Bercail and
 * is not older than the version installed. Only one package is kept at a time.
 */
class PhonePackages(
    context: Context,
    private val folder: File = File(context.cacheDir, "update"),
    private val background: CoroutineDispatcher = Dispatchers.IO
) : UpdatePackages {
    private val context = context.applicationContext
    private val android = AndroidInstaller(this.context)

    private fun fileOf(release: Release) = File(folder, "bercail-${release.version}.apk")

    override fun has(release: Release): Boolean = fileOf(release).let { it.isFile && it.length() == release.size }

    override suspend fun download(release: Release): Boolean = withContext(background) {
        val partial = File(folder, "partial")
        try {
            clear()
            folder.mkdirs()
            fetchPackage(release, partial) && partial.renameTo(fileOf(release))
        } catch (_: IOException) {
            false
        } finally {
            partial.delete()
        }
    }

    override fun mayInstallUnasked(): Boolean = context.packageManager.canRequestPackageInstalls()

    override suspend fun install(release: Release, askUser: Boolean): InstallAnswer = withContext(background) {
        if (has(release)) android.install(fileOf(release), askUser) else InstallAnswer.Refused
    }

    override fun clear() {
        folder.deleteRecursively()
    }
}
