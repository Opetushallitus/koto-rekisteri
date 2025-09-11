package fi.oph.kitu.auth

import com.fasterxml.jackson.annotation.JsonIgnore
import fi.oph.kitu.Oid
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.core.MethodParameter
import org.springframework.security.cas.authentication.CasAssertionAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Component
class CasUserDetailsService : AuthenticationUserDetailsService<CasAssertionAuthenticationToken> {
    @Value("\${kitu.kayttaja.organisaatioOid}")
    lateinit var kayttajanOrganisaatioOidString: String

    override fun loadUserDetails(token: CasAssertionAuthenticationToken): UserDetails {
        val attributes = token.assertion.principal.attributes
        val oid = Oid.parseTyped(attributes["oidHenkilo"] as String).getOrThrow()
        val kayttajanOrganisaatioOid = Oid.parseTyped(kayttajanOrganisaatioOidString).getOrThrow()

        return CasUserDetails(
            token.name,
            oid,
            kayttajanOrganisaatioOid,
            attributes["idpEntityId"] == "vetuma",
            attributes["kayttajaTyyppi"] as String?,
            (attributes["roles"] as List<String>).map { SimpleGrantedAuthority(it) },
        )
    }
}

data class CasUserDetails(
    val name: String,
    val oid: Oid,
    val kayttajanOrganisaatioOid: Oid,
    val strongAuth: Boolean,
    val kayttajaTyyppi: String?,
    private val authorities: List<SimpleGrantedAuthority>,
) : UserDetails {
    override fun getAuthorities(): List<GrantedAuthority> = authorities

    @JsonIgnore override fun getPassword(): String? = null

    override fun getUsername(): String = name

    @JsonIgnore override fun isAccountNonExpired(): Boolean = true

    @JsonIgnore override fun isAccountNonLocked(): Boolean = true

    @JsonIgnore override fun isCredentialsNonExpired(): Boolean = true

    @JsonIgnore override fun isEnabled(): Boolean = true
}

@Component
class CasUserDetailsResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.parameterType == CasUserDetails::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: org.springframework.web.bind.support.WebDataBinderFactory?,
    ): Any? = SecurityContextHolder.getContext().authentication?.principal as? CasUserDetails
}

@Configuration
class CasUserDetailConfig(
    private val casUserDetailsResolver: CasUserDetailsResolver,
) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(casUserDetailsResolver)
    }
}
