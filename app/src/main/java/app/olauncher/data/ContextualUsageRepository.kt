package app.olauncher.data

import java.util.TimeZone

data class ContextualUsageUpdate(
    val serialized: String,
    val score: Int,
    val changed: Boolean,
)

object ContextualUsageRepository {
    fun updateSerializedHistory(
        serialized: String,
        appIdentity: String,
        hour: Int,
        nowMillis: Long,
        timeZone: TimeZone,
        recordLaunch: Boolean = false,
    ): ContextualUsageUpdate {
        val currentDay = ContextualUsageHistory.localEpochDay(nowMillis, timeZone)
        val decoded = ContextualUsageHistoryCodec.decode(serialized, currentDay)
        val pruned = ContextualUsageHistory.prune(decoded.entries, currentDay)
        val updated = if (recordLaunch) {
            ContextualUsageHistory.record(pruned, appIdentity, hour, 0, currentDay)
        } else {
            pruned
        }
        val changed = decoded.requiresRewrite || updated != decoded.entries
        return ContextualUsageUpdate(
            serialized = if (changed) ContextualUsageHistoryCodec.encode(updated) else serialized,
            score = ContextualUsageHistory.score(updated, appIdentity, hour, 0, currentDay),
            changed = changed,
        )
    }
}
