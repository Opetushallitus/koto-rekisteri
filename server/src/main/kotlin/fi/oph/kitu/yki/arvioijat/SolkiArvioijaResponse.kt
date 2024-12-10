package fi.oph.kitu.yki.arvioijat

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import fi.oph.kitu.csvparsing.Features
import fi.oph.kitu.csvparsing.yki.BooleanFromNumericDeserializer
import fi.oph.kitu.csvparsing.yki.TutkintokieliDeserializer
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import java.time.LocalDate

@JsonPropertyOrder(
    "arvioijanOppijanumero",
    "henkilotunnus",
    "sukunimi",
    "etunimet",
    "sahkopostiosoite",
    "katuosoite",
    "postinumero",
    "postitoimipaikka",
    "ensimmainenRekisterointipaiva",
    "kaudenAlkupaiva",
    "kaudenPaattymispaiva",
    "jatkorekisterointi",
    "tila",
    "kieli",
    "tasot",
)
@Features(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
class SolkiArvioijaResponse(
    @JsonProperty("arvioijanOppijanumero")
    val arvioijanOppijanumero: String,
    @JsonProperty("henkilotunnus")
    val henkilotunnus: String?,
    @JsonProperty("sukunimi")
    val sukunimi: String,
    @JsonProperty("etunimet")
    val etunimet: String,
    @JsonProperty("sahkopostiosoite")
    val sahkopostiosoite: String?,
    @JsonProperty("katuosoite")
    val katuosoite: String,
    @JsonProperty("postinumero")
    val postinumero: String,
    @JsonProperty("postitoimipaikka")
    val postitoimipaikka: String,
    @JsonProperty("ensimmainenRekisterointipaiva")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val ensimmainenRekisterointipaiva: LocalDate,
    @JsonProperty("kaudenAlkupaiva")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val kaudenAlkupaiva: LocalDate?,
    @JsonProperty("kaudenPaattymispaiva")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val kaudenPaattymispaiva: LocalDate?,
    @JsonProperty("jatkorekisterointi")
    @JsonDeserialize(using = BooleanFromNumericDeserializer::class)
    val jatkorekisterointi: Boolean,
    @JsonProperty("tila")
    val tila: Number,
    @JsonProperty("kieli")
    @JsonDeserialize(using = TutkintokieliDeserializer::class)
    val kieli: Tutkintokieli,
    @JsonProperty("tasot")
    @JsonDeserialize(using = TutkintotasotFromStringDeserializer::class)
    val tasot: Iterable<Tutkintotaso>,
) {
    override fun toString(): String =
        "SolkiArvioijaResponse(arvioijanOppijanumero='$arvioijanOppijanumero', tila=$tila, kieli=$kieli, tasot='$tasot')"
}
