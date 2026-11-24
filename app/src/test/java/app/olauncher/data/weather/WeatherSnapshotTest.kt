package app.olauncher.data.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherSnapshotTest {
    @Test fun predictionExpiresAfterThirtyMinutes() {
        val snapshot = WeatherSnapshot(23.0, 0, "São Paulo", 10_000L)
        assertTrue(snapshot.isFresh(10_000L))
        assertTrue(snapshot.isFresh(1_809_999L))
        assertFalse(snapshot.isFresh(1_810_000L))
        assertFalse(snapshot.isFresh(9_999L))
    }

    @Test fun mapsRepresentativeWmoCodes() {
        assertEquals("☀", weatherSymbol(0))
        assertEquals("⛅", weatherSymbol(2))
        assertEquals("☁", weatherSymbol(3))
        assertEquals("☔", weatherSymbol(63))
        assertEquals("❄", weatherSymbol(75))
        assertEquals("⚡", weatherSymbol(95))
    }
}
