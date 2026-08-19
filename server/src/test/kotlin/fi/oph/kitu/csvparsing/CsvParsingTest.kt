package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.util.result.getOrThrow
import fi.oph.kitu.yki.Arviointitila
import fi.oph.kitu.yki.Sukupuoli
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import fi.oph.kitu.yki.arvioijat.TutkintokieliDeserializer
import fi.oph.kitu.yki.suoritukset.Todistuskieli
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.testcontainers.postgresql.PostgreSQLContainer
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.annotation.JsonDeserialize
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@Import(DBContainerConfiguration::class)
class CsvParsingTest(
    @param:Autowired val parser: CsvGenerator,
    @param:Autowired private val postgres: PostgreSQLContainer,
) {
    @Test
    fun `test writing csv`() {
        val datePattern = "yyyy-MM-dd"
        val dateFormatter = DateTimeFormatter.ofPattern(datePattern)
        val parser = parser.withUseHeader(true)

        val writable =
            listOf(
                YkiSuoritusCsvResponse(
                    suorittajanOID = Oid.parse("1.2.246.562.24.20281155246").getOrThrow(),
                    hetu = "010180-9026",
                    sukupuoli = Sukupuoli.N,
                    sukunimi = "Öhman-Testi",
                    etunimet = "Ranja Testi",
                    kansalaisuus = "EST",
                    katuosoite = "Testikuja 5",
                    postinumero = "40100",
                    postitoimipaikka = "Testilä",
                    maa = "FIN",
                    email = "testi@testi.fi",
                    solkiTunniste = 183424,
                    lastModified = Instant.parse("2024-10-30T13:53:56Z"),
                    tutkintopaiva = LocalDate.parse("2024-09-01", dateFormatter),
                    tutkintokieli = Tutkintokieli.FIN,
                    tutkintotaso = Tutkintotaso.YT,
                    todistuskieli = Todistuskieli.FIN,
                    jarjestajanOID = Oid.parse("1.2.246.562.10.14893989377").getOrThrow(),
                    jarjestajanNimi = "Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",
                    arviointitila = Arviointitila.ARVIOITU,
                    tilaLahetetty = Instant.parse("2024-10-30T14:00:00Z"),
                    arviointipaiva = LocalDate.parse("2024-11-14", dateFormatter),
                    tekstinYmmartaminen = 5,
                    kirjoittaminen = 4,
                    rakenteetJaSanasto = 3,
                    puheenYmmartaminen = 1,
                    puhuminen = 2,
                    yleisarvosana = 3,
                    tarkistusarvioinninSaapumisPvm = LocalDate.parse("2024-10-01", dateFormatter),
                    tarkistusarvioinninAsiatunnus = "123123",
                    tarkistusarvioidutOsakokeet = "3",
                    arvosanaMuuttui = "2",
                    perustelu = "Tarkistusarvioinnin testi\\nJossa rivinvaihto",
                    tarkistusarvioinninKasittelyPvm = LocalDate.parse("2024-10-15", dateFormatter),
                ),
            )
        val outputStream = ByteArrayOutputStream()
        parser.streamDataAsCsv(outputStream, writable)
        val expectedCsv =
            """
            suorittajanOID,hetu,sukupuoli,sukunimi,etunimet,kansalaisuus,katuosoite,postinumero,postitoimipaikka,maa,email,solkiTunniste,lastModified,tutkintopaiva,tutkintokieli,tutkintotaso,todistuskieli,jarjestajanOID,jarjestajanNimi,arviointitila,tilaLahetetty,arviointipaiva,tekstinYmmartaminen,kirjoittaminen,puheenYmmartaminen,puhuminen,rakenteetJaSanasto,yleisarvosana,"tarkistusarvioinninSaapumisPvm","tarkistusarvioinninAsiatunnus","tarkistusarvioidutOsakokeet",arvosanaMuuttui,perustelu,"tarkistusarvioinninKasittelyPvm"
            "1.2.246.562.24.20281155246",010180-9026,N,Öhman-Testi,"Ranja Testi",EST,"Testikuja 5",40100,Testilä,FIN,testi@testi.fi,183424,2024-10-30T13:53:56Z,2024-09-01,fin,YT,fin,"1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",ARVIOITU,2024-10-30T14:00:00Z,2024-11-14,5,4,1,2,3,3,2024-10-01,123123,3,2,"Tarkistusarvioinnin testi\nJossa rivinvaihto",2024-10-15

            """.trimIndent()
        assertEquals(expectedCsv, outputStream.toString(Charsets.UTF_8))
    }

    @Test
    fun `null values are written correctly to csv`() {
        val datePattern = "yyyy-MM-dd"
        val dateFormatter = DateTimeFormatter.ofPattern(datePattern)
        val parser = parser.withUseHeader(true)
        val writable =
            listOf(
                YkiSuoritusCsvResponse(
                    suorittajanOID = Oid.parse("1.2.246.562.24.20281155246").getOrThrow(),
                    hetu = "010180-9026",
                    sukupuoli = Sukupuoli.N,
                    sukunimi = "Öhman-Testi",
                    etunimet = "Ranja Testi",
                    kansalaisuus = "EST",
                    katuosoite = "Testikuja 5",
                    postinumero = "40100",
                    postitoimipaikka = "Testilä",
                    maa = null,
                    email = null,
                    solkiTunniste = 183424,
                    lastModified = Instant.parse("2024-10-30T13:53:56Z"),
                    tutkintopaiva = LocalDate.parse("2024-09-01", dateFormatter),
                    tutkintokieli = Tutkintokieli.FIN,
                    tutkintotaso = Tutkintotaso.YT,
                    todistuskieli = null,
                    jarjestajanOID = Oid.parse("1.2.246.562.10.14893989377").getOrThrow(),
                    jarjestajanNimi = "Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",
                    arviointitila = Arviointitila.ARVIOITAVA,
                    tilaLahetetty = null,
                    arviointipaiva = LocalDate.parse("2024-11-14", dateFormatter),
                    tekstinYmmartaminen = null,
                    kirjoittaminen = null,
                    rakenteetJaSanasto = null,
                    puheenYmmartaminen = null,
                    puhuminen = null,
                    yleisarvosana = null,
                    tarkistusarvioinninSaapumisPvm = null,
                    tarkistusarvioinninAsiatunnus = null,
                    tarkistusarvioidutOsakokeet = null,
                    arvosanaMuuttui = null,
                    perustelu = null,
                    tarkistusarvioinninKasittelyPvm = null,
                ),
            )
        val outputStream = ByteArrayOutputStream()
        parser.streamDataAsCsv(outputStream, writable)

        val expectedCsv =
            """
            suorittajanOID,hetu,sukupuoli,sukunimi,etunimet,kansalaisuus,katuosoite,postinumero,postitoimipaikka,maa,email,solkiTunniste,lastModified,tutkintopaiva,tutkintokieli,tutkintotaso,todistuskieli,jarjestajanOID,jarjestajanNimi,arviointitila,tilaLahetetty,arviointipaiva,tekstinYmmartaminen,kirjoittaminen,puheenYmmartaminen,puhuminen,rakenteetJaSanasto,yleisarvosana,"tarkistusarvioinninSaapumisPvm","tarkistusarvioinninAsiatunnus","tarkistusarvioidutOsakokeet",arvosanaMuuttui,perustelu,"tarkistusarvioinninKasittelyPvm"
            "1.2.246.562.24.20281155246",010180-9026,N,Öhman-Testi,"Ranja Testi",EST,"Testikuja 5",40100,Testilä,,,183424,2024-10-30T13:53:56Z,2024-09-01,fin,YT,,"1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",ARVIOITAVA,,2024-11-14,,,,,,,,,,,,

            """.trimIndent()
        assertEquals(expectedCsv, outputStream.toString(Charsets.UTF_8))
    }

    @Test
    fun `csv export neutralizes spreadsheet-formula trigger characters`() {
        val datePattern = "yyyy-MM-dd"
        val dateFormatter = DateTimeFormatter.ofPattern(datePattern)
        val parser = parser.withUseHeader(true)
        val writable =
            listOf(
                YkiSuoritusCsvResponse(
                    suorittajanOID = Oid.parse("1.2.246.562.24.20281155246").getOrThrow(),
                    hetu = "010180-9026",
                    sukupuoli = Sukupuoli.N,
                    sukunimi = "=cmd|'/c calc'!A0",
                    etunimet = "@SUM(A1)",
                    kansalaisuus = "EST",
                    katuosoite = "Testikuja 5",
                    postinumero = "40100",
                    postitoimipaikka = "Testilä",
                    maa = "FIN",
                    email = "+attack@example.com",
                    solkiTunniste = 1,
                    lastModified = Instant.parse("2024-10-30T13:53:56Z"),
                    tutkintopaiva = LocalDate.parse("2024-09-01", dateFormatter),
                    tutkintokieli = Tutkintokieli.FIN,
                    tutkintotaso = Tutkintotaso.YT,
                    todistuskieli = Todistuskieli.FIN,
                    jarjestajanOID = Oid.parse("1.2.246.562.10.14893989377").getOrThrow(),
                    jarjestajanNimi = "Jyväskylän yliopisto",
                    arviointitila = Arviointitila.ARVIOITAVA,
                    tilaLahetetty = null,
                    arviointipaiva = null,
                    tekstinYmmartaminen = null,
                    kirjoittaminen = null,
                    rakenteetJaSanasto = null,
                    puheenYmmartaminen = null,
                    puhuminen = null,
                    yleisarvosana = null,
                    tarkistusarvioinninSaapumisPvm = null,
                    tarkistusarvioinninAsiatunnus = null,
                    tarkistusarvioidutOsakokeet = null,
                    arvosanaMuuttui = null,
                    perustelu = null,
                    tarkistusarvioinninKasittelyPvm = null,
                ),
            )
        val outputStream = ByteArrayOutputStream()
        parser.streamDataAsCsv(outputStream, writable)
        val csv = outputStream.toString(Charsets.UTF_8)

        assertTrue(csv.contains("'=cmd|"), "sukunimi must be tick-prefixed; got:\n$csv")
        assertTrue(csv.contains("'@SUM"), "etunimet must be tick-prefixed; got:\n$csv")
        assertTrue(csv.contains("'+attack@example.com"), "email must be tick-prefixed; got:\n$csv")
    }
}

