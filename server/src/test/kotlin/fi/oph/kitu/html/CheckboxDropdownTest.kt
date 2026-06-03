package fi.oph.kitu.html

import kotlinx.html.section
import kotlinx.html.stream.createHTML
import kotlinx.html.table
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class CheckboxDropdownTest {
    @Test
    fun `renders inside a th and embeds the supplied data attributes`() {
        val html =
            createHTML().section {
                table {
                    thead {
                        tr {
                            th {
                                +"Kenttä"
                                checkboxDropdown(
                                    title = "Suodata",
                                    items =
                                        listOf(
                                            CheckboxItem(value = "etunimi", label = "etunimi"),
                                            CheckboxItem(value = "sukunimi", label = "sukunimi", checked = true),
                                        ),
                                    testId = "kentta-filter",
                                    dataAttributes = mapOf("filter-key" to "kentta"),
                                )
                            }
                        }
                    }
                }
            }

        assertTrue(html.contains("""data-testid="kentta-filter""""), "missing data-testid:\n$html")
        assertTrue(html.contains("""data-filter-key="kentta""""), "missing data-filter-key:\n$html")
        assertTrue(html.contains("""<summary>Suodata</summary>"""), "missing summary:\n$html")
        assertTrue(html.contains("""value="etunimi""""), "missing first checkbox value:\n$html")
        assertTrue(html.contains("""value="sukunimi" checked"""), "missing checked second checkbox:\n$html")
    }
}
