package app.olauncher.data

import app.olauncher.data.calendar.CalendarEvent
import java.text.Normalizer
import java.util.Calendar
import java.util.Locale

data class ContextualApp(
    val label: String,
    val packageName: String,
    val activityClassName: String?,
    val user: String,
    val launchesInTimeBucket: Int,
) {
    val identity: String get() = "$packageName|$user"
}

data class ContextualRule(
    val appIdentity: String,
    val startMinutes: Int? = null,
    val endMinutes: Int? = null,
    val eventKeywords: Set<String> = emptySet(),
    val outsideHome: Boolean = false,
    val atHome: Boolean = false,
    val drivingBluetooth: Boolean = false,
    val drivingAndroidAuto: Boolean = false,
    val cooldownMinutes: Int = 0,
    val daysOfWeek: Set<Int> = emptySet(),
) {
    val isEmpty: Boolean
        get() = startMinutes == null && endMinutes == null && eventKeywords.isEmpty() && !outsideHome && !atHome &&
            !drivingBluetooth && !drivingAndroidAuto && daysOfWeek.isEmpty()
}

data class ContextualSignals(
    val bluetoothConnected: Boolean = false,
    val androidAutoConnected: Boolean = false,
    val lastBluetoothActiveAt: Long = 0L,
    val lastAndroidAutoActiveAt: Long = 0L,
)

object ContextualSuggestions {
    private val healthTerms = setOf(
        "consulta", "medico", "medica", "dentista", "exame", "clinica", "hospital",
        "terapia", "fisioterapia", "doctor", "appointment",
    )
    private val healthAppTerms = setOf("saude", "health", "bradesco")
    private val navigationAppTerms = setOf("maps", "mapas", "waze")

    fun availableSuggestionSlots(fixedAppCount: Int, maximumItems: Int): Int =
        (maximumItems - fixedAppCount).coerceAtLeast(0)

    fun rank(
        candidates: List<ContextualApp>,
        event: CalendarEvent?,
        outsideHome: Boolean = false,
        atHome: Boolean = false,
        currentMinutes: Int = 12 * 60,
        currentDayOfWeek: Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK),
        rules: Map<String, ContextualRule> = emptyMap(),
        limit: Int = 3,
        signals: ContextualSignals = ContextualSignals(),
        now: Long = System.currentTimeMillis(),
        minimumScore: Int = 10,
        lastRuleMatchedAt: Map<String, Long> = emptyMap(),
        onRuleMatched: (String, Long) -> Unit = { _, _ -> },
    ): List<ContextualApp> {
        val eventText = normalize(listOfNotNull(event?.title, event?.location).joinToString(" "))
        val isHealthEvent = healthTerms.any(eventText::contains)
        val hasDestination = event?.location?.isNotBlank() == true

        return candidates
            .mapNotNull { candidate ->
                val rule = rules[candidate.identity]
                if (rule != null) {
                    val matchesNow = matches(rule, eventText, outsideHome, atHome, currentMinutes, currentDayOfWeek, signals)
                    if (matchesNow && !rule.isEmpty) onRuleMatched(candidate.identity, now)
                    val lastMatchedAt = lastRuleMatchedAt[candidate.identity] ?: 0L
                    val cooldownMillis = rule.cooldownMinutes.coerceAtLeast(0) * 60_000L
                    val inCooldown = !matchesNow && cooldownMillis > 0 && lastMatchedAt > 0L &&
                        now >= lastMatchedAt && now - lastMatchedAt <= cooldownMillis
                    if (!matchesNow && !inCooldown) return@mapNotNull null
                }
                val appText = normalize(candidate.label + " " + candidate.packageName)
                var score = candidate.launchesInTimeBucket * 10
                if (rule != null && !rule.isEmpty) score += 200
                if (isHealthEvent && healthAppTerms.any(appText::contains)) score += 100
                if (hasDestination && navigationAppTerms.any(appText::contains)) score += 80
                if (outsideHome && navigationAppTerms.any(appText::contains)) score += 30
                if (score >= minimumScore) candidate to score else null
            }
            .sortedWith(compareByDescending<Pair<ContextualApp, Int>> { it.second }.thenBy { it.first.label.lowercase(Locale.getDefault()) })
            .take(limit)
            .map { it.first }
    }

    private fun matches(
        rule: ContextualRule,
        eventText: String,
        outsideHome: Boolean,
        atHome: Boolean,
        currentMinutes: Int,
        currentDayOfWeek: Int,
        signals: ContextualSignals,
    ): Boolean {
        if (rule.outsideHome && !outsideHome) return false
        if (rule.atHome && !atHome) return false
        if (rule.daysOfWeek.isNotEmpty() && currentDayOfWeek !in rule.daysOfWeek) return false
        if (rule.eventKeywords.isNotEmpty() && rule.eventKeywords.none { eventText.contains(normalize(it)) }) return false
        val start = rule.startMinutes
        val end = rule.endMinutes
        if (start != null && end != null) {
            val inRange = if (start <= end) currentMinutes in start..end else currentMinutes >= start || currentMinutes <= end
            if (!inRange) return false
        }
        if (rule.drivingBluetooth || rule.drivingAndroidAuto) {
            val bluetoothValid = rule.drivingBluetooth && signals.bluetoothConnected
            val androidAutoValid = rule.drivingAndroidAuto && signals.androidAutoConnected
            if (!bluetoothValid && !androidAutoValid) return false
        }
        return true
    }

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .lowercase(Locale.ROOT)
}
