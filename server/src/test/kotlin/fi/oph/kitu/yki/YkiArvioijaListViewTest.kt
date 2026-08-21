package fi.oph.kitu.yki

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.html.table.ColumnTag
import fi.oph.kitu.html.table.RenderableDisplayTableEnum
import fi.oph.kitu.jdbc.SortDirection
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.util.result.getOrThrow
import fi.oph.kitu.yki.arvioijat.YkiArvioijaColumn
import fi.oph.kitu.yki.arvioijat.YkiArvioijaEntity
import fi.oph.kitu.yki.arvioijat.YkiArvioijaListRow
import fi.oph.kitu.yki.arvioijat.YkiArvioijaParams
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.arvioijat.YkiArvioijaTila
import fi.oph.kitu.yki.arvioijat.YkiArviointioikeusEntity
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@Import(DBContainerConfiguration::class)
class YkiArvioijaListViewTest(
    @param:Autowired private val postgres: PostgreSQLContainer,
    @param:Autowired private val repository: YkiArvioijaRepository,
) {
    @BeforeEach
    fun nukeDb() {
        repository.deleteAll()
        listOf(
            arvioija("1.2.246.562.24.20281155246", "Öhman-Testi", Tutkintokieli.FIN, YkiArvioijaTila.AKTIIVINEN),
            arvioija("1.2.246.562.24.59267607404", "Andersson-Testi", Tutkintokieli.SWE, YkiArvioijaTila.AKTIIVINEN),
            arvioija("1.2.246.562.24.74064782358", "Kivinen-Testi", Tutkintokieli.ENG, YkiArvioijaTila.PASSIVOITU),
        ).forEach { repository.tallenna(it) }
    }

    private fun arvioija(
        oid: String,
        sukunimi: String,
        kieli: Tutkintokieli,
        tila: YkiArvioijaTila,
    ) = YkiArvioijaEntity(
        id = null,
        arvioijaOid = Oid.parse(oid).getOrThrow(),
        henkilotunnus = null,
        sukunimi = sukunimi,
        etunimet = "Testi",
        sahkopostiosoite = "$sukunimi@testi.fi".lowercase(),
        katuosoite = "Testikuja 5",
        postinumero = "40100",
        postitoimipaikka = "Testilä",
        arviointioikeudet =
            listOf(
                YkiArviointioikeusEntity(
                    id = null,
                    arvioijaId = null,
                    kieli = kieli,
                    tasot = setOf(Tutkintotaso.PT, Tutkintotaso.KT),
                    tila = tila,
                    kaudenAlkupaiva = LocalDate.of(2021, 1, 1),
                    kaudenPaattymispaiva = LocalDate.of(2026, 1, 1),
                    jatkorekisterointi = false,
                    ensimmainenRekisterointipaiva = LocalDate.of(2021, 1, 1),
                    rekisteriintuontiaika = null,
                ),
            ),
    )

    @Test
    fun `listanäkymä palauttaa kaikki rivit oletusjärjestyksessä`() {
        val rows = repository.findForListView(YkiArvioijaParams())

        assertEquals(3, repository.countForListView(YkiArvioijaParams()))
        assertEquals(
            listOf("Andersson-Testi", "Kivinen-Testi", "Öhman-Testi"),
            rows.map { it.sukunimi },
        )
    }

    @Test
    fun `hakusana suodattaa nimen ja oppijanumeron perusteella`() {
        assertEquals(1, repository.findForListView(YkiArvioijaParams(search = "Kivinen")).size)
        assertEquals(1, repository.findForListView(YkiArvioijaParams(search = "59267607404")).size)
        assertEquals(0, repository.findForListView(YkiArvioijaParams(search = "ei-lC6ydy")).size)
    }

    @Test
    fun `monisanainen haku loytaa henkilon vaikka termit ovat eri sarakkeissa`() {
        // Sukunimi ja etunimi ovat eri sarakkeissa, joten yksi ILIKE ei riittaisi.
        assertEquals(
            listOf("Kivinen-Testi"),
            repository.findForListView(YkiArvioijaParams(search = "Kivinen Testi")).map { it.sukunimi },
        )
        // Jarjestyksella ei ole valia.
        assertEquals(
            listOf("Kivinen-Testi"),
            repository.findForListView(YkiArvioijaParams(search = "Testi Kivinen")).map { it.sukunimi },
        )
        // Kaikkien termien on osuttava.
        assertEquals(0, repository.findForListView(YkiArvioijaParams(search = "Kivinen Andersson")).size)
        // Ylimaarainen valilyonti ei riko hakua.
        assertEquals(1, repository.findForListView(YkiArvioijaParams(search = "  Kivinen   ")).size)
    }

    @Test
    fun `haku ei ole kirjainkokoriippuvainen`() {
        assertEquals(1, repository.findForListView(YkiArvioijaParams(search = "kivinen")).size)
        assertEquals(1, repository.findForListView(YkiArvioijaParams(search = "KIVINEN")).size)
    }

    @Test
    fun `tila-, kieli- ja tasosuodattimet rajaavat tuloksia`() {
        assertEquals(
            listOf("Kivinen-Testi"),
            repository.findForListView(YkiArvioijaParams(tila = YkiArvioijaTila.PASSIVOITU)).map { it.sukunimi },
        )
        assertEquals(
            listOf("Öhman-Testi"),
            repository.findForListView(YkiArvioijaParams(kieli = Tutkintokieli.FIN)).map { it.sukunimi },
        )
        assertEquals(3, repository.findForListView(YkiArvioijaParams(taso = Tutkintotaso.PT)).size)
        assertEquals(0, repository.findForListView(YkiArvioijaParams(taso = Tutkintotaso.YT)).size)
    }

    @Test
    fun `kausiPaattyyEnnen rajaa päättyvät kaudet`() {
        assertEquals(
            3,
            repository.findForListView(YkiArvioijaParams(kausiPaattyyEnnen = LocalDate.of(2027, 1, 1))).size,
        )
        assertEquals(
            0,
            repository.findForListView(YkiArvioijaParams(kausiPaattyyEnnen = LocalDate.of(2020, 1, 1))).size,
        )
    }

    @Test
    fun `vainSolkiVirheet rajaa riveihin joilla on lähetysvirhe`() {
        assertEquals(0, repository.findForListView(YkiArvioijaParams(vainSolkiVirheet = true)).size)
    }

    @Test
    fun `sivutus ja laskeva järjestys toimivat`() {
        val ekaSivu =
            repository.findForListView(
                YkiArvioijaParams(sortDirection = SortDirection.DESC, page = 1, limit = 2),
            )
        assertEquals(listOf("Öhman-Testi", "Kivinen-Testi"), ekaSivu.map { it.sukunimi })

        val tokaSivu =
            repository.findForListView(
                YkiArvioijaParams(sortDirection = SortDirection.DESC, page = 2, limit = 2),
            )
        assertEquals(listOf("Andersson-Testi"), tokaSivu.map { it.sukunimi })
    }

    @Test
    fun `CSV-vienti sisältää enemmän sarakkeita kuin listanäkymä ja henkilötiedot voi piilottaa`() {
        val listView =
            RenderableDisplayTableEnum.getByTags<YkiArvioijaColumn, YkiArvioijaListRow>(setOf(ColumnTag.LIST_VIEW))
        val csv =
            RenderableDisplayTableEnum.getByTags<YkiArvioijaColumn, YkiArvioijaListRow>(setOf(ColumnTag.CSV_EXPORT))
        val csvIlmanHenkilotietoja =
            RenderableDisplayTableEnum.getByTags<YkiArvioijaColumn, YkiArvioijaListRow>(
                setOf(ColumnTag.CSV_EXPORT),
                setOf(ColumnTag.PERSONAL_DATA),
            )

        assertTrue(csv.size > listView.size, "CSV-vienti sisaltaa myos vain-CSV-sarakkeet")
        assertTrue(csv.any { it == YkiArvioijaColumn.AshaNumero })
        assertTrue(listView.none { it == YkiArvioijaColumn.AshaNumero })
        assertTrue(csvIlmanHenkilotietoja.none { it == YkiArvioijaColumn.Oppijanumero })
    }
}
