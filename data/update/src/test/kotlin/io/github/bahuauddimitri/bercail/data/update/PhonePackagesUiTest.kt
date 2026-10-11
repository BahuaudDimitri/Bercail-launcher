package io.github.bahuauddimitri.bercail.data.update

import android.app.Application
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.domain.update.InstallAnswer
import io.github.bahuauddimitri.bercail.core.domain.update.Release
import io.github.bahuauddimitri.bercail.core.domain.update.Version
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * The package of a new version: fetched from a server that plays GitHub, checked, kept on the simulated phone,
 * then handed to Android, whose answers are played by hand.
 */
@RunWith(AndroidJUnit4::class)
class PhonePackagesUiTest {
    private val phone = ApplicationProvider.getApplicationContext<Application>()
    private val gitHub = MockWebServer()
    private val folder = File(phone.cacheDir, "update")
    private val packages = PhonePackages(phone, folder, background = Dispatchers.Unconfined)
    private val installer get() = phone.packageManager.packageInstaller

    /** Stands for the package of the release: its content does not matter until Android reads it. */
    private val apk = ByteArray(300_000) { (it % 251).toByte() }

    @Before
    fun start() = gitHub.start()

    @After
    fun stop() = gitHub.close()

    private fun sha256(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun announced(
        version: Version = Version(0, 9, 0),
        size: Long = apk.size.toLong(),
        sha256: String? = sha256(apk)
    ) = Release(version, gitHub.url("/download/v$version/bercail-$version.apk").toString(), size, sha256)

    private fun gitHubServes(bytes: ByteArray = apk, code: Int = 200) =
        gitHub.enqueue(MockResponse.Builder().code(code).body(Buffer().write(bytes)).build())

    private fun download(release: Release): Boolean = runBlocking { packages.download(release) }

    private fun keptFiles() = folder.listFiles().orEmpty().map { it.name }

    @Test
    fun `le paquet annoncé est téléchargé et gardé sur le téléphone`() {
        gitHubServes()

        assertThat(download(announced())).isTrue()

        assertThat(packages.has(announced())).isTrue()
        assertThat(File(folder, "bercail-0.9.0.apk").readBytes().contentEquals(apk)).isTrue()
    }

    @Test
    fun `tant que rien n'est téléchargé, le paquet n'est pas sur le téléphone`() {
        assertThat(packages.has(announced())).isFalse()
    }

    @Test
    fun `un paquet dont l'empreinte n'est pas celle annoncée est jeté`() {
        gitHubServes(apk.copyOf().also { it[1000] = 42 })

        assertThat(download(announced())).isFalse()

        assertThat(packages.has(announced())).isFalse()
        assertThat(keptFiles()).isEmpty()
    }

    @Test
    fun `un paquet plus court qu'annoncé est jeté`() {
        gitHubServes(apk.copyOf(200_000))

        assertThat(download(announced(sha256 = null))).isFalse()
        assertThat(keptFiles()).isEmpty()
    }

    @Test
    fun `un paquet plus long qu'annoncé est jeté`() {
        gitHubServes(apk + apk)

        assertThat(download(announced(sha256 = null))).isFalse()
        assertThat(keptFiles()).isEmpty()
    }

    @Test
    fun `sans empreinte annoncée, la bonne taille suffit`() {
        gitHubServes()

        assertThat(download(announced(sha256 = null))).isTrue()
    }

    @Test
    fun `une erreur du serveur ne laisse rien sur le téléphone`() {
        gitHubServes("Not Found".toByteArray(), code = 404)

        assertThat(download(announced())).isFalse()
        assertThat(keptFiles()).isEmpty()
    }

    @Test
    fun `sans réseau, le téléchargement échoue sans erreur`() {
        val release = announced()
        gitHub.close()

        assertThat(download(release)).isFalse()
    }

    @Test
    fun `le paquet d'une nouvelle version remplace celui de l'ancienne`() {
        gitHubServes()
        gitHubServes()
        download(announced(Version(0, 9, 0)))

        download(announced(Version(0, 9, 1)))

        assertThat(keptFiles()).isEqualTo(listOf("bercail-0.9.1.apk"))
    }

    @Test
    fun `un téléchargement interrompu ne laisse rien sur le téléphone`() = runBlocking {
        gitHub.enqueue(
            MockResponse.Builder().body(Buffer().write(apk)).throttleBody(4_096, 50, TimeUnit.MILLISECONDS).build()
        )
        val slow = PhonePackages(phone, folder, background = Dispatchers.IO)
        val download = launch(Dispatchers.IO) { slow.download(announced()) }
        gitHub.takeRequest()

        download.cancelAndJoin()

        assertThat(keptFiles()).isEmpty()
        assertThat(slow.has(announced())).isFalse()
    }

    @Test
    fun `tant que l'autorisation d'installer n'est pas donnée, Bercail sait qu'Android demandera un accord`() {
        // The state of a real phone after a first installation; the simulated one grants it by default.
        shadowOf(phone.packageManager).setCanRequestPackageInstalls(false)

        assertThat(packages.mayInstallUnasked()).isFalse()
    }

    @Test
    fun `une fois l'autorisation donnée dans les paramètres du téléphone, Bercail peut installer sans demander`() {
        shadowOf(phone.packageManager).setCanRequestPackageInstalls(true)

        assertThat(packages.mayInstallUnasked()).isTrue()
    }

    @Test
    fun `supprimer les paquets vide le téléphone`() {
        gitHubServes()
        download(announced())

        packages.clear()

        assertThat(packages.has(announced())).isFalse()
        assertThat(keptFiles()).isEmpty()
    }

    /** Starts an installation and stops where Bercail waits for Android's answer. */
    private fun installing(askUser: Boolean): Deferred<InstallAnswer> {
        gitHubServes()
        download(announced())
        return CoroutineScope(Dispatchers.Unconfined).async { packages.install(announced(), askUser) }
    }

    private fun androidAnswers(status: Int, sessionId: Int = installer.allSessions.single().sessionId) {
        val answer = Intent(AndroidInstaller.ANSWER)
            .setPackage(phone.packageName)
            .putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionId)
            .putExtra(PackageInstaller.EXTRA_STATUS, status)
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            answer.putExtra(Intent.EXTRA_INTENT, Intent(CONFIRMATION_SCREEN))
        }
        phone.sendBroadcast(answer)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Deferred<InstallAnswer>.answer(): InstallAnswer? = if (isCompleted) getCompleted() else null

