package fi.oph.kitu.auth

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter

/**
Converts a `roles` claim in a Jwt token to a Spring authentication token with Otuva `ROLE_APP_` style authorities.

Copied from [this Otuva example](https://wiki.eduuni.fi/spaces/OPHPALV/pages/522313521/Koneiden+v%C3%A4linen+tunnistus+ja+Oauth2-k%C3%A4ytt%C3%B6%C3%B6notto#Koneidenv%C3%A4linentunnistusjaOauth2k%C3%A4ytt%C3%B6%C3%B6notto-Esimerkkitoteutus.1).
*/
class JwtAuthenticationTokenConverter : Converter<Jwt, AbstractAuthenticationToken> {
    private val authoritiesConverter = JwtGrantedAuthoritiesConverter()

    override fun convert(source: Jwt): AbstractAuthenticationToken? {
        val authorities = extractRoles(source)
        val convertedAuthorities = authoritiesConverter.convert(source) ?: emptyList()
        return JwtAuthenticationToken(source, authorities + convertedAuthorities)
    }

    private fun extractRoles(jwt: Jwt): List<SimpleGrantedAuthority> {
        val roles = jwt.claims["roles"] as? Map<*, *>
        return roles
            ?.mapNotNull { (oid, _) -> oid as? String }
            ?.flatMap { oid ->
                (roles[oid] as? List<*>)
                    ?.mapNotNull { role -> role as? String }
                    ?.flatMap { role -> listOf("ROLE_APP_$role", "ROLE_APP_${role}_$oid") }
                    ?: emptyList()
            }?.map(::SimpleGrantedAuthority)
            ?: emptyList()
    }
}
