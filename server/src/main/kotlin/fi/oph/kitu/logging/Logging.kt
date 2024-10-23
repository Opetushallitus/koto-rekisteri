package fi.oph.kitu.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.spi.LoggingEventBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.OncePerRequestFilter

@Configuration
class Logging {
    @Bean
    fun logFilter() =
        object : OncePerRequestFilter() {
            override fun doFilterInternal(
                request: HttpServletRequest,
                response: HttpServletResponse,
                filterChain: FilterChain,
            ) {
                val start = System.currentTimeMillis()
                val result = runCatching { filterChain.doFilter(request, response) }
                val end = System.currentTimeMillis()

                val prefix = "${request.method} ${request.requestURI}: "

                result.fold(
                    onSuccess = {
                        logHttpRequest(prefix, listOf("duration=${end - start}ms", "status_code=${response.status}"))
                    },
                    onFailure = { err ->
                        logHttpRequest(prefix, listOf("duration=${end - start}ms", "exception=${err.message}"))
                        throw err
                    },
                )
            }

            private fun logHttpRequest(
                prefix: String,
                fields: List<String>,
            ) {
                logger.info(
                    fields.joinToString(
                        separator = ", ",
                        prefix = prefix,
                    ),
                )
            }
        }
}

fun LoggingEventBuilder.add(vararg pairs: Pair<String, Any?>): LoggingEventBuilder {
    for ((key, value) in pairs) {
        addKeyValue(key, value)
    }
    return this
}
