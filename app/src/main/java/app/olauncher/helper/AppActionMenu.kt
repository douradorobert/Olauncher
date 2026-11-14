package app.olauncher.helper

enum class AppMenuSource { DRAWER_APP, FIXED_APP, SUGGESTED_APP, PINNED_SHORTCUT }

enum class AppMenuAction {
    PIN_TO_HOME, REPLACE_HOME, REMOVE_FROM_HOME, REMOVE_FROM_SUGGESTIONS,
    RENAME, HIDE, INFO, UNINSTALL, REMOVE_PINNED_SHORTCUT,
}

fun appMenuActions(source: AppMenuSource, canUninstall: Boolean = false): List<AppMenuAction> = when (source) {
    AppMenuSource.DRAWER_APP -> buildList {
        add(AppMenuAction.PIN_TO_HOME)
        add(AppMenuAction.RENAME)
        add(AppMenuAction.HIDE)
        add(AppMenuAction.INFO)
        if (canUninstall) add(AppMenuAction.UNINSTALL)
    }
    AppMenuSource.FIXED_APP -> listOf(
        AppMenuAction.REPLACE_HOME, AppMenuAction.REMOVE_FROM_HOME, AppMenuAction.INFO,
    )
    AppMenuSource.SUGGESTED_APP -> listOf(
        AppMenuAction.PIN_TO_HOME, AppMenuAction.REMOVE_FROM_SUGGESTIONS, AppMenuAction.INFO,
    )
    AppMenuSource.PINNED_SHORTCUT -> listOf(
        AppMenuAction.PIN_TO_HOME, AppMenuAction.RENAME, AppMenuAction.INFO,
        AppMenuAction.REMOVE_PINNED_SHORTCUT,
    )
}

data class AppDialogAction(val label: CharSequence, val onClick: () -> Unit)

fun firstFreeHomePosition(packageNames: List<String>, visibleCount: Int): Int? {
    val effectiveCount = visibleCount.coerceAtLeast(1)
    return (1..effectiveCount).firstOrNull { position ->
        packageNames.getOrNull(position - 1).isNullOrBlank()
    }
}
