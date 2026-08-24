package fi.oph.kitu.security

import fi.oph.kitu.oid.Oid
import fi.oph.kitu.security.cas.CasUserDetails
import fi.oph.kitu.util.result.getOrThrow
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CurrentUserTest {
    private val oid = Oid.parse("1.2.246.562.24.20281155246").getOrThrow()

    @AfterEach
    fun clearContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `recognises a CAS role`() {
        authenticateWith(Authority.YKI_ARVIOIJAREKISTERI.role())

        assertTrue(CurrentUser.hasAuthority(Authority.YKI_ARVIOIJAREKISTERI))
        assertFalse(CurrentUser.hasAuthority(Authority.VKT_TALLENNUS))
    }

    @Test
    fun `recognises an OAuth2 scope role`() {
        authenticateWith(Authority.YKI_ARVIOIJAREKISTERI.scopeRole())

        assertTrue(CurrentUser.hasAuthority(Authority.YKI_ARVIOIJAREKISTERI))
    }

    @Test
    fun `a plain virkailija does not have the arvioijarekisteri authority`() {
        authenticateWith(Authority.VIRKAILIJA.role())

        assertTrue(CurrentUser.hasAuthority(Authority.VIRKAILIJA))
        assertFalse(CurrentUser.hasAuthority(Authority.YKI_ARVIOIJAREKISTERI))
    }

    @Test
    fun `returns the oid of the logged in virkailija`() {
        authenticateWith(Authority.VIRKAILIJA.role())

        assertEquals(oid, CurrentUser.oid())
    }

    @Test
    fun `returns no oid when the principal is not a CAS user`() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                "solki",
                null,
                listOf(SimpleGrantedAuthority(Authority.YKI_TALLENNUS.scopeRole())),
            )

        assertNull(CurrentUser.oid())
        assertTrue(CurrentUser.hasAuthority(Authority.YKI_TALLENNUS))
    }

    @Test
    fun `an unauthenticated request has no authorities and no oid`() {
        assertFalse(CurrentUser.hasAuthority(Authority.VIRKAILIJA))
        assertNull(CurrentUser.oid())
    }

    private fun authenticateWith(vararg roles: String) {
        val userDetails =
            CasUserDetails(
                name = "kitu_test",
                oid = oid,
                strongAuth = false,
                kayttajaTyyppi = "VIRKAILIJA",
                authorities = roles.map { SimpleGrantedAuthority(it) },
            )
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
    }
}
