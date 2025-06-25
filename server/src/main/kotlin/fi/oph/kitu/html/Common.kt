package fi.oph.kitu.html

import kotlinx.html.FlowContent
import kotlinx.html.INPUT
import kotlinx.html.InputFormEncType
import kotlinx.html.InputFormMethod
import kotlinx.html.InputType
import kotlinx.html.Tag
import kotlinx.html.article
import kotlinx.html.script
import kotlinx.html.unsafe
import kotlinx.html.input as inputBase

fun Tag.data(
    key: String,
    value: String,
) {
    attributes["data-$key"] = value
}

fun Tag.testId(id: String?) {
    if (id != null) data("testid", id)
}

fun FlowContent.input(
    type: InputType? = null,
    formEncType: InputFormEncType? = null,
    formMethod: InputFormMethod? = null,
    name: String? = null,
    classes: String? = null,
    id: String? = null,
    value: String? = null,
    placeholder: String? = null,
    checked: Boolean? = null,
    block: INPUT.() -> Unit = {},
) {
    inputBase(
        type = type,
        formEncType = formEncType,
        formMethod = formMethod,
        name = name,
        classes = classes,
    ) {
        if (id != null) {
            attributes["id"] = id
        }

        if (value != null) {
            attributes["value"] = value
        }

        if (placeholder != null) {
            attributes["placeholder"] = placeholder
        }

        if (checked == true) {
            attributes["checked"] = ""
        }

        block()
    }
}

fun FlowContent.error(
    message: String,
    block: () -> Unit = {},
) {
    article(classes = "error-text") {
        +message
        block()
    }
}

fun FlowContent.javascript(code: String) {
    debugTrace()
    script {
        // Wrap code to IIFE to prevent pollution of global scope.
        unsafe { +"(() => {$code})()" }
    }
}

fun FlowContent.debugTrace() {
    val stackTrace = Throwable().stackTrace
    if (stackTrace.size > 1) {
        comment(" Stacktrace: ")
        stackTrace
            .drop(1)
            .takeWhile {
                it.className.startsWith(
                    "fi.oph.kitu",
                )
            }.forEach {
                comment("   $it ")
            }
    }
}

fun classes(vararg conditionAndName: Pair<Boolean?, String>) =
    conditionAndName
        .filter { it.first == true }
        .joinToString(" ") { it.second }
