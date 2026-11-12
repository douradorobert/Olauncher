package app.olauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

class ContextualUsageHistoryTest {
    private val saoPaulo = TimeZone.getTimeZone("America/Sao_Paulo")

    @Test
    fun `hours and minutes are divided into half-hour buckets`() {
        assertEquals(0, ContextualUsageHistory.timeBucket(0, 0))
        assertEquals(0, ContextualUsageHistory.timeBucket(0, 29))
        assertEquals(1, ContextualUsageHistory.timeBucket(0, 30))
        assertEquals(20, ContextualUsageHistory.timeBucket(10, 20))
        assertEquals(47, ContextualUsageHistory.timeBucket(23, 59))
    }

    @Test
    fun `time bucket coerces values to the civil day bounds`() {
        assertEquals(0, ContextualUsageHistory.timeBucket(-1, -1))
        assertEquals(47, ContextualUsageHistory.timeBucket(24, 60))
    }

    @Test
    fun `temporal weights decay over six half-hour distances`() {
        val expected = listOf(1.0, 0.75, 0.5, 0.375, 0.25, 0.125, 0.0)

        expected.forEachIndexed { distance, weight ->
            assertEquals(weight, ContextualUsageHistory.temporalWeight(20, 20 + distance), 0.0)
        }
    }

    @Test
    fun `temporal distance wraps across midnight in both directions`() {
        assertEquals(0.75, ContextualUsageHistory.temporalWeight(47, 0), 0.0)
        assertEquals(0.75, ContextualUsageHistory.temporalWeight(0, 47), 0.0)
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
            ContextualUsageEntry("spotify", 20, 100, 1.0),
            ContextualUsageEntry("teams", 20, 100, 4.0),
            ContextualUsageEntry("spotify", 19, 100, 5.0),
            ContextualUsageEntry("spotify", 20, 99, 6.0),
        )

        assertEquals(
            listOf(
                ContextualUsageEntry("spotify", 20, 100, 2.0),
                ContextualUsageEntry("teams", 20, 100, 4.0),
                ContextualUsageEntry("spotify", 19, 100, 5.0),
                ContextualUsageEntry("spotify", 20, 99, 6.0),
            ),
            ContextualUsageHistory.record(existing, "spotify", 10, 20, 100),
        )
    }

    @Test
    fun `blank identity is never recorded`() {
        val entries = listOf(entry(bucket = 20, day = 100, mass = 1.0))
        assertEquals(entries, ContextualUsageHistory.record(entries, " ", 10, 20, 100))
    }

    @Test
    fun `recent launch score follows the temporal weight window`() {
        val entries = listOf(entry(bucket = 20, day = 100, mass = 1.0))
        val expected = listOf(10, 8, 5, 4, 3, 1, 0)

        expected.forEachIndexed { distance, score ->
            val targetBucket = 20 + distance
            assertEquals(
                score,
                ContextualUsageHistory.score(
                    entries,
                    "spotify",
                    targetBucket / 2,
                    (targetBucket % 2) * 30,
                    100,
                ),
            )
        }
    }

    @Test
    fun `score halves every seven days and future dates have zero age`() {
        assertEquals(10, ContextualUsageHistory.score(listOf(entry(day = 93, mass = 2.0)), "spotify", 10, 0, 100))
        assertEquals(20, ContextualUsageHistory.score(listOf(entry(day = 101, mass = 2.0)), "spotify", 10, 0, 100))
    }

    @Test
    fun `score rounds only after summing temporal contributions`() {
        val entries = listOf(
            entry(bucket = 16, day = 100, mass = 1.0),
            entry(bucket = 24, day = 100, mass = 1.0),
        )
        assertEquals(5, ContextualUsageHistory.score(entries, "spotify", 10, 0, 100))
    }

    @Test
    fun `thirty-day entries remain and older entries are pruned`() {
        val thirtyDaysOld = entry(day = 70, mass = 8.0)
        val thirtyOneDaysOld = entry(day = 69, mass = 8.0)

        assertEquals(4, ContextualUsageHistory.score(listOf(thirtyDaysOld), "spotify", 10, 0, 100))
        assertEquals(0, ContextualUsageHistory.score(listOf(thirtyOneDaysOld), "spotify", 10, 0, 100))
        assertEquals(listOf(thirtyDaysOld), ContextualUsageHistory.prune(listOf(thirtyDaysOld, thirtyOneDaysOld), 100))
    }

    @Test
    fun `invalid entries never affect score or persisted history`() {
        val invalid = listOf(
            ContextualUsageEntry("", 20, 100, 2.0),
            ContextualUsageEntry("spotify", 48, 100, 2.0),
            ContextualUsageEntry("spotify", 20, 100, 0.0),
            ContextualUsageEntry("spotify", 20, 100, Double.NaN),
            ContextualUsageEntry("spotify", 20, 100, Double.POSITIVE_INFINITY),
        )

        assertEquals(0, ContextualUsageHistory.score(invalid, "spotify", 10, 0, 100))
        assertEquals(emptyList<ContextualUsageEntry>(), ContextualUsageHistory.prune(invalid, 100))
    }

    @Test
    fun `very large launch mass saturates score and increment`() {
        val maximum = entry(day = 100, mass = Int.MAX_VALUE.toDouble())
        assertEquals(Int.MAX_VALUE, ContextualUsageHistory.score(listOf(maximum), "spotify", 10, 0, 100))
        assertEquals(listOf(maximum), ContextualUsageHistory.record(listOf(maximum), "spotify", 10, 0, 100))
    }

    private fun entry(
        bucket: Int = 20,
        day: Long,
        mass: Double,
    ) = ContextualUsageEntry("spotify", bucket, day, mass)

    private fun localMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        GregorianCalendar(saoPaulo).apply {
            clear()
            set(year, month, day, hour, minute)
        }.timeInMillis
}
