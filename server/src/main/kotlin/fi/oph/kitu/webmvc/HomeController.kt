package fi.oph.kitu.webmvc

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HomeController(
    private val dashboardService: DashboardService,
) {
    @GetMapping("/", produces = ["text/html"])
    fun home(): ResponseEntity<String> = ResponseEntity.ok(HomePage.render())

    @GetMapping("/dashboard/yki", produces = ["text/html"])
    fun ykiCard(): ResponseEntity<String> =
        ResponseEntity.ok(HomePage.renderYkiCardContent(dashboardService.getYkiStats()))

    @GetMapping("/dashboard/vkt", produces = ["text/html"])
    fun vktCard(): ResponseEntity<String> =
        ResponseEntity.ok(HomePage.renderVktCardContent(dashboardService.getVktStats()))

    @GetMapping("/dashboard/koto", produces = ["text/html"])
    fun kotoCard(): ResponseEntity<String> =
        ResponseEntity.ok(HomePage.renderKotoCardContent(dashboardService.getKotoStats()))
}
