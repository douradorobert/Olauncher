package app.olauncher.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class ContextualScoringSettingsStructureTest {
    private val androidNamespace = "http://schemas.android.com/apk/res/android"

    @Test
    fun `portrait and landscape expose contextual scoring controls`() {
        listOf(
            "app/src/main/res/layout/fragment_settings.xml",
            "app/src/main/res/layout-land/fragment_settings.xml",
        ).forEach { path ->
            val byId = elementsById(path)
            assertTrue("$path must expose minimum score", byId.containsKey("@+id/contextualMinimumScore"))
            assertTrue("$path must expose day profiles", byId.containsKey("@+id/separateContextualDayProfiles"))
        }
    }

    @Test
    fun `minimum score dialog matches the rule info pattern and has a stepper`() {
        val byId = elementsById("app/src/main/res/layout/dialog_minimum_score.xml")
        val info = byId.getValue("@+id/minimumScoreInfo")

        assertEquals("48dp", info.getAttributeNS(androidNamespace, "layout_width"))
        assertEquals("48dp", info.getAttributeNS(androidNamespace, "layout_height"))
        assertEquals("@drawable/ic_info", info.getAttributeNS(androidNamespace, "src"))
        assertEquals("@string/info", info.getAttributeNS(androidNamespace, "contentDescription"))
        assertTrue(byId.containsKey("@+id/minimumScoreMinus"))
        assertTrue(byId.containsKey("@+id/minimumScoreCurrent"))
        assertTrue(byId.containsKey("@+id/minimumScorePlus"))
    }

    private fun elementsById(path: String): Map<String, Element> {
        val file = File(path).takeIf(File::exists) ?: File("../$path")
        val document = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(file)
        val elements = document.getElementsByTagName("*")
        return (0 until elements.length)
            .map { elements.item(it) as Element }
            .associateBy { it.getAttributeNS(androidNamespace, "id") }
    }
}
