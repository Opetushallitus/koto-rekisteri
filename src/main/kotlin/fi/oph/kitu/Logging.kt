package fi.oph.kitu

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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
                val result = filterChain.doFilter(request, response).runCatching { }
                val end = System.currentTimeMillis()

                result.fold(
                    onSuccess = {
                        logger.info("${request.method} ${request.requestURI}: duration=${end - start}ms status_code=${response.status}")
                    },
                    onFailure = fun (err) {
                        logger.info("${request.method} ${request.requestURI}: duration=${end - start}ms exception=${err.message}")
                        throw err
                    },
                )
            }
        }
}
