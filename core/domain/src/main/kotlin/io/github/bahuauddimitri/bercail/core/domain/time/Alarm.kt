package io.github.bahuauddimitri.bercail.core.domain.time

import io.github.bahuauddimitri.bercail.core.domain.weather.Forecast
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.Flow

/** The alarm set in the phone's clock app. */
interface AlarmSource {
    /** When the next alarm rings, or nothing; again each time it changes. Nothing is read while nobody listens. */
    val next: Flow<LocalDateTime?>

    /** Shows the alarms in the phone's clock app. */
    fun openAlarms()
}

/**
 * The alarm at the end of the line of the day: "07:15" over "réveil · 9°", and what a screen reader says of it,
 * "Réveil demain à 07:15". Without one ([set] false), see [None].
 */
data class WakeUp(val time: String, val caption: String, val spoken: String, val set: Boolean = true) {
    companion object {
        /** No alarm within 24 hours: the line still ends on a grey ring, "Aucun réveil", which opens the alarms. */
        val None = WakeUp(time = "", caption = "Aucun réveil", spoken = "Aucun réveil", set = false)
    }
}

/**
 * The alarm the line of the day ends on: the next one, if it rings within 24 hours, with the temperature forecast
 * for its hour when there is one. Otherwise [WakeUp.None]: the line ends on "Aucun réveil", to reach the alarms all
 * the same.
 */
fun wakeUpAt(now: LocalDateTime, alarm: LocalDateTime?, forecast: Forecast?): WakeUp {
    if (alarm == null || alarm < now || Duration.between(now, alarm) > AHEAD) return WakeUp.None
    val time = alarm.toLocalTime().asClockTime()
    val hour = alarm.truncatedTo(ChronoUnit.HOURS)
    val degrees = forecast?.hours?.firstOrNull { it.time == hour }?.temperature
    val day = if (alarm.toLocalDate() > now.toLocalDate()) " demain" else ""
    return WakeUp(
        time = time,
        caption = if (degrees == null) WAKE_UP else "$WAKE_UP · $degrees°",
        spoken = "Réveil$day à $time"
    )
}

private const val WAKE_UP = "réveil"
private val AHEAD: Duration = Duration.ofHours(24)
