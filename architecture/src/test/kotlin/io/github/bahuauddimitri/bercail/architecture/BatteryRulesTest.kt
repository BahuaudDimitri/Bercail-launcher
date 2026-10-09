package io.github.bahuauddimitri.bercail.architecture

import assertk.assertThat
import assertk.assertions.isEmpty
import org.junit.Test

/**
 * Battery comes first: Bercail never keeps the phone awake, never runs in the foreground,
 * and never wakes it up at a set time.
 */
class BatteryRulesTest {
    @Test
    fun `aucun verrou de réveil`() {
        assertThat(codeUsing("newWakeLock", "WakeLock")).isEmpty()
        assertThat(manifestsDeclaring("android.permission.WAKE_LOCK")).isEmpty()
    }

    @Test
    fun `aucun service au premier plan`() {
        assertThat(codeUsing("startForeground(", "startForegroundService(")).isEmpty()
        assertThat(manifestsDeclaring("android.permission.FOREGROUND_SERVICE")).isEmpty()
    }

    @Test
    fun `aucune alarme exacte`() {
        assertThat(codeUsing("setExact(", "setExactAndAllowWhileIdle(", "setAlarmClock(")).isEmpty()
        assertThat(manifestsDeclaring("SCHEDULE_EXACT_ALARM", "USE_EXACT_ALARM")).isEmpty()
    }

    private fun codeUsing(vararg calls: String) = Repository.productionFiles
        .filter { file -> calls.any { it in file.text } }
        .map { it.relativePath }

    private fun manifestsDeclaring(vararg permissions: String) = Repository.mainManifests
        .filter { manifest -> permissions.any { it in manifest.readText() } }
        .map { it.relativeTo(Repository.root).invariantSeparatorsPath }
}
