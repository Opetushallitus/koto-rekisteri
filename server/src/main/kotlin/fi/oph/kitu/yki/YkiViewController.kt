package fi.oph.kitu.yki

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView
import java.net.URLEncoder
import kotlin.math.ceil

data class HeaderCell(
    val search: String,
    val includeVersionHistory: String,
    val sortColumn: String,
    val sortDirection: String,
    val columnName: String,
)

@Controller
@RequestMapping("yki")
class YkiViewController(
    private val ykiService: YkiService,
) {
    fun reverseSortDirection(sortDirection: String) = if (sortDirection == "ASC") "DESC" else "ASC"

    fun generateHeader(
        search: String,
        currentColumn: String,
        currentDirection: String,
        versionHistory: Boolean,
    ): List<HeaderCell> =
        listOf(
            // first:sortColumn - the value that is used to sort columns
            // second:columnName  - the visible value in the header
            Pair("suorittajan_oid", "Oppijanumero"),
            Pair("sukunimi", "Sukunimi"),
            Pair("etunimet", "Etunimi"),
            Pair("sukupuoli", "Sukupuoli"),
            Pair("hetu", "Henkilötunnus"),
            Pair("kansalaisuus", "Kansalaisuus"),
            Pair("katuosoite", "Osoite"),
            Pair("email", "Sähköposti"),
            Pair("suoritus_id", "Suorituksen tunniste"),
            Pair("tutkintopaiva", "Tutkintopäivä"),
            Pair("tutkintokieli", "Tutkintokieli"),
            Pair("tutkintotaso", "Tutkintotaso"),
            Pair("jarjestajan_tunnus_oid", "Järjestäjän OID"),
            Pair("jarjestajan_nimi", "Järjestäjän nimi"),
            Pair("arviointipaiva", "Arviointipäivä"),
            Pair("tekstin_ymmartaminen", "Tekstin ymmärtäminen"),
            Pair("kirjoittaminen", "Kirjoittaminen"),
            Pair("rakenteet_ja_sanasto", "Rakenteet ja sanasto"),
            Pair("puheen_ymmartaminen", "Puheen ymmärtämine"),
            Pair("puhuminen", "Puhuminen"),
            Pair("yleisarvosana", "Yleisarvosana"),
        ).map { (sortColumn, columnName) ->
            HeaderCell(
                "search=$search",
                "includeVersionHistory=$versionHistory",
                "sortColumn=$sortColumn",
                "sortDirection=" +
                    if (currentColumn == sortColumn) reverseSortDirection(currentDirection) else currentDirection,
                columnName,
            )
        }

    @GetMapping("/suoritukset", produces = ["text/html"])
    fun suorituksetView(
        search: String = "",
        versionHistory: Boolean = false,
        limit: Int = 100,
        page: Int = 1,
        sortColumn: String = "tutkintopaiva",
        sortDirection: String = "DESC",
    ): ModelAndView {
        val suorituksetTotal = ykiService.countSuoritukset(search, versionHistory)
        val totalPages = ceil(suorituksetTotal.toDouble() / limit).toInt()
        val offset = limit * (page - 1)
        val nextPage = if (page >= totalPages) null else page + 1
        val previousPage = if (page <= 1) null else page - 1
        val searchStrUrl = URLEncoder.encode(search, Charsets.UTF_8)
        val paging =
            mapOf(
                "totalEntries" to suorituksetTotal,
                "currentPage" to page,
                "nextPage" to nextPage,
                "previousPage" to previousPage,
                "totalPages" to totalPages,
                "searchStr" to search,
                "searchStrUrl" to searchStrUrl,
            )

        return ModelAndView("yki-suoritukset")
            .addObject(
                "suoritukset",
                ykiService.findSuorituksetPaged(
                    search,
                    sortColumn,
                    sortDirection,
                    versionHistory,
                    limit,
                    offset,
                ),
            ).addObject("header", generateHeader(searchStrUrl, sortColumn, sortDirection, versionHistory))
            .addObject("sortColumn", sortColumn)
            .addObject("sortDirection", sortDirection)
            .addObject("paging", paging)
            .addObject("versionHistory", versionHistory)
    }

    @GetMapping("/arvioijat")
    fun arvioijatView(): ModelAndView =
        ModelAndView("yki-arvioijat")
            .addObject("arvioijat", ykiService.allArvioijat())
}
