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
    fun `record selects the half-hour bucket on both sides of the boundary`() {
        val first = ContextualUsageRepository.updateSerializedHistory(
            serialized = ContextualUsageHistoryCodec.encode(emptyList()),
            appIdentity = "spotify|user",
            hour = 10,
            minute = 29,
            nowMillis = nowMillis,
            timeZone = utc,
            recordLaunch = true,
        )
        val second = ContextualUsageRepository.updateSerializedHistory(
            serialized = first.serialized,
            appIdentity = "spotify|user",
            hour = 10,
            minute = 30,
            nowMillis = nowMillis,
            timeZone = utc,
            recordLaunch = true,
        )

        assertEquals(
            listOf(
                ContextualUsageEntry("spotify|user", 20, currentDay, 1.0),
                ContextualUsageEntry("spotify|user", 21, currentDay, 1.0),
            ),
            ContextualUsageHistoryCodec.decode(second.serialized, currentDay).entries,
        )
    }

    @Test
    fun `version two read migrates once without changing its score again`() {
        val first = ContextualUsageRepository.updateSerializedHistory(
            serialized = """{"version":2,"entries":[
                {"app":"spotify|user","bucket":2,"day":$currentDay,"count":8}
            ]}""".trimIndent(),
            appIdentity = "spotify|user",
            hour = 10,
            minute = 0,
            nowMillis = nowMillis,
            timeZone = utc,
        )
        val second = ContextualUsageRepository.updateSerializedHistory(
            serialized = first.serialized,
            appIdentity = "spotify|user",
            hour = 10,
            minute = 0,
            nowMillis = nowMillis,
            timeZone = utc,
        )

        assertTrue(first.changed)
        assertEquals(3, org.json.JSONObject(first.serialized).getInt("version"))
        assertEquals(first.score, second.score)
        assertFalse(second.changed)
    }

    @Test
    fun `new launch has exact adjacent and three-hour scores`() {
        val recorded = ContextualUsageRepository.updateSerializedHistory(
            serialized = ContextualUsageHistoryCodec.encode(emptyList()),
            appIdentity = "spotify|user",
            hour = 10,
            minute = 20,
            nowMillis = nowMillis,
            timeZone = utc,
            recordLaunch = true,
        )

        fun scoreAt(hour: Int, minute: Int) = ContextualUsageRepository.updateSerializedHistory(
            serialized = recorded.serialized,
            appIdentity = "spotify|user",
            hour = hour,
            minute = minute,
            nowMillis = nowMillis,
            timeZone = utc,
        ).score

        assertEquals(10, scoreAt(10, 20))
        assertEquals(8, scoreAt(10, 30))
        assertEquals(0, scoreAt(13, 0))
    }

    @Test
    fun `read prunes expired entries and retains the thirty day boundary`() {
        val serialized = ContextualUsageHistoryCodec.encode(
            listOf(
                ContextualUsageEntry("spotify|user", 20, currentDay - 31, 8.0),
                ContextualUsageEntry("spotify|user", 20, currentDay - 30, 8.0),
            ),
        )

        val update = ContextualUsageRepository.updateSerializedHistory(
            serialized = serialized,
            appIdentity = "spotify|user",
            hour = 10,
            minute = 0,
            nowMillis = nowMillis,
            timeZone = utc,
        )

        assertTrue(update.changed)
        assertEquals(4, update.score)
        assertEquals(
            listOf(ContextualUsageEntry("spotify|user", 20, currentDay - 30, 8.0)),
            ContextualUsageHistoryCodec.decode(update.serialized, currentDay).entries,
        )
    }

    @Test
    fun `corrupt history is recovered to empty version three json`() {
        val update = ContextualUsageRepository.updateSerializedHistory(
            serialized = "broken",
            appIdentity = "spotify|user",
            hour = 10,
            minute = 20,
            nowMillis = nowMillis,
            timeZone = utc,
        )

        assertTrue(update.changed)
        assertEquals(0, update.score)
        assertEquals(3, org.json.JSONObject(update.serialized).getInt("version"))
        assertFalse(ContextualUsageHistoryCodec.decode(update.serialized, currentDay).requiresRewrite)
    }
}
