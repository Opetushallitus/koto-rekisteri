package fi.oph.kitu.html

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

object KituRequest {
    fun currentCsrfToken(): CsrfToken = current().getAttribute("_csrf") as CsrfToken

    fun current(): HttpServletRequest =
        (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
}
