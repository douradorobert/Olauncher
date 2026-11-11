package app.olauncher.data

import org.json.JSONArray
import org.json.JSONObject

data class DecodedContextualUsage(
    val entries: List<ContextualUsageEntry>,
    val requiresRewrite: Boolean,
)

object ContextualUsageHistoryCodec {
    private const val VERSION = 2

    fun decode(serialized: String, migrationEpochDay: Long): DecodedContextualUsage {
        val root = runCatching { JSONObject(serialized) }.getOrNull()
            ?: return DecodedContextualUsage(emptyList(), true)
        return if (root.has("version")) decodeVersionTwo(root) else migrateLegacy(root, migrationEpochDay)
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
                    .put("count", entry.count))
            }
        return JSONObject().put("version", VERSION).put("entries", encodedEntries).toString()
    }

    private fun decodeVersionTwo(root: JSONObject): DecodedContextualUsage {
        if (root.optInt("version", -1) != VERSION) return DecodedContextualUsage(emptyList(), true)
        val array = root.optJSONArray("entries") ?: return DecodedContextualUsage(emptyList(), true)
        var requiresRewrite = false
        val entries = buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index)
                val entry = item?.toEntry()
                if (entry == null || !isValid(entry)) {
                    requiresRewrite = true
                } else {
                    add(entry)
                }
            }
        }
        return DecodedContextualUsage(entries.sortedEntries(), requiresRewrite)
    }

    private fun migrateLegacy(root: JSONObject, migrationEpochDay: Long): DecodedContextualUsage {
        val entries = buildList {
            root.keys().forEach { key ->
                val separator = key.lastIndexOf('#')
                if (separator <= 0 || separator == key.lastIndex) return@forEach
                val identity = key.substring(0, separator)
                val bucket = key.substring(separator + 1).toIntOrNull() ?: return@forEach
                val count = root.strictInt(key) ?: return@forEach
                val entry = ContextualUsageEntry(identity, bucket, migrationEpochDay, count)
                if (isValid(entry)) add(entry)
            }
        }
        return DecodedContextualUsage(entries.sortedEntries(), true)
    }

    private fun JSONObject.toEntry(): ContextualUsageEntry? {
        if (!has("app") || !has("bucket") || !has("day") || !has("count")) return null
        return ContextualUsageEntry(
            appIdentity = opt("app") as? String ?: return null,
            timeBucket = strictInt("bucket") ?: return null,
            epochDay = strictLong("day") ?: return null,
            count = strictInt("count") ?: return null,
        )
    }

    private fun JSONObject.strictInt(name: String): Int? =
        (opt(name) as? Number)?.toString()?.toIntOrNull()

    private fun JSONObject.strictLong(name: String): Long? =
        (opt(name) as? Number)?.toString()?.toLongOrNull()

    private fun isValid(entry: ContextualUsageEntry): Boolean =
        entry.appIdentity.isNotBlank() && entry.timeBucket in 0..5 && entry.count > 0

    private fun List<ContextualUsageEntry>.sortedEntries(): List<ContextualUsageEntry> =
        sortedWith(compareBy(ContextualUsageEntry::appIdentity)
            .thenBy(ContextualUsageEntry::timeBucket)
            .thenBy(ContextualUsageEntry::epochDay))
}
