package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.HeaderCell
import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Navigation
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.error
import kotlinx.html.ButtonType
import kotlinx.html.FormMethod
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.br
import kotlinx.html.button
import kotlinx.html.fieldSet
import kotlinx.html.form
import kotlinx.html.input

object YkiSuorituksetPage {
    fun render(
        suoritukset: List<YkiSuoritusEntity>,
        header: List<HeaderCell<YkiSuoritusColumn>>,
        sortColumn: String,
        sortDirection: SortDirection,
        paging: Map<String, String>,
        versionHistory: Boolean,
        errorsCount: Long,
    ): String =
        Page.renderHtml(
            Navigation.getBreadcrumbs("/yki/suoritukset"),
        ) {
            if (errorsCount > 0) {
                error("Järjestelmässä on $errorsCount virhettä.") {
                    br()
                    a("/yki/suoritukset/virheet") {
                        +"Katso virheet"
                    }
                }

                form(
                    action = "",
                    method = FormMethod.get,
                    classes = "grid center-vertically",
                ) {
                    fieldSet {
                        attributes["role"] = "search"
                        input(
                            type = InputType.text,
                            name = "search",
                        ) {
                            attributes["id"] = "search"
                            // TODO: Maybe we should create a class for paging, so that we can minimize typo errors
                            attributes["value"] = paging["searchStr"]
                                ?: throw RuntimeException("Missing 'searchStr' field from paging.")
                            attributes["placeholder"] = "Oppijanumero, henkilötunnus tai hakusana"

                            button(type = ButtonType.submit) {
                                +"Suodata"
                            }
                        }
                    }
                }
            }
        }
}
