package fi.oph.kitu.yki.html

import fi.oph.kitu.html.error
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.br

fun FlowContent.errorsArticle(
    errorsCount: Long,
    errorPage: String,
) {
    if (errorsCount > 0) {
        error("Järjestelmässä on $errorsCount virhettä.") {
            br()
            a(errorPage) {
                +"Katso virheet"
            }
        }
    }
}
