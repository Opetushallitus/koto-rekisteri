package fi.oph.kitu.webmvc

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.security.Authority
import fi.oph.kitu.security.cas.CasUserDetails
import fi.oph.kitu.util.result.getOrThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import kotlin.test.assertContains

@SpringBootTest
@Import(DBContainerConfiguration::class)
class HomeControllerTest(
    @param:Autowired private val context: WebApplicationContext,
) {
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
    }

    @Test
    fun `etusivu renderoityy ja sisaltaa nelja sektiokorttia`() {
        val response =
            mockMvc
                .perform(get("/").session(virkailijaSession()))
                .andExpect(status().isOk)
                .andReturn()
                .response.contentAsString

        assertContains(response, "Kielitutkintorekisteri")
        assertContains(response, """data-testid="dashboard"""")
        assertContains(response, """data-testid="yki-links"""")
        assertContains(response, """data-testid="vkt-links"""")
        assertContains(response, """data-testid="koto-kielitesti-links"""")
        assertContains(response, """data-testid="admin-links"""")
    }

    @Test
    fun `etusivulla on sektion otsikot`() {
        val response =
            mockMvc
                .perform(get("/").session(virkailijaSession()))
                .andExpect(status().isOk)
                .andReturn()
                .response.contentAsString

        assertContains(response, "Yleinen kielitutkinto")
        assertContains(response, "Valtionhallinnon kielitutkinto")
        assertContains(response, "Kotoutumiskoulutuksen kielitaidon päättötesti")
        assertContains(response, "Ylläpito")
    }

    @Test
    fun `etusivu sisaltaa linkit alasivuille`() {
        val response =
            mockMvc
                .perform(get("/").session(virkailijaSession()))
                .andExpect(status().isOk)
                .andReturn()
                .response.contentAsString

        assertContains(response, "Suoritukset")
        assertContains(response, "Arvioijat")
        assertContains(response, "Tarkistusarvioinnit")
        assertContains(response, "Tehtäväpaketit")
        assertContains(response, "Eräajojen hallinta")
        assertContains(response, "Erinomaisen taidon ilmoittautuneet")
        assertContains(response, "Hyvän ja tyydyttävän taidon suoritukset")
    }

    @Test
    fun `etusivu nayttaa viivan kun viimeisinta saapunutta suoritusta ei ole`() {
        val response =
            mockMvc
                .perform(get("/").session(virkailijaSession()))
                .andExpect(status().isOk)
                .andReturn()
                .response.contentAsString

        assertContains(response, "Viimeisin saapunut suoritus")
        assertContains(response, "—")
    }

    private fun virkailijaSession(): MockHttpSession {
        val principal =
            CasUserDetails(
                name = "test-virkailija",
                oid = Oid.parse("1.2.246.562.24.20281155246").getOrThrow(),
                strongAuth = false,
                kayttajaTyyppi = "VIRKAILIJA",
                authorities = listOf(SimpleGrantedAuthority(Authority.VIRKAILIJA.role())),
            )
        val authentication: Authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        val securityContext =
            SecurityContextHolder.createEmptyContext().apply {
                this.authentication = authentication
            }
        return MockHttpSession().also { session ->
            session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext,
            )
        }
    }
}
