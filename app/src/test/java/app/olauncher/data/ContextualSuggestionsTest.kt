package app.olauncher.data

import app.olauncher.data.calendar.CalendarEvent
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class ContextualSuggestionsTest {
    private fun app(label: String, launches: Int = 0) = ContextualApp(label, label, null, "user", launches)

    @Test
    fun `fixed apps and suggestions never exceed home limit`() {
        assertEquals(3, ContextualSuggestions.availableSuggestionSlots(fixedAppCount = 5, maximumItems = 8))
        assertEquals(0, ContextualSuggestions.availableSuggestionSlots(fixedAppCount = 8, maximumItems = 8))
        assertEquals(0, ContextualSuggestions.availableSuggestionSlots(fixedAppCount = 10, maximumItems = 8))
    }

    @Test
    fun `low score candidates stay hidden`() {
        assertEquals(emptyList<ContextualApp>(), ContextualSuggestions.rank(listOf(app("Unused")), null))
    }

    @Test
    fun `any rule remains valid during cooldown after it stops matching`() {
        val lamp = app("Lamp")
        val rule = ContextualRule(lamp.identity, atHome = true, cooldownMinutes = 30)
        val now = 2_000_000L
        assertEquals(
            listOf("Lamp"),
            ContextualSuggestions.rank(
                listOf(lamp), null, atHome = false, rules = mapOf(lamp.identity to rule), now = now,
                lastRuleMatchedAt = mapOf(lamp.identity to now - 20 * 60_000L),
            ).map { it.label },
        )
        assertEquals(
            emptyList<ContextualApp>(),
            ContextualSuggestions.rank(
                listOf(lamp), null, atHome = false, rules = mapOf(lamp.identity to rule), now = now,
                lastRuleMatchedAt = mapOf(lamp.identity to now - 31 * 60_000L),
            ),
        )
    }

    @Test
    fun `matching rule reports its last valid instant`() {
        val lamp = app("Lamp")
        val rule = ContextualRule(lamp.identity, atHome = true, cooldownMinutes = 30)
        val matches = mutableMapOf<String, Long>()

        ContextualSuggestions.rank(
            listOf(lamp), null, atHome = true, rules = mapOf(lamp.identity to rule), now = 42L,
            onRuleMatched = matches::put,
        )

        assertEquals(42L, matches[lamp.identity])
    }

    @Test
    fun `learned time bucket usage determines default order`() {
        val ranked = ContextualSuggestions.rank(listOf(app("Spotify", 4), app("Mfit", 7), app("Wellhub", 2)), null)
        assertEquals(listOf("Mfit", "Spotify", "Wellhub"), ranked.map { it.label })
    }

    @Test
    fun `health appointment boosts health app`() {
        val event = CalendarEvent(1, "Consulta dermatologista", 0, 1, false, "")
        val ranked = ContextualSuggestions.rank(listOf(app("Spotify", 5), app("Bradesco Saúde")), event)
        assertEquals("Bradesco Saúde", ranked.first().label)
    }

    @Test
    fun `event destination boosts navigation app`() {
        val event = CalendarEvent(1, "Cortar cabelo", 0, 1, false, "Rua das Flores")
        val ranked = ContextualSuggestions.rank(listOf(app("Spotify", 5), app("Google Maps")), event)
        assertEquals("Google Maps", ranked.first().label)
    }

    @Test
    fun `being outside home boosts navigation app`() {
        val ranked = ContextualSuggestions.rank(listOf(app("Spotify", 2), app("Google Maps")), null, outsideHome = true)
        assertEquals("Google Maps", ranked.first().label)
    }

    @Test
    fun `at home rule requires confirmed home context`() {
        val roku = app("Roku")
        val rules = mapOf(roku.identity to ContextualRule(roku.identity, atHome = true))
        assertEquals(
            listOf("Roku"),
            ContextualSuggestions.rank(listOf(roku), null, atHome = true, rules = rules).map { it.label },
        )
        assertEquals(
            emptyList<ContextualApp>(),
            ContextualSuggestions.rank(listOf(roku), null, atHome = false, rules = rules),
        )
    }

    @Test
    fun `explicit time rule hides app outside configured range`() {
        val mfit = app("Mfit")
        val rules = mapOf(mfit.identity to ContextualRule(mfit.identity, startMinutes = 6 * 60, endMinutes = 10 * 60))
        assertEquals(emptyList<ContextualApp>(), ContextualSuggestions.rank(listOf(mfit), null, currentMinutes = 14 * 60, rules = rules))
        assertEquals(listOf("Mfit"), ContextualSuggestions.rank(listOf(mfit), null, currentMinutes = 8 * 60, rules = rules).map { it.label })
    }

    @Test
    fun `explicit calendar keyword rule matches normalized title`() {
        val health = app("Bradesco Saúde")
        val rules = mapOf(health.identity to ContextualRule(health.identity, eventKeywords = setOf("consulta")))
        val event = CalendarEvent(1, "Consulta cardiologista", 0, 1, false, "")
        assertEquals("Bradesco Saúde", ContextualSuggestions.rank(listOf(health), event, rules = rules).first().label)
    }

    @Test
    fun `weekday rule only matches selected days`() {
        val spotify = app("Spotify")
        val rules = mapOf(spotify.identity to ContextualRule(spotify.identity, daysOfWeek = setOf(Calendar.MONDAY, Calendar.FRIDAY)))
        assertEquals(listOf("Spotify"), ContextualSuggestions.rank(listOf(spotify), null, currentDayOfWeek = Calendar.MONDAY, rules = rules).map { it.label })
        assertEquals(emptyList<ContextualApp>(), ContextualSuggestions.rank(listOf(spotify), null, currentDayOfWeek = Calendar.TUESDAY, rules = rules))
    }
}
