@file:Suppress("ktlint:standard:no-wildcard-imports")

package fi.oph.kitu.vkt.html

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.DisplayTableEnum
import fi.oph.kitu.html.MenuItem
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.displayTable
import fi.oph.kitu.schema.Henkilosuoritus
import fi.oph.kitu.vkt.VktSuoritus
import kotlinx.html.*

object VktIlmoittautuneet {
    fun render(
        ilmoittautuneet: List<Henkilosuoritus<VktSuoritus>>,
        sortedBy: VktIlmoittautuneet.Column,
        sortDirection: SortDirection,
    ): String =
        Page.renderHtml(
            listOf(
                MenuItem("Valtionhallinnon kielitutkinto", "/vkt/ilmoittautuneet"),
                MenuItem("Ilmoittautuneet", "/vkt/ilmoittautuneet"),
            ),
        ) {
            vktIlmoittautuneetTable(ilmoittautuneet, sortedBy, sortDirection)
        }

    enum class Column(
        override val dbColumn: String,
        override val uiHeaderValue: String,
    ) : DisplayTableEnum {
        Sukunimi("sukunimi", "Sukunimi"),
        Etunimet("etunimet", "Etunimet"),
        Kieli("kieli", "Kieli"),
        Taitotaso("taitotaso", "Taitotaso"),
    }
}

fun FlowContent.vktIlmoittautuneetTable(
    ilmoittautuneet: List<Henkilosuoritus<VktSuoritus>>,
    sortedBy: VktIlmoittautuneet.Column,
    sortDirection: SortDirection,
) {
    displayTable(
        ilmoittautuneet,
        listOf(
            VktIlmoittautuneet.Column.Sukunimi.withValue { it.henkilo.sukunimi },
            VktIlmoittautuneet.Column.Etunimet.withValue { it.henkilo.etunimet },
            VktIlmoittautuneet.Column.Kieli.withValue { it.suoritus.kieli.koodiarvo },
            VktIlmoittautuneet.Column.Taitotaso.withValue { it.suoritus.taitotaso.koodiarvo },
        ),
        sortedBy = sortedBy,
        sortDirection = sortDirection,
    )
}
