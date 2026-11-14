package app.olauncher.helper

import org.junit.Assert.assertEquals
import org.junit.Test

class AppShortcutSelectorTest {
    @Test
    fun `disabled and unlabeled shortcuts are excluded`() {
        val result = selectAppShortcuts(
            listOf(
                AppShortcutCandidate("enabled", "Open chat", null, 0, true),
                AppShortcutCandidate("disabled", "Hidden", null, 1, false),
                AppShortcutCandidate("unlabeled", " ", null, 2, true),
            )
        )

        assertEquals(listOf("enabled"), result.map { it.id })
    }

    @Test
    fun `duplicates are removed after rank ordering`() {
        val result = selectAppShortcuts(
            listOf(
                AppShortcutCandidate("same", "Later", null, 4, true),
                AppShortcutCandidate("same", "Earlier", null, 1, true),
                AppShortcutCandidate("other", "Other", null, 2, true),
            )
        )

        assertEquals(listOf("same", "other"), result.map { it.id })
        assertEquals("Earlier", result.first().label)
    }

    @Test
    fun `rank then label determine order and long label is the fallback`() {
        val result = selectAppShortcuts(
            listOf(
                AppShortcutCandidate("z", "Zulu", null, 2, true),
                AppShortcutCandidate("b", null, "Beta", 1, true),
                AppShortcutCandidate("a", "Alpha", null, 1, true),
            )
        )

        assertEquals(listOf("Alpha", "Beta", "Zulu"), result.map { it.label })
    }

    @Test
    fun `only the first four shortcuts are returned`() {
        val result = selectAppShortcuts(
            (0..5).map { index ->
                AppShortcutCandidate("id-$index", "Shortcut $index", null, index, true)
            }
        )

        assertEquals(listOf("id-0", "id-1", "id-2", "id-3"), result.map { it.id })
    }
}
