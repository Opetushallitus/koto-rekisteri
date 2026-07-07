package fi.oph.kitu.html

import fi.oph.kitu.i18n.UiText
import fi.oph.kitu.i18n.unaryPlus
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.br

fun FlowContent.errorsArticle(
    errorsCount: Long,
    errorPage: String,
) {
    if (errorsCount > 0) {
        errorMessage(UiText.Error.jarjestelmassaVirheita(errorsCount)) {
            br()
            a(errorPage) {
                +UiText.Error.katsoVirheet
            }
        }
    }
}

fun FlowContent.koskiErrorsArticle(
    errorsCount: Long,
    errorPage: String,
) {
    if (errorsCount > 0) {
        errorMessage(UiText.Error.koskiSiirtoEpaonnistunut(errorsCount)) {
            br()
            a(errorPage) {
                +UiText.Error.katsoVirheet
            }
        }
    }
}

fun FlowContent.poikkeamatArticle(
    count: Long,
    link: String,
) {
    if (count > 0) {
        errorMessage(UiText.Error.poikkeamat(count)) {
            br()
            a(link) {
                +UiText.Error.katsoPoikkeamat
            }
        }
    }
}