@JsonPropertyOrder(
    "suorittajanOID",
    "hetu",
    "sukupuoli",
    "sukunimi",
    "etunimet",
    "kansalaisuus",
    "katuosoite",
    "postinumero",
    "postitoimipaikka",
    "email",
    "suoritusID",
    "lastModified",
    "tutkintopaiva",
    "tutkintokieli",
    "tutkintotaso",
    "jarjestajanOID",
    "jarjestajanNimi",
    "arviointipaiva",
    "tekstinYmmartaminen",
    "kirjoittaminen",
    "rakenteetJaSanasto",
    "puheenYmmartaminen",
    "puhuminen",
    "yleisarvosana",
    "tarkistusarvioinninSaapumisPvm",
    "tarkistusarvioinninAsiatunnus",
    "tarkistusarvioidutOsakokeet",
    "arvosanaMuuttui",
    "perustelu",
    "tarkistusarvioinninKasittelyPvm",
)
@MapperFeatures(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
private data class YkiSuoritusCsv(
    @param:JsonProperty("suorittajanOID")
    val suorittajanOID: Oid,
    @param:JsonProperty("hetu")
    val hetu: String?,
    @param:JsonProperty("sukupuoli")
    val sukupuoli: Sukupuoli?,
    @param:JsonProperty("sukunimi")
    val sukunimi: String,
    @param:JsonProperty("etunimet")
    val etunimet: String,
    @param:JsonProperty("kansalaisuus")
    val kansalaisuus: String,
    @param:JsonProperty("katuosoite")
    val katuosoite: String,
    @param:JsonProperty("postinumero")
    val postinumero: String,
    @param:JsonProperty("postitoimipaikka")
    val postitoimipaikka: String,
    @param:JsonProperty("email")
    val email: String?,
    @param:JsonProperty("suoritusID")
    val suoritusID: Int,
    @param:JsonProperty("lastModified")
    @param:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssX", timezone = "UTC")
    val lastModified: Instant,
    @param:JsonProperty("tutkintopaiva")
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tutkintopaiva: LocalDate,
    @param:JsonProperty("tutkintokieli")
    @param:JsonDeserialize(using = TutkintokieliDeserializer::class)
    val tutkintokieli: Tutkintokieli,
    @param:JsonProperty("tutkintotaso")
    val tutkintotaso: Tutkintotaso,
    @param:JsonProperty("jarjestajanOID")
    val jarjestajanOID: Oid,
    @param:JsonProperty("jarjestajanNimi")
    val jarjestajanNimi: String,
    @param:JsonProperty("arviointipaiva")
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val arviointipaiva: LocalDate?,
    @param:JsonProperty("tekstinYmmartaminen")
    val tekstinYmmartaminen: Int?,
    @param:JsonProperty("kirjoittaminen")
    val kirjoittaminen: Int?,
    @param:JsonProperty("rakenteetJaSanasto")
    val rakenteetJaSanasto: Int?,
    @param:JsonProperty("puheenYmmartaminen")
    val puheenYmmartaminen: Int?,
    @param:JsonProperty("puhuminen")
    val puhuminen: Int?,
    @param:JsonProperty("yleisarvosana")
    val yleisarvosana: Int?,
    @param:JsonProperty("tarkistusarvioinninSaapumisPvm")
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tarkistusarvioinninSaapumisPvm: LocalDate?,
    @param:JsonProperty("tarkistusarvioinninAsiatunnus")
    val tarkistusarvioinninAsiatunnus: String?,
    @param:JsonProperty("tarkistusarvioidutOsakokeet")
    val tarkistusarvioidutOsakokeet: Int?,
    @param:JsonProperty("arvosanaMuuttui")
    val arvosanaMuuttui: Int?,
    @param:JsonProperty("perustelu")
    val perustelu: String?,
    @param:JsonProperty("tarkistusarvioinninKasittelyPvm")
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tarkistusarvioinninKasittelyPvm: LocalDate?,
)

@JsonPropertyOrder(
    "suorittajanOID",
    "hetu",
    "sukupuoli",
    "sukunimi",
    "etunimet",
    "kansalaisuus",
    "katuosoite",
    "postinumero",
    "postitoimipaikka",
    "maa",
    "email",
    "solkiTunniste",
    "lastModified",
    "tutkintopaiva",
    "tutkintokieli",
    "tutkintotaso",
    "todistuskieli",
    "jarjestajanOID",
    "jarjestajanNimi",
    "arviointitila",
    "tilaLahetetty",
    "arviointipaiva",
    "tekstinYmmartaminen",
    "kirjoittaminen",
    "puheenYmmartaminen",
    "puhuminen",
    "rakenteetJaSanasto",
    "yleisarvosana",
    "tarkistusarvioinninSaapumisPvm",
    "tarkistusarvioinninAsiatunnus",
    "tarkistusarvioidutOsakokeet",
    "arvosanaMuuttui",
    "perustelu",
    "tarkistusarvioinninKasittelyPvm",
)
private data class YkiSuoritusCsvResponse(
    val suorittajanOID: Oid,
    val hetu: String?,
    val sukupuoli: Sukupuoli?,
    val sukunimi: String,
    val etunimet: String,
    val kansalaisuus: String,
    val katuosoite: String,
    val postinumero: String,
    val postitoimipaikka: String,
    val maa: String?,
    val email: String?,
    val solkiTunniste: Int,
    @param:JsonProperty("lastModified")
    @param:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssX", timezone = "UTC")
    val lastModified: Instant,
    @param:JsonProperty("tutkintopaiva")
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tutkintopaiva: LocalDate,
    @param:JsonProperty("tutkintokieli")
    @param:JsonDeserialize(using = TutkintokieliDeserializer::class)
    val tutkintokieli: Tutkintokieli,
    val tutkintotaso: Tutkintotaso,
    val todistuskieli: Todistuskieli?,
    val jarjestajanOID: Oid,
    val jarjestajanNimi: String,
    val arviointitila: Arviointitila,
    @param:JsonProperty("tilaLahetetty")
    @param:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssX", timezone = "UTC")
    val tilaLahetetty: Instant?,
    @param:JsonProperty("arviointipaiva")
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val arviointipaiva: LocalDate?,
    val tekstinYmmartaminen: Int?,
    val kirjoittaminen: Int?,
    val rakenteetJaSanasto: Int?,
    val puheenYmmartaminen: Int?,
    val puhuminen: Int?,
    val yleisarvosana: Int?,
    @param:JsonProperty("tarkistusarvioinninSaapumisPvm")
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tarkistusarvioinninSaapumisPvm: LocalDate?,
    val tarkistusarvioinninAsiatunnus: String?,
    val tarkistusarvioidutOsakokeet: String?,
    val arvosanaMuuttui: String?,
    val perustelu: String?,
    @param:JsonProperty("tarkistusarvioinninKasittelyPvm")
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tarkistusarvioinninKasittelyPvm: LocalDate?,
)
