package fi.oph.kitu.yki

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.security.Authority
import fi.oph.kitu.security.cas.CasUserDetails
import fi.oph.kitu.util.result.getOrThrow
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@Import(DBContainerConfiguration::class)
class YkiArvioijaLisaysTest(
    @param:Autowired private val context: WebApplicationContext,
    @param:Autowired private val repository: YkiArvioijaRepository,
) {
    private lateinit var mockMvc: MockMvc

    /** Ainoa mock-ONR:n henkilo, jolla on osoite ja sahkoposti. */
    private val petronOid = "1.2.246.562.24.59267607404"

    @BeforeEach
    fun setup() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
        repository.deleteAll()
    }

    @Test
    fun `hakulomake renderoityy`() {
        val html = html(get("/yki/arvioijat/uusi").session(session()))

        assertContains(html, """data-testid="hetu"""")
        assertContains(html, """data-testid="haeHenkilonTiedot"""")
    }

    @Test
    fun `oppijanumerolla haettu henkilo esitaytetaan oppijanumerorekisterin tiedoilla`() {
        val html = haku("oppijanumero" to petronOid)

        assertContains(html, """value="Kivinen-Testi"""")
        assertContains(html, """value="Petro Testi"""")
        assertContains(html, """value="kivinen-testi@oph.fi"""")
        assertContains(html, """value="Kivinenkatu 2 A 3"""")
        assertContains(html, """value="00100"""")
        assertContains(html, """value="HELSINKI"""")
        assertContains(html, """data-testid="arviointioikeusMatriisi"""")
    }

    @Test
    fun `hetulla ja nimilla haettu henkilo esitaytetaan`() {
        val html =
            haku(
                "hetu" to "010180-9026",
                "etunimet" to "Ranja Testi",
                "sukunimi" to "Öhman-Testi",
                "kutsumanimi" to "Ranja",
            )

        assertContains(html, """data-testid="tallennaArvioija"""")
        assertContains(html, "1.2.246.562.24.33342764709")
    }

    @Test
    fun `puuttuvat hakukentat palauttavat hakulomakkeen virheineen`() {
        val html = haku("hetu" to "")

        assertContains(html, "Henkilötunnus on pakollinen tieto")
        assertContains(html, "Etunimet on pakollinen tieto")
        assertContains(html, "Sukunimi on pakollinen tieto")
        assertContains(html, """aria-invalid="true"""")
        assertFalse(html.contains("""data-testid="tallennaArvioija""""), "ei saa edeta lomakkeen vaiheeseen 2")
    }

    @Test
    fun `tuntematon henkilo nayttaa yleisen virheen`() {
        val html =
            haku(
                "hetu" to "121280-123A",
                "etunimet" to "Tuntematon",
                "sukunimi" to "Testaaja",
            )

        assertContains(html, "oppijanumerorekisteristä")
    }

    @Test
    fun `arvioijan tallennus ohjaa tietosivulle ja kirjoittaa kannan rivin`() {
        val result =
            mockMvc
                .perform(
                    post("/yki/arvioijat/uusi")
                        .session(session())
                        .with(csrf())
                        .param("arvioijaOid", petronOid)
                        .param("sukunimi", "Kivinen-Testi")
                        .param("etunimet", "Petro Testi")
                        .param("sahkopostiosoite", "kivinen-testi@oph.fi")
                        .param("katuosoite", "Kivinenkatu 2 A 3")
                        .param("postinumero", "00100")
                        .param("postitoimipaikka", "HELSINKI")
                        .param("kaudenAlkupaiva", "2025-12-07")
                        .param("ashaNumero", "OPH-1234-2025")
                        .param("arviointioikeus", "FIN:PT")
                        .param("arviointioikeus", "FIN:KT")
                        .param("arviointioikeus", "SWE:YT"),
                ).andExpect(status().isSeeOther)
                .andReturn()

        val tallennettu = repository.findByArvioijaOid(Oid.parse(petronOid).getOrThrow())
        assertNotNull(tallennettu)
        assertNull(tallennettu.henkilotunnus, "hetua ei saa tallentaa")
        assertEquals("OPH-1234-2025", tallennettu.ashaNumero)
        assertContains(
            result.response.getHeader("Location").orEmpty(),
            "/yki/arvioijat/${tallennettu.id!!.toInt()}",
        )

        val oikeudet = tallennettu.arviointioikeudet.associateBy { it.kieli }
        assertEquals(setOf(Tutkintokieli.FIN, Tutkintokieli.SWE), oikeudet.keys)
        assertEquals(setOf(Tutkintotaso.PT, Tutkintotaso.KT), oikeudet[Tutkintokieli.FIN]!!.tasot)
        assertEquals(setOf(Tutkintotaso.YT), oikeudet[Tutkintokieli.SWE]!!.tasot)

        oikeudet.values.forEach { oikeus ->
            assertEquals(LocalDate.of(2025, 12, 7), oikeus.kaudenAlkupaiva, "sama kausi kaikille kielille")
            assertEquals(LocalDate.of(2030, 12, 7), oikeus.kaudenPaattymispaiva)
        }
    }

    @Test
    fun `puuttuva arviointioikeus palauttaa lomakkeen eivatka syotteet katoa`() {
        val html =
            html(
                post("/yki/arvioijat/uusi")
                    .session(session())
                    .with(csrf())
                    .param("arvioijaOid", petronOid)
                    .param("sukunimi", "Kivinen-Testi")
                    .param("etunimet", "Petro Testi")
                    .param("katuosoite", "Kivinenkatu 2 A 3")
                    .param("postinumero", "00100")
                    .param("postitoimipaikka", "HELSINKI")
                    .param("kaudenAlkupaiva", "2025-12-07"),
            )

        assertContains(html, "Valitse vähintään yksi tutkintokieli ja tutkintotaso")
        assertContains(html, """value="Kivinen-Testi"""")
        assertContains(html, """value="Kivinenkatu 2 A 3"""")
        assertNull(repository.findByArvioijaOid(Oid.parse(petronOid).getOrThrow()))
    }

    @Test
    fun `virheellinen postinumero merkitaan kenttaan`() {
        val html =
            html(
                post("/yki/arvioijat/uusi")
                    .session(session())
                    .with(csrf())
                    .param("arvioijaOid", petronOid)
                    .param("sukunimi", "Kivinen-Testi")
                    .param("etunimet", "Petro Testi")
                    .param("katuosoite", "Kivinenkatu 2 A 3")
                    .param("postinumero", "1")
                    .param("postitoimipaikka", "HELSINKI")
                    .param("kaudenAlkupaiva", "2025-12-07")
                    .param("arviointioikeus", "FIN:PT"),
            )

        assertContains(html, "Postinumeron on oltava viisi numeroa")
        assertContains(html, """data-testid="postinumero-error"""")
    }

    @Test
    fun `lisaysnappi nakyy vain kirjoitusoikeudella`() {
        val kirjoittaja = html(get("/yki/arvioijat").session(session()))
        assertContains(kirjoittaja, """data-testid="lisaaArvioija"""")

        val lukija = html(get("/yki/arvioijat").session(session(Authority.VIRKAILIJA)))
        assertFalse(lukija.contains("""data-testid="lisaaArvioija""""), "lukijalle ei saa nayttaa lisaysnappia")
    }

    @Test
    fun `tietosivu nayttaa arvioijan tiedot`() {
        mockMvc
            .perform(
                post("/yki/arvioijat/uusi")
                    .session(session())
                    .with(csrf())
                    .param("arvioijaOid", petronOid)
                    .param("sukunimi", "Kivinen-Testi")
                    .param("etunimet", "Petro Testi")
                    .param("katuosoite", "Kivinenkatu 2 A 3")
                    .param("postinumero", "00100")
                    .param("postitoimipaikka", "HELSINKI")
                    .param("kaudenAlkupaiva", "2025-12-07")
                    .param("arviointioikeus", "FIN:PT"),
            ).andExpect(status().isSeeOther)

        val id = repository.findByArvioijaOid(Oid.parse(petronOid).getOrThrow())!!.id!!.toInt()
        val html = html(get("/yki/arvioijat/$id").session(session()))

        assertContains(html, "Petro Testi Kivinen-Testi")
        assertContains(html, petronOid)
        assertContains(html, "Perustaso")
    }

    @Test
    fun `tuntematon arvioija palauttaa 404`() {
        mockMvc
            .perform(get("/yki/arvioijat/999999").session(session()))
            .andExpect(status().isNotFound)
    }

    private fun haku(vararg params: Pair<String, String>): String =
        html(
            post("/yki/arvioijat/uusi/haku").session(session()).with(csrf()).apply {
                params.forEach { (name, value) -> param(name, value) }
            },
        )

    private fun html(request: org.springframework.test.web.servlet.RequestBuilder): String =
        mockMvc
            .perform(request)
            .andExpect(status().isOk)
            .andReturn()
            .let(MvcResult::getResponse)
            .contentAsString

    private fun session(
        vararg authorities: Authority = arrayOf(Authority.VIRKAILIJA, Authority.YKI_ARVIOIJAREKISTERI),
    ): MockHttpSession {
        val principal =
            CasUserDetails(
                name = "test-virkailija",
                oid = Oid.parse("1.2.246.562.24.20281155246").getOrThrow(),
                strongAuth = false,
                kayttajaTyyppi = "VIRKAILIJA",
                asiointikieli = null,
                authorities = authorities.map { SimpleGrantedAuthority(it.role()) },
            )
        val authentication: Authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        val securityContext =
            SecurityContextHolder.createEmptyContext().apply { this.authentication = authentication }
        return MockHttpSession().also { session ->
            session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext,
            )
        }
    }
}
