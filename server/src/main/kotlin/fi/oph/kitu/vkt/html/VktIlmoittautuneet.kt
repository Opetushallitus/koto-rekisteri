@file:Suppress("ktlint:standard:no-wildcard-imports")

package fi.oph.kitu.vkt.html

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.DisplayTableEnum
import fi.oph.kitu.html.MenuItem
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.Pagination
import fi.oph.kitu.html.card
import fi.oph.kitu.html.displayTable
import fi.oph.kitu.html.pagination
import fi.oph.kitu.i18n.Translations
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.vkt.tiedonsiirtoschema.Henkilosuoritus
import kotlinx.html.*

object VktIlmoittautuneet {
    fun render(
        ilmoittautuneet: List<Henkilosuoritus<VktSuoritus>>,
        sortedBy: Column,
        sortDirection: SortDirection,
        pagination: Pagination,
        translations: Translations,
        searchQuery: String?,
    ): String =
        Page.renderHtml(
            wideContent = true,
            breadcrumbs =
                listOf(
                    MenuItem("Valtionhallinnon kielitutkinto", "/vkt/ilmoittautuneet"),
                    MenuItem("Ilmoittautuneet", "/vkt/ilmoittautuneet"),
                ),
        ) {
            h1 { +"Erinomaisen taitotason ilmoittautuneet" }
            vktIlmoittautuneetSearch(searchQuery)
            vktIlmoittautuneetTable(ilmoittautuneet, sortedBy, sortDirection, pagination, translations)
        }

    enum class Column(
        override val dbColumn: String?,
        override val uiHeaderValue: String,
        override val urlParam: String,
    ) : DisplayTableEnum {
        Sukunimi("sukunimi", "Sukunimi", "sukunimi"),
        Etunimet("etunimi", "Etunimet", "etunimet"),
        Kieli("tutkintokieli", "Tutkintokieli", "kieli"),
        Taitotaso("taitotaso", "Taitotaso", "taitotaso"),
        Tutkintopaiva("tutkintopaiva", "Tutkintopäivä", "tutkintopaiva"),
    }
}

fun FlowContent.vktIlmoittautuneetTable(
    ilmoittautuneet: List<Henkilosuoritus<VktSuoritus>>,
    sortedBy: VktIlmoittautuneet.Column,
    sortDirection: SortDirection,
    pagination: Pagination,
    t: Translations,
) {
    card(overflowAuto = true) {
        fun getHref(id: Int?) = id?.let { "/vkt/ilmoittautuneet/$it" } ?: "#"

        displayTable(
            ilmoittautuneet,
            listOf(
                VktIlmoittautuneet.Column.Sukunimi.withValue {
                    a(href = getHref(it.suoritus.internalId)) {
                        +(it.henkilo.sukunimi ?: "")
                    }
                },
                VktIlmoittautuneet.Column.Etunimet.withValue { +(it.henkilo.etunimet ?: "") },
                VktIlmoittautuneet.Column.Kieli.withValue { +t.get(it.suoritus.kieli) },
                VktIlmoittautuneet.Column.Tutkintopaiva.withValue {
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
