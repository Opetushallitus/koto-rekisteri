@file:Suppress("ktlint:standard:no-wildcard-imports")

package fi.oph.kitu.html.table

import fi.oph.kitu.html.ModalCommand
import fi.oph.kitu.html.input
import fi.oph.kitu.html.modal
import fi.oph.kitu.html.modalCommandButton
import fi.oph.kitu.html.submitButton
import kotlinx.html.*
import java.time.LocalDate

fun FlowContent.tableFilterDialog(block: FlowContent.() -> Unit) {
    val modalId = "table-filter-dialog"
    modalCommandButton(modalId, ModalCommand.OPEN) {
        +"Rajaa näytettävät tiedot"
    }
    modal(modalId, "Tiedon rajaus") {
        form(action = ".", method = FormMethod.get) {
            block()

            footer {
                fieldSet(classes = "grid") {
                    submitButton("Rajaa")
                }
            }
        }
    }
}

fun FlowContent.dateFilter(
    filterId: String,
    labelText: String,
    value: LocalDate?,
) {
    label {
        +labelText
        input(type = InputType.date, name = filterId, value = value?.toString()) {}
    }
}

fun FlowContent.toggleFilter(
    filterId: String,
    labelText: String,
    value: Boolean,
) {
    label {
        input(type = InputType.checkBox, name = filterId, checked = value)
        +labelText
    }
}

inline fun <reified E : Enum<E>> FlowContent.enumFilter(
    filterId: String,
    labelText: String,
    value: E?,
) {
    val options =
        enumValues<E>().filterNot {
            E::class.java.getField(it.name).isAnnotationPresent(HideInTableFilter::class.java)
        }

    if (options.size <= 5) {
        p { +labelText }
        options.forEach { option ->
            label {
                input(type = InputType.radio, name = filterId, value = option.name) {
                    if (option == value) {
                        attributes["checked"] = "checked"
                    }
                }
                +option.name
            }
        }
    } else {
        label {
            +labelText
            select {
                attributes["name"] = filterId
                options.forEach { option ->
                    option {
                        attributes["value"] = option.name
                        if (option == value) {
                            attributes["selected"] = "selected"
                        }
                        +option.name
                    }
                }
            }
        }
    }
}

annotation class HideInTableFilter
