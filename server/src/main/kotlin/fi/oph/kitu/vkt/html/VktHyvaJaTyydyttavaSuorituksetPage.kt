@file:Suppress("ktlint:standard:no-wildcard-imports")

package fi.oph.kitu.vkt.html

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Navigation
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.Pagination
import fi.oph.kitu.html.card
import fi.oph.kitu.html.displayTable
import fi.oph.kitu.html.pagination
import fi.oph.kitu.i18n.Translations
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.vkt.CustomVktSuoritusRepository
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.vkt.VktViewController
import fi.oph.kitu.vkt.tiedonsiirtoschema.Henkilosuoritus
import kotlinx.html.*
import org.springframework.hateoas.server.mvc.linkTo

object VktHyvaJaTyydyttavaSuorituksetPage {
    fun render(
        suoritukset: List<Henkilosuoritus<VktSuoritus>>,
        sortedBy: CustomVktSuoritusRepository.Column,
        sortDirection: SortDirection,
        pagination: Pagination,
        translations: Translations,
        searchQuery: String?,
    ): String =
        Page.renderHtml(
            wideContent = true,
            breadcrumbs =
                Navigation.getBreadcrumbs(
                    linkTo(
                        VktViewController::hyvanJaTyydyttavanTaitotasonIlmoittautuneetView,
                    ).toString(),
                ),
        ) {
            h1 { +"Hyvän ja tyydyttävän taitotason suoritukset" }
            vktSearch(searchQuery)
            vktHyvaJaTyydyttavaTable(suoritukset, sortedBy, sortDirection, pagination, translations, searchQuery)
        }
}

fun FlowContent.vktHyvaJaTyydyttavaTable(
    suoritukset: List<Henkilosuoritus<VktSuoritus>>,
    sortedBy: CustomVktSuoritusRepository.Column,
    sortDirection: SortDirection,
    pagination: Pagination,
    t: Translations,
    searchQuery: String?,
) {
    card(overflowAuto = true, compact = true) {
        fun getHref(id: Int?) = id?.let { "/vkt/suoritukset/$it" } ?: "#"

        displayTable(
            suoritukset,
            listOf(
                CustomVktSuoritusRepository.Column.Sukunimi.withValue {
                    a(href = getHref(it.suoritus.internalId)) {
                        +(it.henkilo.sukunimi.orEmpty())
                    }
                },
                CustomVktSuoritusRepository.Column.Etunimet.withValue { +(it.henkilo.etunimet.orEmpty()) },
                CustomVktSuoritusRepository.Column.Kieli.withValue { +t.get(it.suoritus.kieli) },
                CustomVktSuoritusRepository.Column.Tutkintopaiva.withValue {
                    it.suoritus.tutkintopaiva?.let { finnishDate(it) }
                },
            ),
            sortedBy = sortedBy,
            sortDirection = sortDirection,
            testId = "suoritukset",
            rowTestId = { it.suoritus.lahdejarjestelmanId.toString() },
            urlParams = mapOf("search" to searchQuery),
        )
    }

    pagination(pagination)
}
