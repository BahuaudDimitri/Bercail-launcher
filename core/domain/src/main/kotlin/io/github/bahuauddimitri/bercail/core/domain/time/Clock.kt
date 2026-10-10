package io.github.bahuauddimitri.bercail.core.domain.time

import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

/** The time of the phone. Always injected: no rule reads the real time by itself. */
interface Clock {
    fun now(): LocalDateTime

    /** The current time, then a new value at each minute change. Nothing runs while nobody listens. */
    val minutes: Flow<LocalDateTime>
}

/** The moments of the day: they choose the weather sentence and the colors of Brume. */
enum class DayPart(val isDark: Boolean = false) {
    Morning,
    Afternoon,
    Evening,
    Night(isDark = true);

    companion object {
        fun at(time: LocalTime): DayPart = when (time.hour) {
            in MORNING_START until NOON -> Morning
            in NOON until EVENING_START -> Afternoon
            in EVENING_START until NIGHT_START -> Evening
            else -> Night
        }

        private const val MORNING_START = 5
        private const val NOON = 12
        private const val EVENING_START = 18
        private const val NIGHT_START = 20
    }
}

/** A time of day as the interface writes it: 09:30. */
fun LocalTime.asClockTime(): String = "%02d:%02d".format(hour, minute)
