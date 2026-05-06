package fi.oph.kitu.kotoutumiskoulutus.suoritukset

import fi.oph.kitu.html.table.ColumnTag
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.jdbc.PAGINATED_DEFAULT_PAGE_SIZE
import fi.oph.kitu.jdbc.SortDirection
import fi.oph.kitu.yki.toTrueOrNull
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

data class KielitestiSuorituksetParams(
    val limit: Int = PAGINATED_DEFAULT_PAGE_SIZE,
    val page: Int = 1,
    val sortColumn: KielitestiSuoritusColumn = KielitestiSuoritusColumn.Suoritusaika,
    val sortDirection: SortDirection = SortDirection.DESC,
    val search: String = "",
    @param:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    var suoritusalku: LocalDate? = null,
    @param:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    var suoritusloppu: LocalDate? = null,
    val piilotaHenkilotiedot: Boolean = false,
    val testikieli: Testikieli? = null,
) {
    fun toFilter(): KielitestiSuoritusFilter =
        KielitestiSuoritusFilter(
            search = search,
            suoritusalku = suoritusalku,
            suoritusloppu = suoritusloppu,
            testikieli = testikieli,
        )

    fun toOrder(): KielitestiSuoritusOrder =
        KielitestiSuoritusOrder(
            sortColumn = sortColumn,
            sortDirection = sortDirection,
            pageNumber = page - 1,
        )

    fun toMap(): Map<String, String?> =
        mapOf(
            "search" to search,
            "page" to page.toString(),
            "sortColumn" to sortColumn.urlParam,
            "sortDirection" to sortDirection.name,
            "suoritusalku" to suoritusalku?.toString(),
            "suoritusloppu" to suoritusloppu?.toString(),
            "testikieli" to testikieli?.toString(),
            "piilotaHenkilotiedot" to piilotaHenkilotiedot.toTrueOrNull(),
        )

    fun excludeTags(): Set<ColumnTag> =
        setOfNotNull(
            if (piilotaHenkilotiedot) ColumnTag.PERSONAL_DATA else null,
        )

    fun csvFileName() =
        listOfNotNull(
            "yki_suoritukset",
            if (piilotaHenkilotiedot) null else "henkilotiedot",
            testikieli?.toString(),
            suoritusalku?.toString(),
            suoritusloppu?.toString(),
        ).joinToString("_", postfix = ".csv")

    fun filterDescriptions(): List<String> =
        listOfNotNull(
            if (suoritusalku != null || suoritusloppu != null) {
                listOf(
                    suoritusalku?.finnishDate().orEmpty(),
                    suoritusloppu?.finnishDate().orEmpty(),
                ).joinToString("-", prefix = "Aikarajaus: ")
            } else {
                null
            },
            testikieli?.let { "Testikieli: $it" },
            if (piilotaHenkilotiedot) "Henkilötiedot piilotettu" else null,
        )
}
