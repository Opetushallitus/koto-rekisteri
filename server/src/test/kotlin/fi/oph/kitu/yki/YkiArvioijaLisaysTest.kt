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
    fun `hakulomake renderoityy hetuvalilehdella`() {
        val html = html(get("/yki/arvioijat/uusi").session(session()))

        assertContains(html, """data-testid="hetuHakuLomake"""")
        assertContains(html, """data-testid="hetu"""")
        assertContains(html, """data-testid="haeHenkilonTiedot"""")
        assertFalse(
            html.contains("""data-testid="oppijanumeroHakuLomake""""),
            "vain valittu valilehti saa renderoitya",
        )
    }

    @Test
    fun `oppijanumerovalilehti renderoi oman lomakkeensa`() {
        val html = html(get("/yki/arvioijat/uusi").session(session()).param("tapa", "OPPIJANUMERO"))

        assertContains(html, """data-testid="oppijanumeroHakuLomake"""")
        assertContains(html, """data-testid="oppijanumero"""")
        assertFalse(html.contains("""data-testid="hetuHakuLomake""""), "vain valittu valilehti saa renderoitya")
        assertFalse(html.contains("""data-testid="hetu-input""""), "hetukentta kuuluu vain toiselle valilehdelle")
    }

    @Test
    fun `valilehtilinkit kertovat kumpi on valittuna`() {
        val html = html(get("/yki/arvioijat/uusi").session(session()).param("tapa", "OPPIJANUMERO"))

        val valittu = Regex("""<a href="[^"]*tapa=OPPIJANUMERO"[^>]*aria-current="page"""")
        assertTrue(valittu.containsMatchIn(html), "valittu valilehti on merkittava aria-currentilla:\n$html")
        assertContains(html, """data-testid="hakutapa-HETU"""")
    }

    @Test
    fun `tyhja oppijanumero palauttaa oman valilehtensa virheineen`() {
        val html = haku("tapa" to "OPPIJANUMERO", "oppijanumero" to "")

        assertContains(html, "Oppijanumero on pakollinen tieto")
        assertContains(html, """data-testid="oppijanumeroHakuLomake"""")
        assertFalse(
            html.contains("Henkilötunnus on pakollinen tieto"),
            "toisen valilehden kenttia ei saa validoida",
        )
    }

    @Test
    fun `oppijanumerolla haettu henkilo esitaytetaan oppijanumerorekisterin tiedoilla`() {
        val html = haku("tapa" to "OPPIJANUMERO", "oppijanumero" to petronOid)

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
        assertFalse(
            kirjoittaja.contains("""aria-disabled="true""""),
            "kytkimen ollessa paalla lisaysnapin on oltava klikattavissa",
        )

        val lukija = html(get("/yki/arvioijat").session(session(Authority.VIRKAILIJA)))
        assertFalse(lukija.contains("""data-testid="lisaaArvioija""""), "lukijalle ei saa nayttaa lisaysnappia")
    }

    @Test
    fun `jo rekisterissa olevan arvioijan lomake esitaytetaan rekisterin merkinnalla`() {
        tallennaOlemassaolevaMerkinta()

        val html = haku("tapa" to "OPPIJANUMERO", "oppijanumero" to petronOid)

        assertContains(html, "Arvioija on jo rekisterissä")
        assertContains(html, """value="OPH-9-2020"""")
        assertTrue(valittuna(html, "FIN:PT"), "nykyisen merkinnan oikeudet on esitaytettava:\n$html")
        assertTrue(valittuna(html, "SWE:YT"), "muunkielinen oikeus ei saa kadota lomakkeelta:\n$html")
        assertFalse(valittuna(html, "ENG:YT"), "valitsematon oikeus ei saa nakya valittuna")
    }

    @Test
    fun `lisayslomakkeelta tallennus sailyttaa ensimmaisen rekisterointipaivan`() {
        tallennaOlemassaolevaMerkinta()

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
                    .param("kaudenAlkupaiva", "2026-03-01")
                    .param("arviointioikeus", "FIN:PT")
                    .param("arviointioikeus", "SWE:YT"),
            ).andExpect(status().isSeeOther)

        val tallennettu = repository.findByArvioijaOid(Oid.parse(petronOid).getOrThrow())!!
        val oikeudet = tallennettu.arviointioikeudet.associateBy { it.kieli }

        assertEquals(setOf(Tutkintokieli.FIN, Tutkintokieli.SWE), oikeudet.keys)
        oikeudet.values.forEach { oikeus ->
            assertEquals(LocalDate.of(2026, 3, 1), oikeus.kaudenAlkupaiva, "uusi kausi alkaa lomakkeen paivasta")
            assertEquals(
                LocalDate.of(2020, 1, 1),
                oikeus.ensimmainenRekisterointipaiva,
                "ensimmainen rekisterointipaiva on merkinnan historiaa, sita ei saa nollata",
            )
        }
    }

    private fun tallennaOlemassaolevaMerkinta() {
        repository.tallenna(
            YkiArvioijaEntity(
                id = null,
                arvioijaOid = Oid.parse(petronOid).getOrThrow(),
                henkilotunnus = null,
                sukunimi = "Kivinen-Testi",
                etunimet = "Petro Testi",
                sahkopostiosoite = "kivinen-testi@oph.fi",
                katuosoite = "Kivinenkatu 2 A 3",
                postinumero = "00100",
                postitoimipaikka = "HELSINKI",
                ashaNumero = "OPH-9-2020",
                arviointioikeudet =
                    listOf(
                        arviointioikeus(Tutkintokieli.FIN, setOf(Tutkintotaso.PT, Tutkintotaso.KT)),
                        arviointioikeus(Tutkintokieli.SWE, setOf(Tutkintotaso.YT)),
                    ),
            ),
        )
    }

    private fun arviointioikeus(
        kieli: Tutkintokieli,
        tasot: Set<Tutkintotaso>,
    ) = YkiArviointioikeusEntity(
        id = null,
        arvioijaId = null,
        kieli = kieli,
        tasot = tasot,
        tila = YkiArvioijaTila.AKTIIVINEN,
        kaudenAlkupaiva = LocalDate.of(2020, 1, 1),
        kaudenPaattymispaiva = LocalDate.of(2025, 1, 1),
        jatkorekisterointi = false,
        ensimmainenRekisterointipaiva = LocalDate.of(2020, 1, 1),
        rekisteriintuontiaika = null,
    )

    private fun valittuna(
        html: String,
        arvo: String,
    ): Boolean =
        Regex("""<input[^>]*data-testid="arviointioikeus-$arvo"[^>]*>""")
            .find(html)
            ?.value
            ?.contains("checked") == true

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

    private fun html(request: org.springframework.test.web.servlet.RequestBuilder): String {
        val result = mockMvc.perform(request).andReturn()
        assertEquals(200, result.response.status, "poikkeus: ${result.resolvedException}")
        return result.response.contentAsString
    }

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
