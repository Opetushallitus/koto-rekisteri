package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.util.result.getOrThrow
import io.opentelemetry.api.OpenTelemetry
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@JsonPropertyOrder("oid", "nimi", "maara", "paivamaara", "kuvaus")
private data class TestRow(
    val oid: Oid,
    val nimi: String,
    val maara: Int?,
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val paivamaara: LocalDate?,
    val kuvaus: String?,
)

class CsvGeneratorTest {
    private val generator = CsvGenerator(OpenTelemetry.noop().getTracer("test"))

    private fun row(
        nimi: String = "Testi",
        maara: Int? = 5,
        paivamaara: LocalDate? = LocalDate.of(2024, 9, 1),
        kuvaus: String? = "kuvaus",
    ) = TestRow(
        oid = Oid.parse("1.2.246.562.24.20281155246").getOrThrow(),
        nimi = nimi,
        maara = maara,
        paivamaara = paivamaara,
        kuvaus = kuvaus,
    )

    private fun writeCsv(
        rows: List<TestRow>,
        useHeader: Boolean = true,
    ): String {
        val outputStream = ByteArrayOutputStream()
        generator.withUseHeader(useHeader).streamDataAsCsv(outputStream, rows)
        return outputStream.toString(Charsets.UTF_8)
    }

    @Test
    fun `otsikko ja sarakejarjestys noudattavat JsonPropertyOrder-annotaatiota`() {
        val csv = writeCsv(listOf(row()))

        assertEquals(
            """
            oid,nimi,maara,paivamaara,kuvaus
            "1.2.246.562.24.20281155246",Testi,5,2024-09-01,kuvaus

            """.trimIndent(),
            csv,
        )
    }

    @Test
    fun `otsikkorivi jatetaan pois kun useHeader on epatosi`() {
        val csv = writeCsv(listOf(row()), useHeader = false)

        assertEquals(
            """
            "1.2.246.562.24.20281155246",Testi,5,2024-09-01,kuvaus

            """.trimIndent(),
            csv,
        )
    }

    @Test
    fun `null-arvot kirjoitetaan tyhjina sarakkeina`() {
        val csv = writeCsv(listOf(row(maara = null, paivamaara = null, kuvaus = null)))

        assertEquals(
            """
            oid,nimi,maara,paivamaara,kuvaus
            "1.2.246.562.24.20281155246",Testi,,,

            """.trimIndent(),
            csv,
        )
    }

    @Test
    fun `erotinmerkin sisaltava arvo lainausmerkitetaan`() {
        val csv = writeCsv(listOf(row(nimi = "Sukunimi, Etunimi")))

        assertTrue(
            csv.contains(""""Sukunimi, Etunimi""""),
            "pilkun sisältävä arvo pitää lainausmerkitä; saatiin:\n$csv",
        )
    }

    @Test
    fun `taulukkolaskennan kaavan aloittavat merkit neutraloidaan`() {
        val csv =
            writeCsv(
                listOf(
                    row(nimi = "=cmd|'/c calc'!A0", kuvaus = "@SUM(A1)"),
                    row(nimi = "+attack@example.com", kuvaus = "-1+1"),
                ),
            )

        assertTrue(csv.contains("'=cmd|"), "= pitää etuliittää heittomerkillä; saatiin:\n$csv")
        assertTrue(csv.contains("'@SUM"), "@ pitää etuliittää heittomerkillä; saatiin:\n$csv")
        assertTrue(csv.contains("'+attack@example.com"), "+ pitää etuliittää heittomerkillä; saatiin:\n$csv")
        assertTrue(csv.contains("'-1+1"), "- pitää etuliittää heittomerkillä; saatiin:\n$csv")
    }
}
