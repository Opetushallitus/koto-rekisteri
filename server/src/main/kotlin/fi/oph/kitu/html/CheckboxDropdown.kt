package fi.oph.kitu.html

import kotlinx.html.DETAILS
import kotlinx.html.InputType
import kotlinx.html.Tag
import kotlinx.html.attributesMapOf
import kotlinx.html.label
import kotlinx.html.li
import kotlinx.html.summary
import kotlinx.html.ul
import kotlinx.html.visit

data class CheckboxItem(
    val value: String,
    val label: String,
    val checked: Boolean = false,
    val testId: String? = null,
)

// https://picocss.com/docs/dropdown
fun Tag.checkboxDropdown(
    title: String,
    items: List<CheckboxItem>,
    name: String? = null,
    classes: String? = null,
    testId: String? = null,
    block: DETAILS.() -> Unit = {},
) {
    val classAttr = listOfNotNull("dropdown", classes).joinToString(" ")
    DETAILS(attributesMapOf("class", classAttr), consumer).visit {
        testId(testId)
        summary { +title }
        ul {
            items.forEach { item ->
                li {
                    label {
                        input(type = InputType.checkBox, value = item.value, checked = item.checked) {
                            if (name != null) attributes["name"] = name
                            testId(item.testId)
                        }
                        +item.label
                    }
                }
            }
        }
        block()
    }
}
