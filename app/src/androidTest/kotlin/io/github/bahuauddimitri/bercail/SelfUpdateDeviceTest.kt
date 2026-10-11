package io.github.bahuauddimitri.bercail

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import io.github.bahuauddimitri.bercail.core.domain.update.UpdatePackages
import io.github.bahuauddimitri.bercail.core.domain.update.UpdateStatus
import io.github.bahuauddimitri.bercail.core.domain.update.UpdateTrouble
import io.github.bahuauddimitri.bercail.core.domain.update.Updater
import io.github.bahuauddimitri.bercail.core.domain.update.Version
import io.github.bahuauddimitri.bercail.core.testing.FakeClock
import io.github.bahuauddimitri.bercail.core.testing.FakeUpdateJournal
import io.github.bahuauddimitri.bercail.data.update.GitHubReleases
import io.github.bahuauddimitri.bercail.data.update.PhonePackages
import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The whole update path on a real Android, in CI on an emulator or on a phone plugged in over USB: the release is
 * read over the network, its package fetched and checked, then handed to the real Android installer. The package
 * served is not Bercail (replacing the app in the middle of its own tests would stop them), so the path ends
 * where Android turns it down, which is one of the protections to check.
 */
@RunWith(AndroidJUnit4::class)
class SelfUpdateDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val phone = instrumentation.targetContext
    private val gitHub = LocalGitHub()

    /** A real Android package that is not Bercail: the one of these tests. */
    private val notBercail = File(instrumentation.context.packageCodePath).readBytes()
    private val packages = PhonePackages(phone, File(phone.cacheDir, "update-under-test"))

    /** As on a phone where the user already let Bercail install its updates: Android is asked, never the user. */
    private val allowed = object : UpdatePackages by packages {
        override fun mayInstallUnasked() = true
    }
    private val journal = FakeUpdateJournal()
    private val updater = Updater(
        current = Version(0, 0, 1),
        feed = GitHubReleases("${gitHub.address}/releases/latest", "${gitHub.address}/download/"),
        packages = allowed,
        journal = journal,
        clock = FakeClock(LocalDateTime.of(2026, 10, 12, 8, 0))
    )

    @After
    fun stop() {
        packages.clear()
        gitHub.close()
    }

    private fun publish(version: String, apk: ByteArray, announcedSha256: String = sha256(apk)) {
        gitHub.serve("/download/v$version/bercail-$version.apk", apk)
        gitHub.serve(
            "/releases/latest",
            """
            {
              "tag_name": "v$version",
              "draft": false,
              "prerelease": false,
              "assets": [
                {
                  "name": "bercail-$version.apk",
                  "size": ${apk.size},
                  "digest": "sha256:$announcedSha256",
                  "browser_download_url": "${gitHub.address}/download/v$version/bercail-$version.apk"
                }
              ]
            }
            """.trimIndent().toByteArray()
        )
    }

    private fun sha256(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    @Test
    fun `une version publiée est lue puis téléchargée et attend que l’accueil soit caché`() = runBlocking {
        publish("9.9.9", notBercail)

        updater.onHomeShown()

        assertThat(updater.status.value).isEqualTo(UpdateStatus.Ready(Version(9, 9, 9)))
        assertThat(journal.notes.found?.size).isEqualTo(notBercail.size.toLong())
    }

    @Test
    fun `Android refuse un paquet qui n’est pas Bercail et Bercail l’oublie`() = runBlocking {
        publish("9.9.9", notBercail)
        updater.onHomeShown()

        updater.onHomeHidden()

        assertThat(updater.status.value).isEqualTo(UpdateStatus.UpToDate(UpdateTrouble.Refused))
        assertThat(journal.notes.found).isNull()
        assertThat(phone.packageManager.packageInstaller.mySessions).isEmpty()
    }

    @Test
    fun `un paquet dont l’empreinte n’est pas celle annoncée n’est jamais remis à Android`() = runBlocking {
        publish("9.9.9", notBercail, announcedSha256 = "0".repeat(SHA_256_LENGTH))

        updater.onHomeShown()
        updater.onHomeHidden()

        assertThat(updater.status.value).isEqualTo(UpdateStatus.Waiting(Version(9, 9, 9)))
        assertThat(packages.has(checkNotNull(journal.notes.found))).isFalse()
    }

    private companion object {
        const val SHA_256_LENGTH = 64
    }
}

/** A few lines of HTTP on the phone itself, standing for GitHub: each path answers with the bytes it was given. */
private class LocalGitHub : Closeable {
    private val server = ServerSocket(0, BACKLOG, InetAddress.getByName(LOOPBACK))
    private val pages = ConcurrentHashMap<String, ByteArray>()

    val address = "http://$LOOPBACK:${server.localPort}"

    init {
        thread(isDaemon = true) {
            while (!server.isClosed) {
                try {
                    server.accept().use(::answer)
                } catch (_: IOException) {
                    // The test is over, or the caller hung up.
                }
            }
        }
    }

    fun serve(path: String, bytes: ByteArray) {
        pages[path] = bytes
    }

    private fun answer(caller: Socket) {
        val request = caller.getInputStream().bufferedReader()
        val path = request.readLine().orEmpty().split(" ").getOrNull(1)
        request.skipHeaders()
        val page = pages[path]
        val status = if (page == null) "404 Not Found" else "200 OK"
        caller.getOutputStream().run {
            write("HTTP/1.1 $status\r\nContent-Length: ${page?.size ?: 0}\r\nConnection: close\r\n\r\n".toByteArray())
            page?.let(::write)
            flush()
        }
    }

    private fun BufferedReader.skipHeaders() {
        while (!readLine().isNullOrEmpty()) Unit
    }

    override fun close() = server.close()

    private companion object {
        const val LOOPBACK = "127.0.0.1"
        const val BACKLOG = 8
    }
}
