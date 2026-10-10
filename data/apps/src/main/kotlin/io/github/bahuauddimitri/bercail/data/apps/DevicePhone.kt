package io.github.bahuauddimitri.bercail.data.apps

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import io.github.bahuauddimitri.bercail.core.domain.apps.Phone

/** The places outside Bercail the search and the settings send to: web search, Play Store, phone settings. */
class DevicePhone(context: Context) : Phone {
    private val context = context.applicationContext

    override fun searchWeb(query: String) = open(Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query))

    override fun searchStore(query: String) =
        open(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${Uri.encode(query)}")))

    override fun openSettings() = open(Intent(Settings.ACTION_SETTINGS))

    override fun openHomeScreenSettings() = open(Intent(Settings.ACTION_HOME_SETTINGS))

    private fun open(intent: Intent) = context.startSafely(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
