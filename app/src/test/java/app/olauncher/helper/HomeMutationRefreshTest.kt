package app.olauncher.helper

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeMutationRefreshTest {
    @Test
    fun `home mutation refreshes fixed apps and contextual suggestions`() {
        val refreshed = mutableListOf<String>()

        refreshAfterHomeMutation(
            refreshHome = { refreshed += "home" },
            refreshSuggestions = { refreshed += "suggestions" },
        )

        assertEquals(listOf("home", "suggestions"), refreshed)
    }
}
