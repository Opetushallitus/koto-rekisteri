package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/koto-tehtavapankki", produces = ["text/html"])
@ConditionalOnProperty(
    name = ["spring.cloud.aws.s3.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class TehtavapankkiViewController(
    private val tehtavapankkiService: TehtavapankkiService,
) {
    @GetMapping("")
    fun listView(): ResponseEntity<String> {
        val tehtavapaketit =
            tehtavapankkiService
                .listTehtavapaketit()
                .sortedByDescending { it.timestamp }
        return ResponseEntity.ok(TehtavapankkiPage.render(tehtavapaketit))
    }

    @WithSpan
    @GetMapping("/lataa")
    fun downloadRedirect(
        @RequestParam key: String,
    ): ResponseEntity<Void> {
        val url = tehtavapankkiService.getTemporaryDownloadUrl(key) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.status(HttpStatus.FOUND).location(url.toURI()).build()
    }
}
