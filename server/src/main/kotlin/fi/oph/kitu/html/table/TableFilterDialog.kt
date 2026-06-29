@file:Suppress("ktlint:standard:no-wildcard-imports")

package fi.oph.kitu.html.table

import fi.oph.kitu.html.DisplayEnum
import fi.oph.kitu.html.ModalCommand
import fi.oph.kitu.html.input
import fi.oph.kitu.html.modal
import fi.oph.kitu.html.modalCommandButton
import fi.oph.kitu.html.submitButton
import fi.oph.kitu.i18n.LocalizedString
import fi.oph.kitu.koodisto.Koodisto
import kotlinx.html.*
import java.time.LocalDate

fun FlowContent.tableFilterDialog(
    action: String,
    buttonText: String? = null,
    block: FlowContent.() -> Unit,
) {
    val modalId = "table-filter-dialog"
    modalCommandButton(modalId, ModalCommand.OPEN) {
        +(buttonText ?: "Rajaa näytettävät tiedot")
    }
    modal(modalId, "Tiedon rajaus") {
        form(action = action, method = FormMethod.get) {
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
    id: String,
    labelText: String,
    value: LocalDate?,
) {
    label {
        +labelText
        input(type = InputType.date, name = id, value = value?.toString()) {}
    }
}

fun FlowContent.toggleFilter(
    id: String,
    labelText: String,
    value: Boolean,
) {
    label {
        input(type = InputType.checkBox, name = id, checked = value)
        +labelText
    }
}

fun FlowContent.trueFalseOrAllFilter(
    id: String,
    labelText: String,
    value: Boolean?,
    optionLabels: Triple<String, String, String> = Triple("Kaikki", "Kyllä", "Ei"),
) {
    val options =
        mapOf(
            optionLabels.first to (null to ""),
            optionLabels.second to (true to "true"),
            optionLabels.third to (false to "false"),
        )

    p { +labelText }
    options.forEach { labelText, (optionValue, optionValueString) ->
        label {
            input(type = InputType.radio, name = id, value = optionValueString) {
                if (optionValue == value) {
                    attributes["checked"] = "checked"
                }
            }
            +labelText
        }
    }
}

inline fun <reified E : Enum<E>> FlowContent.enumFilter(
    id: String,
    labelText: String,
    value: E?,
) {
    val options =
        listOf(null) +
            enumValues<E>().filterNot {
                E::class.java.getField(it.name).isAnnotationPresent(HideInTableFilter::class.java)
            }

    if (options.size <= 5) {
        p { +labelText }
        options.forEach { option ->
            label {
                input(type = InputType.radio, name = id, value = option?.name.orEmpty()) {
                    if (option == value) {
                        attributes["checked"] = "checked"
                    }
                }
                +enumValueName(option)
            }
        }
    } else {
        label {
            +labelText
            select {
                attributes["name"] = id
                options.forEach { option ->
                    option {
                        attributes["value"] = option?.name.orEmpty()
                        if (option == value) {
                            attributes["selected"] = "selected"
                        }
                        +enumValueName(option)
                    }
                }
            }
        }
    }
}

inline fun <reified E : Enum<E>> enumValueName(value: E?): String =
    when (value) {
        is Koodisto.KoodiviiteNimella -> value.nimi.toString()
        is Nimetty -> value.nimi.toString()
        is DisplayEnum -> value.displayText()
        null -> "Kaikki"
        else -> value.name
    }

interface Nimetty {
    val nimi: LocalizedString
}

annotation class HideInTableFilter
