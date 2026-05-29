package fi.oph.kitu.webmvc

import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@RestController
class HomeController(
    private val dashboardService: DashboardService,
) {
    @GetMapping("/", produces = ["text/html"])
    fun home(): ResponseEntity<String> = ResponseEntity.ok(HomePage.render())

    @GetMapping("/dashboard/yki", produces = ["text/html"])
    fun ykiCard(): ResponseEntity<String> =
        cachedFragment(HomePage.renderYkiCardContent(dashboardService.getYkiStats()))

    @GetMapping("/dashboard/vkt", produces = ["text/html"])
    fun vktCard(): ResponseEntity<String> =
        cachedFragment(HomePage.renderVktCardContent(dashboardService.getVktStats()))

    @GetMapping("/dashboard/koto", produces = ["text/html"])
    fun kotoCard(): ResponseEntity<String> =
        cachedFragment(HomePage.renderKotoCardContent(dashboardService.getKotoStats()))

    private fun cachedFragment(body: String): ResponseEntity<String> =
        ResponseEntity
            .ok()
            .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).mustRevalidate())
            .body(body)
}
