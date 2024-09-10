package fi.oph.kitu.auth

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.security.cas.authentication.CasAssertionAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component

@Component
class CasUserDetailsService : AuthenticationUserDetailsService<CasAssertionAuthenticationToken> {
    override fun loadUserDetails(token: CasAssertionAuthenticationToken): UserDetails {
        val attributes = token.assertion.principal.attributes
        return CasUserDetails(
            token.name,
            attributes["oidHenkilo"] as String,
            attributes["idpEntityId"] == "vetuma",
            attributes["kayttajaTyyppi"] as String?,
            (attributes["roles"] as List<String>).map { SimpleGrantedAuthority(it) },
        )
    }
}

data class CasUserDetails(
    val name: String,
    val oid: String,
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
