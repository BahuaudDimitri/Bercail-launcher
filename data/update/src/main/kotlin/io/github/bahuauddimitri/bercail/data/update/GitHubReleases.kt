package io.github.bahuauddimitri.bercail.data.update

import io.github.bahuauddimitri.bercail.core.domain.update.Release
import io.github.bahuauddimitri.bercail.core.domain.update.ReleaseFeed
import io.github.bahuauddimitri.bercail.core.domain.update.Version
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.json.JSONException
import org.json.JSONObject

/**
 * The releases of Bercail on GitHub, read through its public API: one small request, no account, no token. A
 * package only counts when it is hosted under [packagesUnder], the releases of Bercail's own repository.
 */
class GitHubReleases(
    private val latestUrl: String = LATEST,
    private val packagesUnder: String = PACKAGES,
    private val background: CoroutineDispatcher = Dispatchers.IO
) : ReleaseFeed {
    override suspend fun latest(): Release? = runInterruptible(background) {
        try {
            read()?.let(::releaseIn)
        } catch (_: IOException) {
            null
        } catch (_: JSONException) {
            null
        }
    }

    private fun read(): String? {
        val connection = URL(latestUrl).openHttp()
        try {
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val answer = connection.inputStream.use { it.readNBytes(LONGEST_ANSWER + 1) }
            return answer.takeIf { it.size <= LONGEST_ANSWER }?.decodeToString()
        } finally {
            connection.disconnect()
        }
    }

    private fun releaseIn(answer: String): Release? {
        val release = JSONObject(answer)
        val version = Version.parse(release.getString("tag_name"))
        val published = !release.optBoolean("draft") && !release.optBoolean("prerelease")
        val files = release.getJSONArray("assets").let { all -> (0 until all.length()).map(all::getJSONObject) }
        return if (version != null && published) files.firstNotNullOfOrNull { packageOf(version, it) } else null
    }

    /** The file as the package of that version, if it is an Android package hosted where Bercail's releases are. */
    private fun packageOf(version: Version, file: JSONObject): Release? {
        val url = file.getString("browser_download_url")
        if (!file.getString("name").endsWith(".apk") || !url.startsWith(packagesUnder)) return null
        val sha256 = file.optString("digest").removePrefix("sha256:").lowercase().takeIf(SHA_256::matches)
        return Release(version, url, file.getLong("size"), sha256)
    }

    companion object {
        const val LATEST = "https://api.github.com/repos/BahuaudDimitri/Bercail-launcher/releases/latest"
        const val PACKAGES = "https://github.com/BahuaudDimitri/Bercail-launcher/releases/download/"

        /** GitHub's answer weighs about 5 KB; anything much longer is not what was asked for. */
        private const val LONGEST_ANSWER = 256 * 1024
        private val SHA_256 = Regex("[0-9a-f]{64}")
    }
}

/** A plain request that gives up after [TIMEOUT_MS] and names Bercail, as GitHub's API requires. */
internal fun URL.openHttp(): HttpURLConnection = (openConnection() as HttpURLConnection).apply {
    connectTimeout = TIMEOUT_MS
    readTimeout = TIMEOUT_MS
    setRequestProperty("User-Agent", "Bercail")
}

private const val TIMEOUT_MS = 15_000
