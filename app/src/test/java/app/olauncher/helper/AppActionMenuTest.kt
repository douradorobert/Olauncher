package app.olauncher.helper

import org.junit.Assert.assertEquals
import org.junit.Test

class AppActionMenuTest {
    @Test
    fun `pin target is first empty visible home position`() {
        assertEquals(2, firstFreeHomePosition(listOf("one", "", "three"), 3))
        assertEquals(null, firstFreeHomePosition(listOf("one", "two"), 2))
        assertEquals(1, firstFreeHomePosition(emptyList(), 0))
    }

    @Test
    fun `drawer app offers pin and management actions`() {
        assertEquals(
            listOf(
                AppMenuAction.PIN_TO_HOME,
                AppMenuAction.RENAME,
                AppMenuAction.HIDE,
                AppMenuAction.INFO,
                AppMenuAction.UNINSTALL,
            ),
            appMenuActions(AppMenuSource.DRAWER_APP, canUninstall = true),
        )
    }

    @Test
    fun `system drawer app omits uninstall`() {
        assertEquals(
            listOf(
                AppMenuAction.PIN_TO_HOME,
                AppMenuAction.RENAME,
                AppMenuAction.HIDE,
                AppMenuAction.INFO,
            ),
            appMenuActions(AppMenuSource.DRAWER_APP, canUninstall = false),
        )
    }

    @Test
    fun `fixed app offers replacement and removal from home`() {
        assertEquals(
            listOf(
                AppMenuAction.REPLACE_HOME,
                AppMenuAction.REMOVE_FROM_HOME,
                AppMenuAction.INFO,
            ),
            appMenuActions(AppMenuSource.FIXED_APP),
        )
    }

    @Test
    fun `suggested app offers pin removal from candidates and info`() {
        assertEquals(
            listOf(
                AppMenuAction.PIN_TO_HOME,
                AppMenuAction.REMOVE_FROM_SUGGESTIONS,
                AppMenuAction.INFO,
            ),
            appMenuActions(AppMenuSource.SUGGESTED_APP),
        )
    }

    @Test
    fun `pinned Android shortcut keeps its own management actions`() {
        assertEquals(
            listOf(
                AppMenuAction.PIN_TO_HOME,
                AppMenuAction.RENAME,
                AppMenuAction.INFO,
                AppMenuAction.REMOVE_PINNED_SHORTCUT,
            ),
            appMenuActions(AppMenuSource.PINNED_SHORTCUT),
        )
    }
}
