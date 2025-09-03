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

object VktErinomaisenSuorituksetPage {
    fun render(
        title: String,
        linkBuilder: WebMvcLinkBuilder,
        ilmoittautuneet: List<VktTableItem>,
        sortedBy: CustomVktSuoritusRepository.Column,
        sortDirection: SortDirection,
        pagination: Pagination,
        translations: Translations,
        searchQuery: String?,
    ): String =
        Page.renderHtml(
            wideContent = true,
            breadcrumbs = Navigation.getBreadcrumbs(linkBuilder),
        ) {
            h1 { +title }
            vktSearch(searchQuery)
            vktIlmoittautuneetTable(ilmoittautuneet, sortedBy, sortDirection, pagination, translations, searchQuery)
        }
}

fun FlowContent.vktIlmoittautuneetTable(
    ilmoittautuneet: List<VktTableItem>,
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
        } ?: "#"

        displayTable(
            ilmoittautuneet,
            listOf(
                CustomVktSuoritusRepository.Column.Sukunimi.withValue {
                    a(href = getHref(it.oppijanumero, it.kieli)) {
                        +(it.sukunimi)
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
            testId = "ilmoittautuneet",
            rowTestId = { it.oppijanumero },
            urlParams = mapOf("search" to searchQuery),
        )
    }

    pagination(pagination)
}
