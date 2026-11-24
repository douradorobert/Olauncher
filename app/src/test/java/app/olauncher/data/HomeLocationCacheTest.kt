package app.olauncher.data

import androidx.core.util.Consumer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeLocationCacheTest {
    private data class Fix(val elapsedMillis: Long)

    private var now = 200_000L
    private val cache = HomeLocationCache<Fix>({ now }, Fix::elapsedMillis)
    private val requests = mutableListOf<Consumer<Fix?>>()
    private val request: (Consumer<Fix?>) -> Unit = { requests.add(it) }
    private var lastKnown: Fix? = null

    @Test
    fun reusesRecentFixUntilItExpires() {
        val fix = Fix(now - 10_000)
        cache.get(request, { lastKnown }) { assertEquals(fix, it) }
        requests.single().accept(fix)

        now = fix.elapsedMillis + 119_999
        cache.get(request, { lastKnown }) { assertEquals(fix, it) }
        assertEquals(1, requests.size)

        now++
        assertNull(cache.recent())
        cache.get(request, { lastKnown }) {}
        assertEquals(2, requests.size)
    }

    @Test
    fun sharesPendingRequestAndDeliversToLatestRefresh() {
        var earlierResults = 0
        val results = mutableListOf<Fix?>()
        cache.get(request, { lastKnown }) { earlierResults++ }
        cache.get(request, { lastKnown }) { results.add(it) }

        assertEquals(1, requests.size)
        val fix = Fix(now)
        requests.single().accept(fix)
        assertEquals(0, earlierResults)
        assertEquals(listOf(fix), results)
    }

    @Test
    fun missingFixDoesNotPreventTheNextRequest() {
        val results = mutableListOf<Fix?>()
        cache.get(request, { lastKnown }) { results.add(it) }
        requests.single().accept(null)

        assertEquals(listOf<Fix?>(null), results)
        assertNull(cache.recent())
        cache.get(request, { lastKnown }) {}
        assertEquals(2, requests.size)
    }

    @Test
    fun olderFallbackIsDeliveredWithoutBecomingRecent() {
        val oldFix = Fix(now - 120_000)
        lastKnown = oldFix
        cache.get(request, { lastKnown }) { assertEquals(oldFix, it) }
        requests.single().accept(null)
        assertNull(cache.recent())
    }

    @Test
    fun recentFallbackCanBeReused() {
        val recentFix = Fix(now - 30_000)
        lastKnown = recentFix
        cache.get(request, { lastKnown }) { assertEquals(recentFix, it) }
        requests.single().accept(null)

        assertEquals(recentFix, cache.recent())
        cache.get(request, { lastKnown }) { assertEquals(recentFix, it) }
        assertEquals(1, requests.size)
    }

    @Test
    fun clearingIgnoresAnOutstandingResult() {
        var delivered = false
        cache.get(request, { lastKnown }) { delivered = true }
        cache.clear()
        cache.get(request, { lastKnown }) {}

        requests.first().accept(Fix(now))
        assertEquals(false, delivered)
        assertNull(cache.recent())
        assertEquals(2, requests.size)
    }
}
