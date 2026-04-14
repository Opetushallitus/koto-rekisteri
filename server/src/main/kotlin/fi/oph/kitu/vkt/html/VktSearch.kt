package fi.oph.kitu.vkt.html

import fi.oph.kitu.html.hiddenValues
import fi.oph.kitu.html.testId
import fi.oph.kitu.vkt.VktSuoritusFilter
import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.FormMethod
import kotlinx.html.InputType
import kotlinx.html.button
import kotlinx.html.fieldSet
import kotlinx.html.form
import kotlinx.html.id
import kotlinx.html.input

fun FlowContent.vktSearch(searchQuery: String?) {
    vktSearch(VktSuoritusFilter(searchQuery))
}

fun FlowContent.vktSearch(filter: VktSuoritusFilter) {
    form(action = "", method = FormMethod.get, classes = "grid center-vertically") {
        hiddenValues(filter.toMap().filterKeys { it != "search" })
        fieldSet {
            attributes["role"] = "search"
            input {
                testId("search")
                id = "search"
                type = InputType.search
                name = "search"
                value = filter.search.orEmpty()
                placeholder = "Oppijanumero tai nimi"
            }
            button {
                testId("search-button")
                type = ButtonType.submit
                +"Suodata"
            }
        }
    }
}
