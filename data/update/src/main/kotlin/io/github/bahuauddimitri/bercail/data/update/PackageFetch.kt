package io.github.bahuauddimitri.bercail.data.update

import io.github.bahuauddimitri.bercail.core.domain.update.Release
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Fetches the package of a release into [into]. True only when the server answered with exactly the size
 * announced and, when a fingerprint was announced, with that fingerprint.
 */
internal suspend fun fetchPackage(release: Release, into: File): Boolean {
    val connection = URL(release.packageUrl).openHttp()
    try {
        val sha256 = if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            connection.inputStream.use { it.copyTo(into, atMost = release.size) }
        } else {
            null
        }
        val asAnnounced = release.sha256 == null || release.sha256 == sha256
        return sha256 != null && into.length() == release.size && asAnnounced
    } finally {
        connection.disconnect()
    }
}

/** Copies the stream and returns its SHA-256 fingerprint, or nothing as soon as it is longer than announced. */
private suspend fun InputStream.copyTo(file: File, atMost: Long): String? {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(BUFFER)
    var received = 0L
    file.outputStream().use { output ->
        while (received <= atMost) {
            // Leaving the home screen stops the download between two pieces.
            coroutineContext.ensureActive()
            val read = read(buffer)
            if (read < 0) return digest.digest().joinToString("") { "%02x".format(it) }
            received += read
            digest.update(buffer, 0, read)
            output.write(buffer, 0, read)
        }
    }
    return null
}

private const val BUFFER = 64 * 1024
