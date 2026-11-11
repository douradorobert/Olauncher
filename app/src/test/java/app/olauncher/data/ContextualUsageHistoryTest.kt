package app.olauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

class ContextualUsageHistoryTest {
    private val saoPaulo = TimeZone.getTimeZone("America/Sao_Paulo")

    @Test
    fun `hours are divided into six four-hour buckets`() {
        assertEquals(0, ContextualUsageHistory.timeBucket(0))
        assertEquals(0, ContextualUsageHistory.timeBucket(3))
        assertEquals(1, ContextualUsageHistory.timeBucket(4))
        assertEquals(5, ContextualUsageHistory.timeBucket(23))
    }

    @Test
    fun `local epoch day stays stable within a civil day across historical offset change`() {
        val morning = localMillis(2018, Calendar.NOVEMBER, 4, 1, 30)
        val evening = localMillis(2018, Calendar.NOVEMBER, 4, 22, 30)

        assertEquals(
            ContextualUsageHistory.localEpochDay(morning, saoPaulo),
            ContextualUsageHistory.localEpochDay(evening, saoPaulo),
        )
    }

    @Test
    fun `record increments only the matching app bucket and day`() {
        val existing = listOf(
            ContextualUsageEntry("spotify", 3, 100, 1),
            ContextualUsageEntry("teams", 3, 100, 4),
            ContextualUsageEntry("spotify", 2, 100, 5),
            ContextualUsageEntry("spotify", 3, 99, 6),
        )

        assertEquals(
            listOf(
                ContextualUsageEntry("spotify", 3, 100, 2),
                ContextualUsageEntry("teams", 3, 100, 4),
                ContextualUsageEntry("spotify", 2, 100, 5),
                ContextualUsageEntry("spotify", 3, 99, 6),
            ),
            ContextualUsageHistory.record(existing, "spotify", 12, 100),
        )
    }

    @Test
    fun `recent launches score ten points each`() {
        assertEquals(10, ContextualUsageHistory.score(listOf(entry(day = 100, count = 1)), "spotify", 12, 100))
        assertEquals(20, ContextualUsageHistory.score(listOf(entry(day = 100, count = 2)), "spotify", 12, 100))
    }

    @Test
    fun `score halves every seven days and future dates have zero age`() {
        assertEquals(10, ContextualUsageHistory.score(listOf(entry(day = 93, count = 2)), "spotify", 12, 100))
        assertEquals(20, ContextualUsageHistory.score(listOf(entry(day = 101, count = 2)), "spotify", 12, 100))
    }

    @Test
    fun `score rounds only after summing contributions`() {
        val entries = listOf(entry(day = 86, count = 1), entry(day = 86, count = 1))

        assertEquals(5, ContextualUsageHistory.score(entries, "spotify", 12, 100))
    }

    @Test
    fun `thirty-day entries remain and older entries are pruned`() {
        val thirtyDaysOld = entry(day = 70, count = 8)
        val thirtyOneDaysOld = entry(day = 69, count = 8)

        assertEquals(4, ContextualUsageHistory.score(listOf(thirtyDaysOld), "spotify", 12, 100))
        assertEquals(0, ContextualUsageHistory.score(listOf(thirtyOneDaysOld), "spotify", 12, 100))
        assertEquals(listOf(thirtyDaysOld), ContextualUsageHistory.prune(listOf(thirtyDaysOld, thirtyOneDaysOld), 100))
    }

    @Test
    fun `invalid entries never affect score or persisted history`() {
        val invalid = listOf(
            ContextualUsageEntry("", 3, 100, 2),
            ContextualUsageEntry("spotify", 6, 100, 2),
            ContextualUsageEntry("spotify", 3, 100, 0),
        )

        assertEquals(0, ContextualUsageHistory.score(invalid, "spotify", 12, 100))
        assertEquals(emptyList<ContextualUsageEntry>(), ContextualUsageHistory.prune(invalid, 100))
    }

    @Test
    fun `very large counts saturate score and increment without overflowing`() {
        val maximum = entry(day = 100, count = Int.MAX_VALUE)

        assertEquals(Int.MAX_VALUE, ContextualUsageHistory.score(listOf(maximum), "spotify", 12, 100))
        assertEquals(listOf(maximum), ContextualUsageHistory.record(listOf(maximum), "spotify", 12, 100))
    }

    private fun entry(day: Long, count: Int) = ContextualUsageEntry("spotify", 3, day, count)

    private fun localMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        GregorianCalendar(saoPaulo).apply {
            clear()
            set(year, month, day, hour, minute)
        }.timeInMillis
}
