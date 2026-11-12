package app.olauncher.data

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

data class ContextualUsageEntry(
    val appIdentity: String,
    val timeBucket: Int,
    val epochDay: Long,
    val launchMass: Double,
)

object ContextualUsageHistory {
    private const val MAX_AGE_DAYS = 30L
    private const val HALF_LIFE_DAYS = 7.0
    private const val POINTS_PER_LAUNCH = 10
    private const val BUCKETS_PER_DAY = 48
    private val maximumLaunchMass = Int.MAX_VALUE.toDouble()
    private val utc = TimeZone.getTimeZone("UTC")

    fun timeBucket(hour: Int, minute: Int): Int =
        hour.coerceIn(0, 23) * 2 + minute.coerceIn(0, 59) / 30

    fun temporalWeight(sourceBucket: Int, targetBucket: Int): Double {
        if (sourceBucket !in 0 until BUCKETS_PER_DAY || targetBucket !in 0 until BUCKETS_PER_DAY) return 0.0
        val directDistance = abs(sourceBucket - targetBucket)
        return when (min(directDistance, BUCKETS_PER_DAY - directDistance)) {
            0 -> 1.0
            1 -> 0.75
            2 -> 0.5
            3 -> 0.375
            4 -> 0.25
            5 -> 0.125
            else -> 0.0
        }
    }

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
        minute: Int,
        epochDay: Long,
    ): List<ContextualUsageEntry> {
        if (appIdentity.isBlank()) return entries
        val bucket = timeBucket(hour, minute)
        val index = entries.indexOfFirst {
            it.appIdentity == appIdentity && it.timeBucket == bucket && it.epochDay == epochDay
        }
        if (index < 0) return entries + ContextualUsageEntry(appIdentity, bucket, epochDay, 1.0)
        return entries.toMutableList().apply {
            val current = get(index)
            set(index, current.copy(launchMass = (current.launchMass + 1.0).coerceAtMost(maximumLaunchMass)))
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
        minute: Int,
        currentEpochDay: Long,
    ): Int {
        val bucket = timeBucket(hour, minute)
        return entries.asSequence()
            .filter { it.isValid() && it.appIdentity == appIdentity }
            .map { entry -> (currentEpochDay - entry.epochDay).coerceAtLeast(0L) to entry }
            .filter { (age, _) -> age <= MAX_AGE_DAYS }
            .sumOf { (age, entry) ->
                entry.launchMass * POINTS_PER_LAUNCH * temporalWeight(entry.timeBucket, bucket) *
                    0.5.pow(age / HALF_LIFE_DAYS)
            }
            .roundToInt()
    }

    private fun ContextualUsageEntry.isValid(): Boolean =
        appIdentity.isNotBlank() && timeBucket in 0 until BUCKETS_PER_DAY &&
            launchMass.isFinite() && launchMass > 0.0
}
