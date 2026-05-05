package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.LocalStackContainerConfiguration
import fi.oph.kitu.LocalStackContainerConfiguration.Companion.TEST_BUCKET
import fi.oph.kitu.Oid
import fi.oph.kitu.auth.Authority
import fi.oph.kitu.auth.CasUserDetails
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
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import kotlin.test.assertContains
import kotlin.test.assertNotNull

@SpringBootTest
@Import(DBContainerConfiguration::class, LocalStackContainerConfiguration::class)
@TestPropertySource(properties = ["spring.cloud.aws.s3.enabled=true"])
class TehtavapankkiViewControllerTest(
    @param:Autowired private val context: WebApplicationContext,
    @param:Autowired private val s3Client: S3Client,
) {
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        s3Client
            .listObjectsV2 { it.bucket(TEST_BUCKET) }
            .contents()
            .forEach { obj ->
                s3Client.deleteObject { it.bucket(TEST_BUCKET).key(obj.key()) }
            }

        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
    }

    @Test
    fun `listView nayttaa bucketin tehtavapaketit`() {
        s3Client.putObject(
            { it.bucket(TEST_BUCKET).key("42-Suomi_alkeet/2026-01-01T00:00:00-0.xml") },
            RequestBody.fromString("<questions/>"),
        )
        s3Client.putObject(
            { it.bucket(TEST_BUCKET).key("7-Suomi_2/2026-02-02T00:00:00-0.xml") },
            RequestBody.fromString("<questions/>"),
        )

        val response =
            mockMvc
                .perform(get("/koto-tehtavapankki").session(virkailijaSession()))
                .andExpect(status().isOk)
                .andReturn()
                .response.contentAsString

        assertContains(response, "42-Suomi_alkeet")
        assertContains(response, "7-Suomi_2")
    }

    @Test
    fun `listView nayttaa tyhjan listan kun bucketissa ei ole objekteja`() {
        val response =
            mockMvc
                .perform(get("/koto-tehtavapankki").session(virkailijaSession()))
                .andExpect(status().isOk)
                .andReturn()
                .response.contentAsString

        assertContains(response, "Ei tehtäväpaketteja.")
    }

    @Test
    fun `downloadRedirect ohjaa signed URLiin`() {
        val key = "42-Suomi_alkeet/2026-01-01T00:00:00-0.xml"
        s3Client.putObject(
            { it.bucket(TEST_BUCKET).key(key) },
            RequestBody.fromString("<questions/>"),
        )

        val location =
            mockMvc
                .perform(get("/koto-tehtavapankki/lataa").param("key", key).session(virkailijaSession()))
                .andExpect(status().isFound)
                .andExpect(header().exists("Location"))
                .andReturn()
                .response
                .getHeader("Location")

        assertNotNull(location)
        assertContains(location, TEST_BUCKET)
    }

    @Test
    fun `downloadRedirect palauttaa 404 kun avainta ei loydy`() {
        mockMvc
            .perform(get("/koto-tehtavapankki/lataa").param("key", "ei-olemassa.xml").session(virkailijaSession()))
            .andExpect(status().isNotFound)
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
