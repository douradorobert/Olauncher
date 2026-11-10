package app.olauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsBackupCodecTest {
    @Test
    fun `backup payload contains preferences and excludes device identifiers`() {
        val values = SettingsBackupCodec.valuesForBackup(
            mapOf(
                "CONTEXTUAL_SUGGESTIONS" to true,
                "CONTEXTUAL_APPS" to setOf("lamp|user"),
                "APP_SPACING_SCALE" to 0.8f,
                "HOME_WIFI_NAME" to "Robert",
            ),
        )

        assertTrue(values.isNotEmpty())
        assertEquals(true, values["CONTEXTUAL_SUGGESTIONS"])
        assertEquals(setOf("lamp|user"), values["CONTEXTUAL_APPS"])
        assertFalse("HOME_WIFI_NAME" in values)
    }
}
