package app.olauncher.data

import org.json.JSONArray
import org.json.JSONObject

data class DecodedContextualUsage(
    val entries: List<ContextualUsageEntry>,
    val requiresRewrite: Boolean,
)

object ContextualUsageHistoryCodec {
    private const val VERSION = 3
    private const val VERSION_TWO = 2
    private const val HALF_HOUR_BUCKETS_PER_OLD_BUCKET = 8

    fun decode(serialized: String, migrationEpochDay: Long): DecodedContextualUsage {
        val root = runCatching { JSONObject(serialized) }.getOrNull()
            ?: return DecodedContextualUsage(emptyList(), true)
        return when (root.strictInt("version")) {
            VERSION -> decodeVersionThree(root)
            VERSION_TWO -> migrateVersionTwo(root)
            null -> if (root.has("version")) DecodedContextualUsage(emptyList(), true)
                else migrateLegacy(root, migrationEpochDay)
            else -> DecodedContextualUsage(emptyList(), true)
        }
    }

    fun encode(entries: List<ContextualUsageEntry>): String {
        val encodedEntries = JSONArray()
        entries.filter(ContextualUsageHistoryCodec::isValid)
            .sortedWith(compareBy(ContextualUsageEntry::appIdentity)
                .thenBy(ContextualUsageEntry::timeBucket)
                .thenBy(ContextualUsageEntry::epochDay))
            .forEach { entry ->
                encodedEntries.put(JSONObject()
                    .put("app", entry.appIdentity)
                    .put("bucket", entry.timeBucket)
                    .put("day", entry.epochDay)
                    .put("mass", entry.launchMass))
            }
        return JSONObject().put("version", VERSION).put("entries", encodedEntries).toString()
    }

    private fun decodeVersionThree(root: JSONObject): DecodedContextualUsage {
        val array = root.optJSONArray("entries") ?: return DecodedContextualUsage(emptyList(), true)
        var requiresRewrite = false
        val entries = buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index)
                val entry = item?.toVersionThreeEntry()
                if (entry == null || !isValid(entry)) {
                    requiresRewrite = true
                } else {
                    add(entry)
                }
            }
        }
        return DecodedContextualUsage(entries.sortedEntries(), requiresRewrite)
    }

    private fun migrateVersionTwo(root: JSONObject): DecodedContextualUsage {
        val array = root.optJSONArray("entries") ?: return DecodedContextualUsage(emptyList(), true)
        val entries = buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val identity = item.opt("app") as? String ?: continue
                val bucket = item.strictInt("bucket") ?: continue
                val day = item.strictLong("day") ?: continue
                val count = item.strictInt("count") ?: continue
                addAll(migrateFourHourEntry(identity, bucket, day, count))
            }
        }
        return DecodedContextualUsage(entries.sortedEntries(), true)
    }

    private fun migrateLegacy(root: JSONObject, migrationEpochDay: Long): DecodedContextualUsage {
        val entries = buildList {
            root.keys().forEach { key ->
                val separator = key.lastIndexOf('#')
                if (separator <= 0 || separator == key.lastIndex) return@forEach
                val identity = key.substring(0, separator)
                val bucket = key.substring(separator + 1).toIntOrNull() ?: return@forEach
                val count = root.strictInt(key) ?: return@forEach
                addAll(migrateFourHourEntry(identity, bucket, migrationEpochDay, count))
            }
        }
        return DecodedContextualUsage(entries.sortedEntries(), true)
    }

    private fun migrateFourHourEntry(
        identity: String,
        oldBucket: Int,
        epochDay: Long,
        count: Int,
    ): List<ContextualUsageEntry> {
        if (identity.isBlank() || oldBucket !in 0..5 || count <= 0) return emptyList()
        val mass = count.toDouble() / HALF_HOUR_BUCKETS_PER_OLD_BUCKET
        val firstBucket = oldBucket * HALF_HOUR_BUCKETS_PER_OLD_BUCKET
        return (firstBucket until firstBucket + HALF_HOUR_BUCKETS_PER_OLD_BUCKET).map { bucket ->
            ContextualUsageEntry(identity, bucket, epochDay, mass)
        }
    }

    private fun JSONObject.toVersionThreeEntry(): ContextualUsageEntry? {
        if (!has("app") || !has("bucket") || !has("day") || !has("mass")) return null
        return ContextualUsageEntry(
            appIdentity = opt("app") as? String ?: return null,
            timeBucket = strictInt("bucket") ?: return null,
            epochDay = strictLong("day") ?: return null,
            launchMass = strictDouble("mass") ?: return null,
        )
    }

    private fun JSONObject.strictInt(name: String): Int? =
        (opt(name) as? Number)?.toString()?.toIntOrNull()

    private fun JSONObject.strictLong(name: String): Long? =
        (opt(name) as? Number)?.toString()?.toLongOrNull()

    private fun JSONObject.strictDouble(name: String): Double? =
        (opt(name) as? Number)?.toString()?.toDoubleOrNull()?.takeIf(Double::isFinite)

    private fun isValid(entry: ContextualUsageEntry): Boolean =
        entry.appIdentity.isNotBlank() && entry.timeBucket in 0..47 &&
            entry.launchMass.isFinite() && entry.launchMass > 0.0

    private fun List<ContextualUsageEntry>.sortedEntries(): List<ContextualUsageEntry> =
        sortedWith(compareBy(ContextualUsageEntry::appIdentity)
            .thenBy(ContextualUsageEntry::timeBucket)
            .thenBy(ContextualUsageEntry::epochDay))
}
