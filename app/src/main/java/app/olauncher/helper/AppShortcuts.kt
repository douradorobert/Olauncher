package app.olauncher.helper

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.UserHandle

data class AppShortcutCandidate(
    val id: String,
    val shortLabel: String?,
    val longLabel: String?,
    val rank: Int,
    val enabled: Boolean,
)

data class AppShortcutItem(
    val id: String,
    val label: String,
    val rank: Int,
)

fun selectAppShortcuts(
    candidates: List<AppShortcutCandidate>,
    limit: Int = 4,
): List<AppShortcutItem> = candidates
    .asSequence()
    .filter(AppShortcutCandidate::enabled)
    .mapNotNull { candidate ->
        val label = candidate.shortLabel?.trim().takeUnless { it.isNullOrEmpty() }
            ?: candidate.longLabel?.trim().takeUnless { it.isNullOrEmpty() }
            ?: return@mapNotNull null
        AppShortcutItem(candidate.id, label, candidate.rank)
    }
    .sortedWith(compareBy<AppShortcutItem> { it.rank }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    .distinctBy(AppShortcutItem::id)
    .take(limit.coerceAtLeast(0))
    .toList()

fun Context.getAppShortcuts(packageName: String, user: UserHandle): List<AppShortcutItem> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1 || packageName.isBlank()) return emptyList()
    val launcherApps = getSystemService(LauncherApps::class.java) ?: return emptyList()
    if (!launcherApps.hasShortcutHostPermission()) return emptyList()

    return runCatching {
        val query = LauncherApps.ShortcutQuery().apply {
            setPackage(packageName)
            setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
            )
        }
        val candidates = launcherApps.getShortcuts(query, user).orEmpty().map { shortcut ->
            AppShortcutCandidate(
                id = shortcut.id,
                shortLabel = shortcut.shortLabel?.toString(),
                longLabel = shortcut.longLabel?.toString(),
                rank = shortcut.rank,
                enabled = shortcut.isEnabled,
            )
        }
        selectAppShortcuts(candidates)
    }.getOrDefault(emptyList())
}

fun Context.startAppShortcut(packageName: String, shortcutId: String, user: UserHandle): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return false
    val launcherApps = getSystemService(LauncherApps::class.java) ?: return false
    if (!launcherApps.hasShortcutHostPermission()) return false
    return runCatching {
        launcherApps.startShortcut(packageName, shortcutId, null, null, user)
        true
    }.getOrDefault(false)
}
