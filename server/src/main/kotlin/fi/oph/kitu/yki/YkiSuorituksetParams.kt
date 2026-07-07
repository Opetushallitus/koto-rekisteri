package fi.oph.kitu.yki

import fi.oph.kitu.html.table.ColumnTag
import fi.oph.kitu.i18n.aikarajausDescription
import fi.oph.kitu.jdbc.SortDirection
import fi.oph.kitu.util.SearchTerms
import fi.oph.kitu.webmvc.buildCsvFilename
import fi.oph.kitu.yki.suoritukset.YkiSuoritusColumn
import fi.oph.kitu.yki.suoritukset.YkiSuoritusFilter
import fi.oph.kitu.yki.suoritukset.YkiSuoritusOrder
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

data class YkiSuorituksetParams(
    var versionHistory: Boolean = false,
    var limit: Int = 100,
    var page: Int = 1,
    var sortColumn: YkiSuoritusColumn = YkiSuoritusColumn.Tutkintopaiva,
    var sortDirection: SortDirection = SortDirection.DESC,
    var search: String = "",
    var recallSearch: Boolean = false,
    @param:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    var tutkintoalku: LocalDate? = null,
    @param:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    var tutkintoloppu: LocalDate? = null,
    var tutkintokieli: Tutkintokieli? = null,
    var tutkintotaso: Tutkintotaso? = null,
    var piilotaHenkilotiedot: Boolean = false,
    val piilotaVanhentuneetTiedot: Boolean = false,
    val arviointitila: Arviointitila? = null,
) {
    fun toMap(): Map<String, String?> =
        mapOf(
            "recallSearch" to search.isNotEmpty().toTrueOrNull(),
            "versionHistory" to versionHistory.toTrueOrNull(),
            "page" to page.toString(),
            "sortColumn" to sortColumn.urlParam,
            "sortDirection" to sortDirection.name,
            "tutkintoalku" to tutkintoalku?.toString(),
            "tutkintoloppu" to tutkintoloppu?.toString(),
            "tutkintokieli" to tutkintokieli?.toString(),
            "tutkintotaso" to tutkintotaso?.toString(),
            "piilotaHenkilotiedot" to piilotaHenkilotiedot.toTrueOrNull(),
            "piilotaVanhentuneetTiedot" to piilotaVanhentuneetTiedot.toTrueOrNull(),
            "arviointitila" to arviointitila?.toString(),
        )

    fun toFilter() =
        YkiSuoritusFilter(
            search = SearchTerms.from(search),
            alkupaiva = tutkintoalku,
            loppupaiva = tutkintoloppu,
            tutkintokieli = tutkintokieli,
            tutkintotaso = tutkintotaso,
            arviointitila = arviointitila,
        )

    fun toOrder() =
        YkiSuoritusOrder(
            sortColumn = sortColumn,
            sortDirection = sortDirection,
        )

    fun excludeTags(): Set<ColumnTag> =
        setOfNotNull(
            if (piilotaHenkilotiedot) ColumnTag.PERSONAL_DATA else null,
            if (piilotaVanhentuneetTiedot) ColumnTag.OBSOLETE else null,
            if (versionHistory) null else ColumnTag.VERSION_HISTORY_ONLY,
        )

    fun csvFileName() =
        buildCsvFilename(
            "yki_suoritukset",
            piilotaHenkilotiedot,
            tutkintokieli?.toString(),
            tutkintotaso?.toString(),
            tutkintoalku?.toString(),
            tutkintoloppu?.toString(),
        )

    fun filterDescriptions(): List<String> =
        listOfNotNull(
            aikarajausDescription(tutkintoalku, tutkintoloppu),
            tutkintokieli?.let { "Tutkintokieli: $it" },
            tutkintotaso?.let { "Tutkintotaso: $it" },
            if (piilotaHenkilotiedot) "Henkilötiedot piilotettu" else null,
            if (piilotaVanhentuneetTiedot) "Vanhentuneet tietokentät piilotettu" else null,
            if (versionHistory) "Näytä versiohistoria" else null,
        )
}
