package app.olauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextualUsageHistoryCodecTest {
    @Test
    fun `version two round trip preserves entries in stable order`() {
        val encoded = ContextualUsageHistoryCodec.encode(
            listOf(
                ContextualUsageEntry("teams", 4, 20, 1),
                ContextualUsageEntry("spotify", 3, 21, 2),
                ContextualUsageEntry("spotify", 2, 22, 3),
            ),
        )

        assertEquals(
            listOf(
                ContextualUsageEntry("spotify", 2, 22, 3),
                ContextualUsageEntry("spotify", 3, 21, 2),
                ContextualUsageEntry("teams", 4, 20, 1),
            ),
            ContextualUsageHistoryCodec.decode(encoded, 99).entries,
        )
        assertFalse(ContextualUsageHistoryCodec.decode(encoded, 99).requiresRewrite)
    }

    @Test
    fun `invalid version two entries are discarded without losing valid ones`() {
        val decoded = ContextualUsageHistoryCodec.decode(
            """{"version":2,"entries":[
                {"app":"spotify","bucket":3,"day":100,"count":2},
                {"app":"","bucket":3,"day":100,"count":2},
                {"app":"teams","bucket":8,"day":100,"count":1}
            ]}""".trimIndent(),
            200,
        )

        assertEquals(listOf(ContextualUsageEntry("spotify", 3, 100, 2)), decoded.entries)
        assertTrue(decoded.requiresRewrite)
    }

    @Test
    fun `fractional version two numbers are rejected instead of truncated`() {
        val decoded = ContextualUsageHistoryCodec.decode(
            """{"version":2,"entries":[
                {"app":"valid","bucket":3,"day":100,"count":2},
                {"app":"fractional-bucket","bucket":3.9,"day":100,"count":2},
                {"app":"fractional-count","bucket":3,"day":100,"count":2.9}
            ]}""".trimIndent(),
            200,
        )

        assertEquals(listOf(ContextualUsageEntry("valid", 3, 100, 2)), decoded.entries)
        assertTrue(decoded.requiresRewrite)
    }

    @Test
    fun `malformed json recovers as empty rewriteable history`() {
        val decoded = ContextualUsageHistoryCodec.decode("not-json", 100)

        assertEquals(emptyList<ContextualUsageEntry>(), decoded.entries)
        assertTrue(decoded.requiresRewrite)
    }

    @Test
    fun `legacy history migrates on current day using last hash as separator`() {
        val decoded = ContextualUsageHistoryCodec.decode(
            """{"package#profile|user#3":4,"teams|user#5":2}""",
            123,
        )

        assertEquals(
            listOf(
                ContextualUsageEntry("package#profile|user", 3, 123, 4),
                ContextualUsageEntry("teams|user", 5, 123, 2),
            ),
            decoded.entries,
        )
        assertTrue(decoded.requiresRewrite)
    }

    @Test
    fun `legacy migration discards malformed keys ranges and counts`() {
        val decoded = ContextualUsageHistoryCodec.decode(
            """{"valid|user#2":3,"no-bucket":4,"bad|user#8":1,"zero|user#1":0}""",
            123,
        )

        assertEquals(listOf(ContextualUsageEntry("valid|user", 2, 123, 3)), decoded.entries)
        assertTrue(decoded.requiresRewrite)
    }

    @Test
    fun `legacy migration rejects fractional counts`() {
        val decoded = ContextualUsageHistoryCodec.decode(
            """{"valid|user#2":3,"fractional|user#2":2.9}""",
            123,
        )

        assertEquals(listOf(ContextualUsageEntry("valid|user", 2, 123, 3)), decoded.entries)
        assertTrue(decoded.requiresRewrite)
    }

    @Test
    fun `encoded migrated history is not migrated twice`() {
        val migrated = ContextualUsageHistoryCodec.decode("""{"spotify|user#3":7}""", 123)
        val decodedAgain = ContextualUsageHistoryCodec.decode(
            ContextualUsageHistoryCodec.encode(migrated.entries),
            200,
        )

        assertEquals(listOf(ContextualUsageEntry("spotify|user", 3, 123, 7)), decodedAgain.entries)
        assertFalse(decodedAgain.requiresRewrite)
    }
}
