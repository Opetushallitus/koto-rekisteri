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
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.vkt.CustomVktSuoritusRepository
import fi.oph.kitu.vkt.VktViewController
import kotlinx.html.*
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn
import org.springframework.hateoas.server.mvc.linkTo

object VktHyvaJaTyydyttavaSuorituksetPage {
    fun render(
        suoritukset: List<VktTableItem>,
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
                        VktViewController::hyvanJaTyydyttavanTaitotasonSuorituksetView,
                    ).toString(),
                ),
        ) {
            h1 { +"Hyvän ja tyydyttävän taitotason suoritukset" }
            vktSearch(searchQuery)
            vktHyvaJaTyydyttavaTable(suoritukset, sortedBy, sortDirection, pagination, translations, searchQuery)
        }
}

fun FlowContent.vktHyvaJaTyydyttavaTable(
    suoritukset: List<VktTableItem>,
    sortedBy: CustomVktSuoritusRepository.Column,
    sortDirection: SortDirection,
    pagination: Pagination,
    t: Translations,
    searchQuery: String?,
) {
    card(overflowAuto = true, compact = true) {
        fun getHref(
            oppijanumero: String?,
            kieli: Koodisto.Tutkintokieli,
        ) = oppijanumero?.let {
            WebMvcLinkBuilder
                .linkTo(
                    methodOn(VktViewController::class.java).ilmoittautuneenArviointiView(it, kieli),
                ).toString()
        }
            ?: "#"

        displayTable(
            suoritukset,
            listOf(
                CustomVktSuoritusRepository.Column.Sukunimi.withValue {
                    a(href = getHref(it.oppijanumero, it.kieli)) {
                        +(it.sukunimi.orEmpty())
                    }
                },
                CustomVktSuoritusRepository.Column.Etunimet.withValue { +(it.etunimet) },
                CustomVktSuoritusRepository.Column.Kieli.withValue { +t.get(it.kieli) },
                CustomVktSuoritusRepository.Column.Tutkintopaiva.withValue {
                    finnishDate(it.tutkintopaiva)
                },
            ),
            sortedBy = sortedBy,
            sortDirection = sortDirection,
            testId = "suoritukset",
            rowTestId = { it.oppijanumero },
            urlParams = mapOf("search" to searchQuery),
        )
    }

    pagination(pagination)
}
