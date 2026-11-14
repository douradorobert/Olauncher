package app.olauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ContextualCandidateRemovalTest {
    @Test
    fun `removing candidate also removes its contextual rule`() {
        val removedIdentity = "com.example.chat|UserHandle{0}"
        val keptIdentity = "com.example.maps|UserHandle{0}"
        val result = removeContextualCandidate(
            identity = removedIdentity,
            candidates = setOf(removedIdentity, keptIdentity),
            rules = mapOf(
                removedIdentity to ContextualRule(removedIdentity, atHome = true),
                keptIdentity to ContextualRule(keptIdentity, outsideHome = true),
            ),
        )

        assertEquals(setOf(keptIdentity), result.candidates)
        assertEquals(setOf(keptIdentity), result.rules.keys)
        assertFalse(result.rules.containsKey(removedIdentity))
    }
}
