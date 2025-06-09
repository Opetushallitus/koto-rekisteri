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
import fi.oph.kitu.vkt.tiedonsiirtoschema.Henkilosuoritus
import kotlinx.html.*

object VktErinomaisenSuorituksetPage {
    fun render(
        title: String,
        ref: String,
        ilmoittautuneet: List<Henkilosuoritus<VktSuoritus>>,
        sortedBy: CustomVktSuoritusRepository.Column,
        sortDirection: SortDirection,
        pagination: Pagination,
        translations: Translations,
        searchQuery: String?,
    ): String =
        Page.renderHtml(
            wideContent = true,
            breadcrumbs = Navigation.getBreadcrumbs(ref),
        ) {
            h1 { +title }
            vktSearch(searchQuery)
            vktIlmoittautuneetTable(ilmoittautuneet, sortedBy, sortDirection, pagination, translations)
        }
}

fun FlowContent.vktIlmoittautuneetTable(
    ilmoittautuneet: List<Henkilosuoritus<VktSuoritus>>,
    sortedBy: CustomVktSuoritusRepository.Column,
    sortDirection: SortDirection,
    pagination: Pagination,
    t: Translations,
) {
    card(overflowAuto = true) {
        fun getHref(id: Int?) = id?.let { "/vkt/suoritukset/$it" } ?: "#"

        displayTable(
            ilmoittautuneet,
            listOf(
                CustomVktSuoritusRepository.Column.Sukunimi.withValue {
                    a(href = getHref(it.suoritus.internalId)) {
                        +(it.henkilo.sukunimi ?: "")
                    }
                },
                CustomVktSuoritusRepository.Column.Etunimet.withValue { +(it.henkilo.etunimet ?: "") },
                CustomVktSuoritusRepository.Column.Kieli.withValue { +t.get(it.suoritus.kieli) },
                CustomVktSuoritusRepository.Column.Tutkintopaiva.withValue {
                    it.suoritus.tutkintopaiva?.let { finnishDate(it) }
                },
            ),
            sortedBy = sortedBy,
            sortDirection = sortDirection,
            compact = true,
            testId = "ilmoittautuneet",
            rowTestId = { it.suoritus.lahdejarjestelmanId.toString() },
        )
    }

    pagination(pagination)
}
