package fi.oph.kitu.yki.arvioijat

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import fi.oph.kitu.Oid
import fi.oph.kitu.csvparsing.Features
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
    @param:JsonProperty("arvioijanOppijanumero")
    val arvioijanOppijanumero: Oid,
    @param:JsonProperty("henkilotunnus")
    val henkilotunnus: String?,
    @param:JsonProperty("sukunimi")
    val sukunimi: String,
    @param:JsonProperty("etunimet")
    val etunimet: String,
    @param:JsonProperty("sahkopostiosoite")
    val sahkopostiosoite: String?,
    @param:JsonProperty("katuosoite")
    val katuosoite: String?,
    @param:JsonProperty("postinumero")
    val postinumero: String?,
    @param:JsonProperty("postitoimipaikka")
    val postitoimipaikka: String?,
    @param:JsonProperty("ensimmainenRekisterointipaiva")
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val ensimmainenRekisterointipaiva: LocalDate,
    @param:JsonProperty("kaudenAlkupaiva")
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val kaudenAlkupaiva: LocalDate?,
    @param:JsonProperty("kaudenPaattymispaiva")
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val kaudenPaattymispaiva: LocalDate?,
    @param:JsonProperty("jatkorekisterointi")
    @param:JsonDeserialize(using = BooleanFromNumericDeserializer::class)
    val jatkorekisterointi: Boolean,
    @param:JsonProperty("tila")
    val tila: Number,
    @param:JsonProperty("kieli")
    @param:JsonDeserialize(using = TutkintokieliDeserializer::class)
    val kieli: Tutkintokieli,
    @param:JsonProperty("tasot")
    @param:JsonDeserialize(using = TutkintotasotFromStringDeserializer::class)
    val tasot: Iterable<Tutkintotaso>,
) {
    override fun toString(): String =
        "SolkiArvioijaResponse(arvioijanOppijanumero='$arvioijanOppijanumero', tila=$tila, kieli=$kieli, tasot='$tasot')"
}
