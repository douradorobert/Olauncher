package app.olauncher.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceContextSignalsTest {
    @Test
    fun `any saved home wifi identity confirms home`() {
        val current = WifiNetwork(bssid = "22:22:22:22:22:22", name = "Casa-5G")

        assertTrue(
            DeviceContextSignals.matchesHomeWifi(
                current = current,
                savedNames = setOf("Casa-2G", "Casa-5G"),
                savedBssids = setOf("11:11:11:11:11:11"),
            ),
        )
        assertTrue(
            DeviceContextSignals.matchesHomeWifi(
                current = current,
                savedNames = emptySet(),
                savedBssids = setOf("22:22:22:22:22:22"),
            ),
        )
        assertFalse(
            DeviceContextSignals.matchesHomeWifi(
                current = current,
                savedNames = setOf("Trabalho"),
                savedBssids = setOf("33:33:33:33:33:33"),
            ),
        )
    }
}
