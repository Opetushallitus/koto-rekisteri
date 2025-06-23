package fi.oph.kitu.html

import kotlinx.html.FORM
import kotlinx.html.FlowContent
import kotlinx.html.FormMethod
import kotlinx.html.InputType
import kotlinx.html.form
import kotlinx.html.input
import org.springframework.security.web.csrf.CsrfToken
import java.time.LocalDate

fun FlowContent.formPost(
    action: String,
    csrfToken: CsrfToken,
    content: FORM.() -> Unit,
) {
    form(action = action, method = FormMethod.post) {
        debugTrace()
        hiddenValue(csrfToken.parameterName, csrfToken.token)
        content()
    }
}

fun FlowContent.dateInput(
    name: String,
    date: LocalDate?,
    testId: String? = null,
) {
    input(type = InputType.date, name = name) {
        testId(testId)
        value = date?.toString().orEmpty()
    }
}

fun FlowContent.submitButton(text: String = "Tallenna") {
    val buttonId = "submit-${(0..9999999).random()}"

    input(type = InputType.submit) {
        attributes["id"] = buttonId
        value = text
        disabled = true
    }

    javascript(
        """
        const button = document.getElementById("$buttonId")
        button.form
            .querySelectorAll('input, select')
            .forEach(e => e.addEventListener(
                'change', 
                () => { button.disabled = false }
            ))
        """.trimIndent(),
    )
}

fun FlowContent.hiddenValue(
    name: String,
    value: String,
) {
    input(type = InputType.hidden) {
        this.name = name
        this.value = value
    }
}
