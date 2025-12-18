package fi.oph.kitu

import fi.oph.kitu.html.Navigation.mainNavigation
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.testId
import kotlinx.html.a
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.li
import kotlinx.html.section
import kotlinx.html.ul
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HomeController {
    @GetMapping("/", produces = ["text/html"])
    fun home(): ResponseEntity<String> =
        ResponseEntity.ok(
            Page.renderHtml {
                h1 { +"Kielitutkintorekisteri" }
                mainNavigation.forEach { group ->
                    section {
                        testId("${group.id}-links")
                        h2 { +group.name }
                        ul {
                            group.children.forEach { child ->
                                li { a(href = child.ref) { +child.title } }
                            }
                        }
                    }
                }
            },
        )
}
