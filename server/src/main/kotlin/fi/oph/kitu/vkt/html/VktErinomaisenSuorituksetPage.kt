@file:Suppress("ktlint:standard:no-wildcard-imports")

package fi.oph.kitu.vkt.html

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.Pagination
import fi.oph.kitu.html.ViewMessageData
import fi.oph.kitu.html.card
import fi.oph.kitu.html.pagination
import fi.oph.kitu.html.table.displayTable
import fi.oph.kitu.html.viewMessage
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
        ilmoittautuneet: List<VktTableItem>,
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
            h2 { +title }
            messages.forEach { viewMessage(it) }
            vktSearch(searchQuery)
            article { +"Yhteensä: ${pagination.numberOfItems}" }
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
            taso: Koodisto.VktTaitotaso,
        ) = oppijanumero?.let {
            WebMvcLinkBuilder
                .linkTo(
                    methodOn(VktViewController::class.java).ilmoittautuneenArviointiView(it, kieli, taso),
                ).toString()
        } ?: "#"

        displayTable(
            ilmoittautuneet,
            listOf(
                CustomVktSuoritusRepository.Column.Sukunimi.withHtml {
                    a(href = getHref(it.oppijanumero, it.kieli, it.taso)) {
                        +(it.sukunimi)
                    }
                },
                CustomVktSuoritusRepository.Column.Etunimet.withHtml { +(it.etunimet) },
                CustomVktSuoritusRepository.Column.Kieli.withHtml { +t.get(it.kieli) },
                CustomVktSuoritusRepository.Column.Tutkintopaiva.withHtml {
                    finnishDate(it.tutkintopaiva)
                },
            ),
            sortedBy = sortedBy,
            sortDirection = sortDirection,
            testId = "ilmoittautuneet",
            rowTestId = { "${it.oppijanumero}-${it.kieli.koodiarvo}" },
            urlParams = mapOf("search" to searchQuery),
        )
    }

    pagination(pagination)
}
