package fi.oph.kitu.html

import fi.oph.kitu.i18n.LocalizedString
import fi.oph.kitu.i18n.UiText
import fi.oph.kitu.i18n.unaryPlus
import fi.oph.kitu.util.validation.Validation
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.li
import kotlinx.html.small
import kotlinx.html.ul
import kotlinx.html.label as labelTag

class FormErrors private constructor(
    private val byPath: Map<String, List<String>>,
) {
    operator fun get(name: String): List<String> = byPath[name].orEmpty()

    val yleiset: List<String> get() = this[""]

    fun isEmpty(): Boolean = byPath.isEmpty()

    fun isNotEmpty(): Boolean = !isEmpty()

    companion object {
        val EMPTY = FormErrors(emptyMap())

        fun of(errors: Iterable<Validation.ValidationError>): FormErrors =
            FormErrors(errors.groupBy({ it.path.joinToString(".") }, { it.message }))
    }
}

fun Tag.ariaInvalid(invalid: Boolean) {
    if (invalid) attributes["aria-invalid"] = "true"
}

fun FlowContent.formField(
    label: LocalizedString,
    name: String,
    errors: FormErrors = FormErrors.EMPTY,
    testId: String? = null,
    input: FlowContent.(invalid: Boolean) -> Unit,
) {
    val fieldErrors = errors[name]
    val invalid = fieldErrors.isNotEmpty()

    labelTag {
        if (testId != null) data("testid", testId)
        +label
        input(invalid)
        if (invalid) {
            small {
                if (testId != null) data("testid", "$testId-error")
                +fieldErrors.joinToString(" ")
            }
        }
    }
}

fun FlowContent.formErrorSummary(errors: FormErrors) {
    val yleiset = errors.yleiset
    if (yleiset.isEmpty()) return

    errorMessage(UiText.Form.tarkistaTiedot) {
        ul {
            data("testid", "formErrorSummary")
            yleiset.forEach { virhe ->
                li { +virhe }
            }
        }
    }
}
