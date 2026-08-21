package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.html.Pagination
import fi.oph.kitu.html.table.httpParams
import fi.oph.kitu.webmvc.Links
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/yki/arvioijat")
class YkiArvioijaViewController(
    private val arvioijaService: YkiArvioijaService,
) {
    @GetMapping("", produces = ["text/html"])
    fun arvioijatView(
        @ModelAttribute params: YkiArvioijaParams,
    ): ResponseEntity<String> {
        val arvioijat = arvioijaService.haeSivullinen(params)
        val kokonaismaara = arvioijaService.laske(params)

        return ResponseEntity.ok(
            YkiArvioijaPage.render(
                arvioijat = arvioijat,
                params = params,
                pagination =
                    Pagination.valueOf(
                        currentPageNumber = params.page,
                        numberOfRows = kokonaismaara,
                        pageSize = params.limit,
                        url = { page ->
                            Links.Yki.arvioijat() + httpParams(params.toMap() + ("page" to page.toString()))
                        },
                    ),
            ),
        )
    }
}
