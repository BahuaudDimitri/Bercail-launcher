package io.github.bahuauddimitri.bercail.architecture

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import org.junit.Test

/**
 * Battery comes first: Bercail never keeps the phone awake, never runs in the foreground, never wakes it up at a
 * set time and schedules no background work: it may only read the next alarm of the phone's clock. What it sends
 * over the network is always encrypted.
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

    @Test
    fun `aucun travail programmé en arrière-plan`() {
        // Even the daily look at the releases happens when the home screen shows, never at a set time.
        assertThat(codeUsing("WorkManager", "JobScheduler")).isEmpty()
    }

    @Test
    fun `le prochain réveil du téléphone se lit, mais aucune alarme ne se programme`() {
        // Only the alarm of the line of the day reaches the alarms of Android, and only to read the next one.
        val alarms = Repository.productionFiles.filter { file ->
            listOf("AlarmManager", "ALARM_SERVICE").any { it in file.text }
        }
        assertThat(alarms.map { it.relativePath })
            .containsExactly("app/src/main/kotlin/io/github/bahuauddimitri/bercail/DeviceAlarm.kt")
        val scheduling = Regex("""\.(set|setRepeating|setInexactRepeating|setWindow|setAndAllowWhileIdle)\(""")
        assertThat(alarms.filter { scheduling.containsMatchIn(it.text) }.map { it.relativePath }).isEmpty()
    }

    @Test
    fun `la version publiée ne parle au réseau qu'en https`() {
        assertThat(codeUsing("\"http://")).isEmpty()
        assertThat(manifestsDeclaring("usesCleartextTraffic", "networkSecurityConfig")).isEmpty()
    }

    private fun codeUsing(vararg calls: String) = Repository.productionFiles
        .filter { file -> calls.any { it in file.text } }
        .map { it.relativePath }

    private fun manifestsDeclaring(vararg permissions: String) = Repository.mainManifests
        .filter { manifest -> permissions.any { it in manifest.readText() } }
        .map { it.relativeTo(Repository.root).invariantSeparatorsPath }
}
