package app.olauncher.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.w3c.dom.Element

class HomeLayoutStructureTest {
    private val androidNamespace = "http://schemas.android.com/apk/res/android"

    @Test
    fun `portrait and landscape center apps in the space below widgets`() {
        listOf(
            "app/src/main/res/layout/fragment_home.xml",
            "app/src/main/res/layout-land/fragment_home.xml",
        ).forEach(::assertWeightedContentFlow)
    }

    private fun assertWeightedContentFlow(path: String) {
        val file = File(path).takeIf(File::exists) ?: File("../$path")
        val document = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(file)
        val elements = document.getElementsByTagName("*")
        val byId = (0 until elements.length)
            .map { elements.item(it) as Element }
            .associateBy { it.getAttributeNS(androidNamespace, "id") }
        val widgets = byId.getValue("@+id/dateTimeLayout")
        val appScroll = byId.getValue("@+id/homeAppsScroll")
        val apps = byId.getValue("@+id/homeAppsLayout")
        val screenTime = byId.getValue("@+id/tvScreenTime")
        val contentColumn = widgets.parentNode as Element
        val root = contentColumn.parentNode as Element

        assertSame("$path must put widgets and the app viewport in the same flow", contentColumn, appScroll.parentNode)
        assertSame("$path must keep apps inside the scrollable viewport", appScroll, apps.parentNode)
        assertEquals("LinearLayout", contentColumn.tagName)
        assertEquals("vertical", contentColumn.getAttributeNS(androidNamespace, "orientation"))
        assertEquals("match_parent", contentColumn.getAttributeNS(androidNamespace, "layout_height"))
        assertEquals("wrap_content", widgets.getAttributeNS(androidNamespace, "layout_height"))
        assertEquals("0dp", appScroll.getAttributeNS(androidNamespace, "layout_height"))
        assertEquals("1", appScroll.getAttributeNS(androidNamespace, "layout_weight"))
        assertEquals("true", appScroll.getAttributeNS(androidNamespace, "fillViewport"))
        assertEquals("wrap_content", apps.getAttributeNS(androidNamespace, "layout_height"))
        assertEquals("center_vertical", apps.getAttributeNS(androidNamespace, "gravity"))

        assertEquals(listOf(widgets, appScroll), elementChildren(contentColumn))
        val rootChildren = elementChildren(root)
        assert(rootChildren.indexOf(screenTime) > rootChildren.indexOf(contentColumn)) {
            "$path must draw screen time above the content column"
        }
    }

    private fun elementChildren(parent: Element): List<Element> =
        (0 until parent.childNodes.length)
            .map { parent.childNodes.item(it) }
            .filterIsInstance<Element>()
}
