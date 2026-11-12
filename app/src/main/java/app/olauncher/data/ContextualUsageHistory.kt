package app.olauncher.data

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.pow
import kotlin.math.roundToInt

data class ContextualUsageEntry(
    val appIdentity: String,
    val timeBucket: Int,
    val epochDay: Long,
    val count: Int,
)

object ContextualUsageHistory {
    private const val MAX_AGE_DAYS = 30L
    private const val HALF_LIFE_DAYS = 7.0
    private const val POINTS_PER_LAUNCH = 10
    private val utc = TimeZone.getTimeZone("UTC")

    fun timeBucket(hour: Int): Int = hour.coerceIn(0, 23) / 4

    fun localEpochDay(nowMillis: Long, timeZone: TimeZone): Long {
        val local = GregorianCalendar(timeZone).apply { timeInMillis = nowMillis }
        val syntheticUtc = GregorianCalendar(utc).apply {
            clear()
            set(
                local.get(Calendar.YEAR),
                local.get(Calendar.MONTH),
                local.get(Calendar.DAY_OF_MONTH),
            )
        }
        return TimeUnit.MILLISECONDS.toDays(syntheticUtc.timeInMillis)
    }

    fun record(
        entries: List<ContextualUsageEntry>,
        appIdentity: String,
        hour: Int,
        epochDay: Long,
    ): List<ContextualUsageEntry> {
        if (appIdentity.isBlank()) return entries
        val bucket = timeBucket(hour)
        val index = entries.indexOfFirst {
            it.appIdentity == appIdentity && it.timeBucket == bucket && it.epochDay == epochDay
        }
        if (index < 0) return entries + ContextualUsageEntry(appIdentity, bucket, epochDay, 1)
        return entries.toMutableList().apply {
            val current = get(index)
            set(index, current.copy(count = if (current.count == Int.MAX_VALUE) Int.MAX_VALUE else current.count + 1))
        }
    }

    fun prune(entries: List<ContextualUsageEntry>, currentEpochDay: Long): List<ContextualUsageEntry> =
        entries.filter { entry ->
            entry.isValid() && currentEpochDay - entry.epochDay <= MAX_AGE_DAYS
        }

    fun score(
        entries: List<ContextualUsageEntry>,
        appIdentity: String,
        hour: Int,
        currentEpochDay: Long,
    ): Int {
        val bucket = timeBucket(hour)
        return entries.asSequence()
            .filter { it.isValid() && it.appIdentity == appIdentity && it.timeBucket == bucket }
            .map { entry -> (currentEpochDay - entry.epochDay).coerceAtLeast(0L) to entry }
            .filter { (age, _) -> age <= MAX_AGE_DAYS }
            .sumOf { (age, entry) ->
                entry.count.toDouble() * POINTS_PER_LAUNCH * 0.5.pow(age / HALF_LIFE_DAYS)
            }
            .roundToInt()
    }

    private fun ContextualUsageEntry.isValid(): Boolean =
        appIdentity.isNotBlank() && timeBucket in 0..5 && count > 0
}
