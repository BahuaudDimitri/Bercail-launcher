package io.github.bahuauddimitri.bercail.data.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.SystemClock
import io.github.bahuauddimitri.bercail.core.domain.weather.Place
import io.github.bahuauddimitri.bercail.core.domain.weather.PlaceSource
import java.time.Duration
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The approximate position of the phone, never the precise one. What Android already knows is used first: it
 * costs nothing. Only when that is more than six hours old does Bercail ask the phone once, through the network
 * and never through the GPS, and it does not wait longer than [patience].
 */
class PhonePosition(context: Context, private val patience: Duration = Duration.ofSeconds(PATIENCE_SECONDS)) :
    PlaceSource {
    private val context = context.applicationContext

    override suspend fun current(): Place? {
        val allowed = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val android = context.getSystemService(LocationManager::class.java)
        if (!allowed || android == null) return null
        return try {
            val known = lastKnown(android)
            val recent = known?.takeIf { it.age() < FRESH_ENOUGH }
            (recent ?: asked(android) ?: known)?.let { Place(it.latitude, it.longitude) }
        } catch (_: SecurityException) {
            // The user took the access back in the meantime.
            null
        }
    }

    /** The most recent position among those the phone's providers remember. */
    @SuppressLint("MissingPermission") // Checked in current(), the only caller.
    private fun lastKnown(android: LocationManager): Location? = android.getProviders(true)
        .mapNotNull(android::getLastKnownLocation)
        .maxByOrNull { it.elapsedRealtimeNanos }

    /** One question to the phone, answered once. Nothing when no cheap provider is on, or when it takes too long. */
    @SuppressLint("MissingPermission") // Checked in current(), the only caller.
    private suspend fun asked(android: LocationManager): Location? {
        val provider = CHEAP_PROVIDERS.firstOrNull(android::isProviderEnabled) ?: return null
        return withTimeoutOrNull(patience.toMillis()) {
            suspendCancellableCoroutine { waiting ->
                val giveUp = CancellationSignal()
                waiting.invokeOnCancellation { giveUp.cancel() }
                android.getCurrentLocation(provider, giveUp, context.mainExecutor) { waiting.resume(it) }
            }
        }
    }

    private fun Location.age(): Duration = Duration.ofNanos(SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos)

    companion object {
        /** How long Bercail waits for the phone to say where it is. */
        const val PATIENCE_SECONDS = 10L

        private val FRESH_ENOUGH: Duration = Duration.ofHours(FRESH_HOURS)
        private const val FRESH_HOURS = 6L

        /** The providers that work from the network around, without turning the GPS on. */
        private val CHEAP_PROVIDERS = listOf(LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER)
    }
}
