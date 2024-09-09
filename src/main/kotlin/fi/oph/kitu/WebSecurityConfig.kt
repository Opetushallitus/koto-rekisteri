package fi.oph.kitu

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class WebSecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf -> csrf.ignoringRequestMatchers("/api/*") }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("api/health/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }.httpBasic(Customizer.withDefaults())
            .formLogin(Customizer.withDefaults())

        return http.build()
    }
}
