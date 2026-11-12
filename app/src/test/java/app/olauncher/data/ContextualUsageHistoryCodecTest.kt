package app.olauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextualUsageHistoryCodecTest {
    @Test
    fun `version three round trip preserves decimal mass in stable order`() {
        val encoded = ContextualUsageHistoryCodec.encode(
            listOf(
                ContextualUsageEntry("teams", 47, 20, 1.25),
                ContextualUsageEntry("spotify", 21, 21, 2.5),
                ContextualUsageEntry("spotify", 20, 22, 3.75),
            ),
        )

        val decoded = ContextualUsageHistoryCodec.decode(encoded, 99)
        assertEquals(3, org.json.JSONObject(encoded).getInt("version"))
        assertEquals(
            listOf(
                ContextualUsageEntry("spotify", 20, 22, 3.75),
                ContextualUsageEntry("spotify", 21, 21, 2.5),
                ContextualUsageEntry("teams", 47, 20, 1.25),
            ),
            decoded.entries,
        )
        assertFalse(decoded.requiresRewrite)
    }

    @Test
    fun `invalid version three entries are discarded without losing valid ones`() {
        val decoded = ContextualUsageHistoryCodec.decode(
            """{"version":3,"entries":[
                {"app":"spotify","bucket":20,"day":100,"mass":2.5},
                {"app":"","bucket":20,"day":100,"mass":2},
                {"app":"overflow","bucket":48,"day":100,"mass":1},
                {"app":"zero","bucket":20,"day":100,"mass":0},
                {"app":"negative","bucket":20,"day":100,"mass":-1},
                {"app":"text","bucket":20,"day":100,"mass":"NaN"}
            ]}""".trimIndent(),
            200,
        )

        assertEquals(listOf(ContextualUsageEntry("spotify", 20, 100, 2.5)), decoded.entries)
        assertTrue(decoded.requiresRewrite)
    }

    @Test
    fun `encode discards nonfinite mass`() {
        val decoded = ContextualUsageHistoryCodec.decode(
            ContextualUsageHistoryCodec.encode(
                listOf(
                    ContextualUsageEntry("valid", 20, 100, 1.0),
                    ContextualUsageEntry("nan", 20, 100, Double.NaN),
                    ContextualUsageEntry("infinite", 20, 100, Double.POSITIVE_INFINITY),
                ),
            ),
            100,
        )

        assertEquals(listOf(ContextualUsageEntry("valid", 20, 100, 1.0)), decoded.entries)
        assertFalse(decoded.requiresRewrite)
    }

    @Test
    fun `version two count is distributed across its eight half-hour buckets`() {
        val decoded = ContextualUsageHistoryCodec.decode(
            """{"version":2,"entries":[{"app":"spotify","bucket":2,"day":100,"count":8}]}""",
            200,
        )

        assertEquals((16..23).toList(), decoded.entries.map { it.timeBucket })
        assertEquals(List(8) { 1.0 }, decoded.entries.map { it.launchMass })
        assertEquals(8.0, decoded.entries.sumOf { it.launchMass }, 0.0)
        assertTrue(decoded.requiresRewrite)
    }

    @Test
    fun `version two fractional distribution conserves launch mass`() {
        val decoded = ContextualUsageHistoryCodec.decode(
            """{"version":2,"entries":[{"app":"spotify","bucket":2,"day":100,"count":2}]}""",
            200,
        )

        assertEquals(List(8) { 0.25 }, decoded.entries.map { it.launchMass })
        assertEquals(2.0, decoded.entries.sumOf { it.launchMass }, 0.0)
        assertTrue(decoded.requiresRewrite)
    }

    @Test
    fun `legacy history uses last hash and distributes current-day mass`() {
        val decoded = ContextualUsageHistoryCodec.decode(
            """{"package#profile|user#3":4}""",
            123,
        )

        assertEquals((24..31).toList(), decoded.entries.map { it.timeBucket })
        assertEquals(setOf("package#profile|user"), decoded.entries.map { it.appIdentity }.toSet())
        assertEquals(setOf(123L), decoded.entries.map { it.epochDay }.toSet())
        assertEquals(List(8) { 0.5 }, decoded.entries.map { it.launchMass })
        assertTrue(decoded.requiresRewrite)
    }

    @Test
    fun `malformed and unknown versions recover as empty rewriteable history`() {
        val malformed = ContextualUsageHistoryCodec.decode("not-json", 100)
        val unknown = ContextualUsageHistoryCodec.decode("""{"version":99,"entries":[]}""", 100)

        assertEquals(emptyList<ContextualUsageEntry>(), malformed.entries)
        assertTrue(malformed.requiresRewrite)
        assertEquals(emptyList<ContextualUsageEntry>(), unknown.entries)
        assertTrue(unknown.requiresRewrite)
    }

    @Test
    fun `encoded version two migration is not migrated twice`() {
        val migrated = ContextualUsageHistoryCodec.decode(
            """{"version":2,"entries":[{"app":"spotify|user","bucket":3,"day":123,"count":7}]}""",
            200,
        )
        val decodedAgain = ContextualUsageHistoryCodec.decode(
            ContextualUsageHistoryCodec.encode(migrated.entries),
            200,
        )

        assertEquals(migrated.entries, decodedAgain.entries)
        assertFalse(decodedAgain.requiresRewrite)
    }
}
