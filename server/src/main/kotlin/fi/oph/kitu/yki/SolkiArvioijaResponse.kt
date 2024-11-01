package fi.oph.kitu.yki

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.MapperFeature
import fi.oph.kitu.csvparsing.Features

@JsonPropertyOrder(
    "arvioijanOppijanumero",
    "henkilotunnus",
    "sukunimi",
    "etunimet",
    "sahkopostiosoite",
    "katuosoite",
    "postinumero",
    "postitoimipaikka",
    "tila",
    "kieli",
    "tasot",
)
@Features(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
class SolkiArvioijaResponse(
    @JsonProperty("arvioijanOppijanumero")
    val arvioijanOppijanumero: String,
    @JsonProperty("henkilotunnus")
    val henkilotunnus: String,
    @JsonProperty("sukunimi")
    val sukunimi: String,
    @JsonProperty("etunimet")
    val etunimet: String,
    @JsonProperty("sahkopostiosoite")
    val sahkopostiosoite: String,
    @JsonProperty("katuosoite")
    val katuosoite: String,
    @JsonProperty("postinumero")
    val postinumero: String,
    @JsonProperty("postitoimipaikka")
    val postitoimipaikka: String,
    @JsonProperty("tila")
    val tila: Number,
    @JsonProperty("kieli")
    val kieli: Tutkintokieli,
    @JsonProperty("tasot")
    val tasot: String,
) {
    fun toEntity(id: Number? = null) =
        YkiArvioijaEntity(
            id,
            arvioijanOppijanumero,
            henkilotunnus,
            sukunimi,
            etunimet,
            sahkopostiosoite,
            katuosoite,
            postinumero,
            postitoimipaikka,
            tila,
            kieli,
            // tasot = tasot.split("+").map({ taso -> Tutkintotaso.valueOf(taso) }).toSet(),
            tasot = tasot.split("+").toSet(),
        )

    override fun toString(): String =
        "SolkiArvioijaResponse(arvioijanOppijanumero='$arvioijanOppijanumero', tila=$tila, kieli=$kieli, tasot='$tasot')"
}