    @Test
    fun `le paquet est remis à Android pour remplacer Bercail, et aucune autre app`() {
        val waiting = installing(askUser = false)

        assertThat(installer.allSessions.single().appPackageName).isEqualTo(phone.packageName)
        assertThat(waiting.answer()).isNull()
    }

    @Test
    fun `quand Android a installé la version, la réponse est « installé »`() {
        val waiting = installing(askUser = false)

        androidAnswers(PackageInstaller.STATUS_SUCCESS)

        assertThat(waiting.answer()).isEqualTo(InstallAnswer.Installed)
    }

    @Test
    fun `si Android veut un accord et que personne ne regarde, Bercail retire sa demande sans rien afficher`() {
        val waiting = installing(askUser = false)

        androidAnswers(PackageInstaller.STATUS_PENDING_USER_ACTION)

        assertThat(waiting.answer()).isEqualTo(InstallAnswer.NeedsUser)
        assertThat(installer.allSessions).isEmpty()
        assertThat(shadowOf(phone).nextStartedActivity).isNull()
    }

    @Test
    fun `quand on a touché la ligne, l'écran d'accord d'Android s'ouvre et Bercail attend la réponse`() {
        val waiting = installing(askUser = true)

        androidAnswers(PackageInstaller.STATUS_PENDING_USER_ACTION)

        assertThat(shadowOf(phone).nextStartedActivity.action).isEqualTo(CONFIRMATION_SCREEN)
        assertThat(waiting.answer()).isNull()

        androidAnswers(PackageInstaller.STATUS_SUCCESS)
        assertThat(waiting.answer()).isEqualTo(InstallAnswer.Installed)
    }

    @Test
    fun `si l'accord est refusé sur l'écran d'Android, la réponse est « refusé par l'utilisateur »`() {
        val waiting = installing(askUser = true)
        androidAnswers(PackageInstaller.STATUS_PENDING_USER_ACTION)

        androidAnswers(PackageInstaller.STATUS_FAILURE_ABORTED)

        assertThat(waiting.answer()).isEqualTo(InstallAnswer.Declined)
    }

    @Test
    fun `un paquet qu'Android rejette est signalé comme refusé`() {
        val waiting = installing(askUser = false)

        androidAnswers(PackageInstaller.STATUS_FAILURE_INVALID)

        assertThat(waiting.answer()).isEqualTo(InstallAnswer.Refused)
    }

    @Test
    fun `la réponse d'une autre installation n'est pas prise pour la nôtre`() {
        val waiting = installing(askUser = false)

        androidAnswers(PackageInstaller.STATUS_SUCCESS, sessionId = installer.allSessions.single().sessionId + 7)

        assertThat(waiting.answer()).isNull()
    }

    @Test
    fun `sans paquet sur le téléphone, rien n'est remis à Android`() {
        val answer = runBlocking { packages.install(announced(), askUser = false) }

        assertThat(answer).isEqualTo(InstallAnswer.Refused)
        assertThat(installer.allSessions).isEmpty()
    }

    @Test
    fun `une demande restée en plan est retirée avant d'en faire une nouvelle`() {
        installing(askUser = false)

        installing(askUser = false)

        assertThat(installer.allSessions.size).isEqualTo(1)
    }

    private companion object {
        const val CONFIRMATION_SCREEN = "android.content.pm.action.CONFIRM_INSTALL"
    }
}
