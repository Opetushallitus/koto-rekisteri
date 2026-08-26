package fi.oph.kitu.yki

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.security.Authority
import fi.oph.kitu.security.cas.CasUserDetails
import fi.oph.kitu.util.result.getOrThrow
import fi.oph.kitu.yki.arvioijat.YkiArvioijaEntity
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.arvioijat.YkiArvioijaTila
import fi.oph.kitu.yki.arvioijat.YkiArviointioikeusEntity
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
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@Import(DBContainerConfiguration::class)
class YkiArvioijaMuokkausTest(
    @param:Autowired private val context: WebApplicationContext,
    @param:Autowired private val repository: YkiArvioijaRepository,
) {
    private lateinit var mockMvc: MockMvc

    private val petro = "1.2.246.562.24.59267607404"
    private val ranja = "1.2.246.562.24.20281155246"

    @BeforeEach
    fun setup() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()

        repository.deleteAll()
        repository.tallenna(arvioija(petro, "Kivinen-Testi", "Petro Testi"))
        repository.tallenna(arvioija(ranja, "Öhman-Testi", "Ranja Testi"))
    }

    @Test
    fun `tietosivulla on muokkausnappi vain kirjoitusoikeudella`() {
        val kirjoittaja = html(get("/yki/arvioijat/${idOf(petro)}").session(session()))
        assertContains(kirjoittaja, """data-testid="muokkaaArvioijaa"""")

        val lukija = html(get("/yki/arvioijat/${idOf(petro)}").session(session(Authority.VIRKAILIJA)))
        assertFalse(lukija.contains("""data-testid="muokkaaArvioijaa""""), "lukijalle ei nayteta muokkausnappia")
    }

    @Test
    fun `muokkauslomake on esitaytetty arvioijan tiedoilla`() {
        val html = html(get("/yki/arvioijat/${idOf(petro)}/muokkaa").session(session()))

        assertContains(html, """value="Kivinen-Testi"""")
        assertContains(html, """value="Testikuja 5"""")
        assertContains(html, """data-testid="arviointioikeus-FIN:PT"""")
        assertContains(html, """data-testid="tallennaArvioija"""")
        assertContains(html, """data-testid="peruutaMuokkaus"""")
    }

    @Test
    fun `muokkauslomake postaa tallennusosoitteeseen eika muokkausosoitteeseen`() {
        val id = idOf(petro)

        val html = html(get("/yki/arvioijat/$id/muokkaa").session(session()))

        val action = Regex("""<form[^>]*action="([^"]*)"""").find(html)?.groupValues?.get(1)
        assertContains(action.orEmpty(), "/yki/arvioijat/$id")
        assertFalse(action.orEmpty().endsWith("/muokkaa"), "muokkausosoite vastaa vain GETtiin")
    }

    @Test
    fun `muokkauslomake sailyttaa valitut arviointioikeudet valittuina`() {
        val html = html(get("/yki/arvioijat/${idOf(petro)}/muokkaa").session(session()))

        val valittu = Regex("""data-testid="arviointioikeus-FIN:PT"[^>]*checked""")
        assertTrue(valittu.containsMatchIn(html), "tallennetun oikeuden on oltava valittuna:\n$html")
    }

    @Test
    fun `muutosten tallennus ohjaa tietosivulle ja paivittaa kannan`() {
        val id = idOf(petro)

        val result =
            mockMvc
                .perform(
                    muokkaus(id, petro)
                        .param("postitoimipaikka", "TAMPERE")
                        .param("arviointioikeus", "FIN:PT"),
                ).andReturn()

        assertEquals(303, result.response.status, "poikkeus: ${result.resolvedException}")
        assertContains(result.response.getHeader("Location").orEmpty(), "/yki/arvioijat/$id")

        val paivitetty = repository.findArvioijaById(id)!!
        assertEquals("TAMPERE", paivitetty.postitoimipaikka)
        assertEquals("Muokattu Sukunimi", paivitetty.sukunimi)
    }

    @Test
    fun `kielen poisto poistaa arviointioikeuden`() {
        val id = idOf(petro)

        mockMvc
            .perform(
                muokkaus(id, petro)
                    .param("postitoimipaikka", "Testilä")
                    .param("arviointioikeus", "SWE:YT"),
            ).andReturn()

        val oikeudet = repository.findArvioijaById(id)!!.arviointioikeudet
        assertEquals(setOf(Tutkintokieli.SWE), oikeudet.map { it.kieli }.toSet(), "FIN-oikeuden on poistuttava")
    }

    @Test
    fun `virheellinen syote palauttaa lomakkeen eivatka arvot katoa`() {
        val id = idOf(petro)

        val html =
            html(
                muokkaus(id, petro)
                    .param("postitoimipaikka", "Testilä")
                    .param("postinumero", "1")
                    .param("arviointioikeus", "FIN:PT"),
            )

        assertContains(html, "Postinumeron on oltava viisi numeroa")
        assertContains(html, """value="Muokattu Sukunimi"""")
        assertEquals("Kivinen-Testi", repository.findArvioijaById(id)!!.sukunimi, "kantaa ei saa muuttaa")
    }

    @Test
    fun `piilokentan oid ei voi ohjata muutosta toiselle arvioijalle`() {
        val petronId = idOf(petro)
        val ranjanId = idOf(ranja)

        mockMvc
            .perform(
                // Lomake vaittaa muokkaavansa Ranjaa, mutta polku osoittaa Petroon.
                muokkaus(petronId, ranja)
                    .param("postitoimipaikka", "KAAPATTU")
                    .param("arviointioikeus", "FIN:PT"),
            ).andReturn()

        assertEquals("KAAPATTU", repository.findArvioijaById(petronId)!!.postitoimipaikka)
        assertEquals("Testilä", repository.findArvioijaById(ranjanId)!!.postitoimipaikka, "toista ei saa muuttaa")
        assertEquals("Öhman-Testi", repository.findArvioijaById(ranjanId)!!.sukunimi)
    }

    @Test
    fun `samanaikainen muokkaus ei ylikirjoita toisen muutoksia hiljaisesti`() {
        val id = idOf(petro)
        val lomake = html(get("/yki/arvioijat/$id/muokkaa").session(session()))
        val tunniste = piilokentanArvo(lomake, "muokattu")

        // Ensimmainen valilehti tallentaa ja siirtaa muokkaushetkea.
        val ensimmainen =
            mockMvc
                .perform(
                    post("/yki/arvioijat/$id")
                        .session(session())
                        .with(csrf())
                        .param("arvioijaOid", petro)
                        .param("muokattu", tunniste)
                        .param("sukunimi", "Ensimmainen")
                        .param("etunimet", "Petro Testi")
                        .param("katuosoite", "Testikuja 5")
                        .param("postinumero", "40100")
                        .param("postitoimipaikka", "HELSINKI")
                        .param("kaudenAlkupaiva", "2025-01-01")
                        .param("arviointioikeus", "FIN:PT"),
                ).andReturn()
        assertEquals(303, ensimmainen.response.status, "poikkeus: ${ensimmainen.resolvedException}")

        // Toinen valilehti tallentaa saman, nyt vanhentuneen tunnisteen kanssa.
        val virhesivu =
            html(
                post("/yki/arvioijat/$id")
                    .session(session())
                    .with(csrf())
                    .param("arvioijaOid", petro)
                    .param("muokattu", tunniste)
                    .param("sukunimi", "Toinen")
                    .param("etunimet", "Petro Testi")
                    .param("katuosoite", "Testikuja 5")
                    .param("postinumero", "40100")
                    .param("postitoimipaikka", "HELSINKI")
                    .param("kaudenAlkupaiva", "2025-01-01")
                    .param("arviointioikeus", "FIN:PT"),
            )

        assertContains(virhesivu, "Toinen käyttäjä ehti muokata")
        assertEquals(
            "Ensimmainen",
            repository.findArvioijaById(id)?.sukunimi,
            "ensimmaisen tallentajan muutos ei saa kadota",
        )
    }

    private fun piilokentanArvo(
        html: String,
        name: String,
    ): String {
        val kentta =
            Regex("""<input[^>]*name="$name"[^>]*>""").find(html)?.value
                ?: throw AssertionError("piilokenttaa $name ei loytynyt lomakkeelta:\n$html")
        return Regex("""value="([^"]*)"""").find(kentta)?.groupValues?.get(1)
            ?: throw AssertionError("piilokentalla $name ei ole arvoa: $kentta")
    }

    @Test
    fun `passivoitu arvioija ei aktivoidu yhteystietojen korjauksesta`() {
        val id = idOf(petro)
        passivoi(id)

        mockMvc
            .perform(
                muokkaus(id, petro)
                    .param("postitoimipaikka", "TAMPERE")
                    .param("arviointioikeus", "FIN:PT"),
            ).andReturn()

        val paivitetty = repository.findArvioijaById(id)!!
        assertEquals("TAMPERE", paivitetty.postitoimipaikka, "muutoksen on tallennuttava")
        assertEquals(
            listOf(YkiArvioijaTila.PASSIVOITU),
            paivitetty.arviointioikeudet.map { it.tila },
            "lomake ei kanna tilaa, joten sen on sailyttava ennallaan",
        )
    }

    private fun passivoi(id: Int) {
        val arvioija = repository.findArvioijaById(id)!!
        repository.tallenna(
            arvioija.copy(
                arviointioikeudet = arvioija.arviointioikeudet.map { it.copy(tila = YkiArvioijaTila.PASSIVOITU) },
            ),
        )
    }

    @Test
    fun `tuntemattoman arvioijan muokkaus palauttaa 404`() {
        mockMvc
            .perform(get("/yki/arvioijat/999999/muokkaa").session(session()))
            .andReturn()
            .let { assertEquals(404, it.response.status) }
    }

    private fun muokkaus(
        id: Int,
        lomakkeenOid: String,
    ) = post("/yki/arvioijat/$id")
        .session(session())
        .with(csrf())
        .param("arvioijaOid", lomakkeenOid)
        .param("sukunimi", "Muokattu Sukunimi")
        .param("etunimet", "Petro Testi")
        .param("katuosoite", "Testikuja 5")
        .param("postinumero", "40100")
        .param("kaudenAlkupaiva", "2025-01-01")

    private fun idOf(oid: String): Int = repository.findByArvioijaOid(Oid.parse(oid).getOrThrow())!!.id!!.toInt()

    private fun html(request: RequestBuilder): String {
        val result = mockMvc.perform(request).andReturn()
        assertEquals(200, result.response.status, "poikkeus: ${result.resolvedException}")
        return result.response.contentAsString
    }

    private fun arvioija(
        oid: String,
        sukunimi: String,
        etunimet: String,
    ) = YkiArvioijaEntity(
        id = null,
        arvioijaOid = Oid.parse(oid).getOrThrow(),
        henkilotunnus = null,
        sukunimi = sukunimi,
        etunimet = etunimet,
        sahkopostiosoite = "testi@testi.fi",
        katuosoite = "Testikuja 5",
        postinumero = "40100",
        postitoimipaikka = "Testilä",
        arviointioikeudet =
            listOf(
                YkiArviointioikeusEntity(
                    id = null,
                    arvioijaId = null,
                    kieli = Tutkintokieli.FIN,
                    tasot = setOf(Tutkintotaso.PT),
                    tila = YkiArvioijaTila.AKTIIVINEN,
                    kaudenAlkupaiva = LocalDate.of(2021, 1, 1),
                    kaudenPaattymispaiva = LocalDate.of(2026, 1, 1),
                    jatkorekisterointi = false,
                    ensimmainenRekisterointipaiva = LocalDate.of(2021, 1, 1),
                    rekisteriintuontiaika = null,
                ),
            ),
    )

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
