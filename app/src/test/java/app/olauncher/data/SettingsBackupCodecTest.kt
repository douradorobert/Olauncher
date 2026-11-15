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
                "CONTEXTUAL_MINIMUM_SCORE" to 40,
                "SEPARATE_CONTEXTUAL_DAY_PROFILES" to true,
                "CONTEXTUAL_HISTORY" to "history",
                "APP_SPACING_SCALE" to 0.8f,
                "HOME_WIFI_NAME" to "Robert",
            ),
        )

        assertTrue(values.isNotEmpty())
        assertEquals(true, values["CONTEXTUAL_SUGGESTIONS"])
        assertEquals(setOf("lamp|user"), values["CONTEXTUAL_APPS"])
        assertEquals(40, values["CONTEXTUAL_MINIMUM_SCORE"])
        assertEquals(true, values["SEPARATE_CONTEXTUAL_DAY_PROFILES"])
        assertFalse("CONTEXTUAL_HISTORY" in values)
        assertFalse("HOME_WIFI_NAME" in values)
    }
}
