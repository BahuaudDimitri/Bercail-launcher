package io.github.bahuauddimitri.bercail.data.weather

import android.Manifest
import android.app.Application
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.github.bahuauddimitri.bercail.core.domain.weather.Place
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSystemClock

/** Where the phone is, roughly: what Android already knows first, one question only when that is too old. */
@RunWith(AndroidJUnit4::class)
class PhonePositionUiTest {
    private val phone = ApplicationProvider.getApplicationContext<Application>()
    private val android = phone.getSystemService(LocationManager::class.java)
    private val position = PhonePosition(phone, patience = Duration.ofMillis(200))

    @Before
    fun setUp() {
        // The phone has been on for a day: a position can be older than a few hours.
        ShadowSystemClock.advanceBy(Duration.ofDays(1))
        shadowOf(phone).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        shadowOf(android).setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        shadowOf(android).setProviderEnabled(LocationManager.PASSIVE_PROVIDER, true)
    }

    private fun known(provider: String, latitude: Double, longitude: Double, age: Duration = Duration.ofMinutes(5)) =
        shadowOf(android).setLastKnownLocation(provider, location(provider, latitude, longitude, age))

    private fun location(provider: String, latitude: Double, longitude: Double, age: Duration = Duration.ZERO) =
        Location(provider).apply {
            this.latitude = latitude
            this.longitude = longitude
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos() - age.toNanos()
            time = System.currentTimeMillis() - age.toMillis()
        }

    private fun current(): Place? = runBlocking { position.current() }

    @Test
    fun `la dernière position connue d'Android suffit, sans rien demander au téléphone`() {
        known(LocationManager.NETWORK_PROVIDER, 47.22, -1.55)

        assertThat(current()).isEqualTo(Place(47.22, -1.55))
    }

    @Test
    fun `entre plusieurs positions connues, la plus récente compte`() {
        known(LocationManager.NETWORK_PROVIDER, 48.85, 2.35, age = Duration.ofHours(3))
        known(LocationManager.PASSIVE_PROVIDER, 47.22, -1.55, age = Duration.ofMinutes(10))

        assertThat(current()).isEqualTo(Place(47.22, -1.55))
    }

    @Test
    fun `sans autorisation, la position n'est pas lue`() {
        known(LocationManager.NETWORK_PROVIDER, 47.22, -1.55)
        shadowOf(phone).denyPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)

        assertThat(current()).isNull()
    }

    @Test
    fun `une position de plus de six heures est redemandée au téléphone, une seule fois`() {
        known(LocationManager.NETWORK_PROVIDER, 48.85, 2.35, age = Duration.ofHours(7))
        val asked = CoroutineScope(Dispatchers.Unconfined).async { position.current() }

        shadowOf(android).simulateLocation(location(LocationManager.NETWORK_PROVIDER, 47.22, -1.55))
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(runBlocking { asked.await() }).isEqualTo(Place(47.22, -1.55))
    }

    @Test
    fun `si le téléphone ne répond pas, la vieille position sert quand même`() {
        known(LocationManager.NETWORK_PROVIDER, 48.85, 2.35, age = Duration.ofHours(7))

        assertThat(current()).isEqualTo(Place(48.85, 2.35))
    }

    @Test
    fun `sans aucune position connue et sans réponse, le téléphone ne sait pas où il est`() {
        assertThat(current()).isNull()
    }

    @Test
    fun `localisation coupée dans les Paramètres, il n'y a pas de position`() {
        known(LocationManager.NETWORK_PROVIDER, 47.22, -1.55)
        shadowOf(android).setProviderEnabled(LocationManager.NETWORK_PROVIDER, false)
        shadowOf(android).setProviderEnabled(LocationManager.PASSIVE_PROVIDER, false)
        shadowOf(android).setProviderEnabled(LocationManager.GPS_PROVIDER, false)
        shadowOf(android).setProviderEnabled(LocationManager.FUSED_PROVIDER, false)

        assertThat(current()).isNull()
    }
}
