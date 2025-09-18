@file:Suppress("ktlint:standard:no-wildcard-imports")

package fi.oph.kitu.vkt.html

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.Pagination
import fi.oph.kitu.html.ViewMessageData
import fi.oph.kitu.html.card
import fi.oph.kitu.html.displayTable
import fi.oph.kitu.html.pagination
import fi.oph.kitu.html.viewMessage
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
        messages: List<ViewMessageData>,
    ): String =
        Page.renderHtml(
            wideContent = true,
        ) {
            h1 { +"Valtionhallinnon kielitutkinto" }
            h2 { +"Hyvän ja tyydyttävän taidon tutkinnon arvioidut suoritukset" }
            messages.forEach { viewMessage(it) }
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
            taso: Koodisto.VktTaitotaso,
        ) = oppijanumero?.let {
            WebMvcLinkBuilder
                .linkTo(
                    methodOn(VktViewController::class.java).ilmoittautuneenArviointiView(it, kieli, taso),
                ).toString()
        }
            ?: "#"

        displayTable(
            suoritukset,
            listOf(
                CustomVktSuoritusRepository.Column.Sukunimi.withValue {
                    a(href = getHref(it.oppijanumero, it.kieli, it.taso)) {
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
            testId = "suoritukset",
            rowTestId = { "${it.oppijanumero}-${it.kieli.koodiarvo}" },
            urlParams = mapOf("search" to searchQuery),
        )
    }

    pagination(pagination)
}
