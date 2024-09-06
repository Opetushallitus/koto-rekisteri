package fi.oph.kitu

import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.AbstractRequestLoggingFilter

@Configuration
class Logging {
    @Bean
    fun logFilter() =
        object : AbstractRequestLoggingFilter() {
            init {
                isIncludeClientInfo = true
                isIncludeQueryString = true
            }

            override fun beforeRequest(
                request: HttpServletRequest,
                message: String,
            ) {
            }

            override fun afterRequest(
                request: HttpServletRequest,
                message: String,
            ) {
                servletContext.log("${request.method} ${request.requestURI}: $message")
            }
        }
}
