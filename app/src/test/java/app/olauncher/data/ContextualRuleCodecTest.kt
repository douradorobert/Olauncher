package app.olauncher.data

import org.junit.Assert.assertFalse
import org.junit.Test

class ContextualRuleCodecTest {
    @Test
    fun `bluetooth device selection makes rule nonempty`() {
        assertFalse(ContextualRule("app", bluetoothDeviceAddresses = setOf("HEADPHONES")).isEmpty)
    }
}
