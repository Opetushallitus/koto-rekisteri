package fi.oph.kitu.yki

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.table.ColumnTag
import fi.oph.kitu.yki.suoritukset.YkiSuoritusColumn
import fi.oph.kitu.yki.suoritukset.YkiSuoritusFilter
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
        )

    fun toFilter() =
        YkiSuoritusFilter(
            search = search,
            alkupaiva = tutkintoalku,
            loppupaiva = tutkintoloppu,
            tutkintokieli = tutkintokieli,
            tutkintotaso = tutkintotaso,
        )

    fun excludeTags(): Set<ColumnTag> = if (piilotaHenkilotiedot) setOf(ColumnTag.PERSONAL_DATA) else emptySet()

    fun csvFileName() =
        listOfNotNull(
            "yki_suoritukset",
            if (piilotaHenkilotiedot) null else "henkilotiedot",
            tutkintokieli?.toString(),
            tutkintotaso?.toString(),
            tutkintoalku?.toString(),
            tutkintoloppu?.toString(),
        ).joinToString("_", postfix = ".csv")
}
