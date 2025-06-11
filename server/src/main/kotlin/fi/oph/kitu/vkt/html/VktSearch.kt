package fi.oph.kitu.vkt.html

import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.FormMethod
import kotlinx.html.InputType
import kotlinx.html.button
import kotlinx.html.fieldSet
import kotlinx.html.form
import kotlinx.html.id
import kotlinx.html.input

fun FlowContent.vktSearch(query: String?) {
    form(action = "", method = FormMethod.get, classes = "grid center-vertically") {
        fieldSet {
            attributes["role"] = "search"
            input {
                id = "search"
                type = InputType.search
                name = "search"
                value = query ?: ""
                placeholder = "Oppijanumero, nimi tai tutkintopäivä"
            }
            button {
                type = ButtonType.submit
                +"Suodata"
            }
        }
    }
}
