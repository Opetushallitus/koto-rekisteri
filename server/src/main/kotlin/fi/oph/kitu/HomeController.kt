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
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
class HomeController {
    @GetMapping("/", produces = ["text/html"])
    @ResponseBody
    fun home(): String = HomePage.render()
}

object HomePage {
    fun render(): String =
        Page.renderHtml(breadcrumbs = emptyList()) {
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
        }
}
