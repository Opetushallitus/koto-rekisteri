package fi.oph.kitu

import fi.oph.kitu.html.Navigation.mainNavigation
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.testId
import jakarta.servlet.RequestDispatcher
import jakarta.servlet.http.HttpServletRequest
import kotlinx.html.a
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.section
import kotlinx.html.ul
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
class HomeController {
    @GetMapping("/", produces = ["text/html"])
    @ResponseBody
    fun home(): String =
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

    @GetMapping("/error", produces = ["text/html"])
    @ResponseBody
    fun error(request: HttpServletRequest): String =
        Page.renderHtml(breadcrumbs = emptyList()) {
            h1 { +"Kielitutkintorekisteri" }
            p {
                val status = HttpStatus.valueOf(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) as Int)
                +"${request.requestURL} ${status.value()} ${status.reasonPhrase}"
            }
        }
}
