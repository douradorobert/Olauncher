package app.olauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class ContextualUsageRepositoryTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val currentDay = 20_000L
    private val nowMillis = TimeUnit.DAYS.toMillis(currentDay)

    @Test
    fun `legacy read migrates once without duplicating its score`() {
        val first = ContextualUsageRepository.updateSerializedHistory(
            serialized = """{"spotify|user#3":2}""",
            appIdentity = "spotify|user",
            hour = 12,
            nowMillis = nowMillis,
            timeZone = utc,
        )
        val second = ContextualUsageRepository.updateSerializedHistory(
            serialized = first.serialized,
            appIdentity = "spotify|user",
            hour = 12,
            nowMillis = nowMillis,
            timeZone = utc,
        )

        assertEquals(20, first.score)
        assertTrue(first.changed)
        assertEquals(20, second.score)
        assertFalse(second.changed)
    }

    @Test
    fun `read prunes expired entries and retains the thirty day boundary`() {
        val serialized = ContextualUsageHistoryCodec.encode(
            listOf(
                ContextualUsageEntry("spotify|user", 3, currentDay - 31, 8.0),
                ContextualUsageEntry("spotify|user", 3, currentDay - 30, 8.0),
            ),
        )

        val update = ContextualUsageRepository.updateSerializedHistory(
            serialized, "spotify|user", 12, nowMillis, utc,
        )

        assertTrue(update.changed)
        assertEquals(4, update.score)
        assertEquals(
            listOf(ContextualUsageEntry("spotify|user", 3, currentDay - 30, 8.0)),
            ContextualUsageHistoryCodec.decode(update.serialized, currentDay).entries,
        )
    }

    @Test
    fun `record increments only the current app bucket and day`() {
        val serialized = ContextualUsageHistoryCodec.encode(
            listOf(ContextualUsageEntry("teams|user", 3, currentDay, 5.0)),
        )

        val update = ContextualUsageRepository.updateSerializedHistory(
            serialized, "spotify|user", 12, nowMillis, utc, recordLaunch = true,
        )

        assertTrue(update.changed)
        assertEquals(10, update.score)
        assertEquals(
            listOf(
                ContextualUsageEntry("spotify|user", 24, currentDay, 1.0),
                ContextualUsageEntry("teams|user", 3, currentDay, 5.0),
            ),
            ContextualUsageHistoryCodec.decode(update.serialized, currentDay).entries,
        )
    }

    @Test
    fun `corrupt history is recovered to empty version two json`() {
        val update = ContextualUsageRepository.updateSerializedHistory(
            "broken", "spotify|user", 12, nowMillis, utc,
        )

        assertTrue(update.changed)
        assertEquals(0, update.score)
        assertFalse(ContextualUsageHistoryCodec.decode(update.serialized, currentDay).requiresRewrite)
    }
}
