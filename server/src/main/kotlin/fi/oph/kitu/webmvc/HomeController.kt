package fi.oph.kitu.webmvc

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HomeController(
    private val dashboardService: DashboardService,
) {
    @GetMapping("/", produces = ["text/html"])
    fun home(): ResponseEntity<String> = ResponseEntity.ok(HomePage.render(dashboardService.getStats()))
}
