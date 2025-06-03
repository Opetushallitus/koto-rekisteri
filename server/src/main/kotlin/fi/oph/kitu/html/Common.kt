package fi.oph.kitu.html

import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.script
import kotlinx.html.unsafe

data class MenuItem(
    val title: String,
    val ref: String,
    val current: Boolean = false,
)

fun List<MenuItem>.setCurrentItem(ref: String?) =
    if (ref != null) {
        this.map { it.copy(current = it.ref == ref) }
    } else {
        this
    }

fun Tag.data(
    key: String,
    value: String,
) {
    attributes["data-$key"] = value
}

fun Tag.testId(id: String?) {
    if (id != null) data("testid", id)
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
