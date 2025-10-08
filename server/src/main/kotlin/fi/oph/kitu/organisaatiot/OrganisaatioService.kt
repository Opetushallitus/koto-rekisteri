package fi.oph.kitu.organisaatiot

import com.fasterxml.jackson.annotation.JsonValue
import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult
import fi.oph.kitu.i18n.LocalizedString
import org.springframework.stereotype.Service
import java.time.LocalDate

interface OrganisaatioService {
    fun getOrganisaatio(oid: Oid): TypedResult<GetOrganisaatioResponse, OrganisaatiopalveluException>
}

@Service
class OrganisaatioServiceImpl(
    val client: OrganisaatiopalveluClient,
) : OrganisaatioService {
    override fun getOrganisaatio(oid: Oid): TypedResult<GetOrganisaatioResponse, OrganisaatiopalveluException> =
        client.get("api/$oid", GetOrganisaatioResponse::class.java)
}

data class GetOrganisaatioResponse(
    val status: String,
    val tyypit: List<KoodiviiteUri>,
    val nimi: LocalizedString,
    val oid: Oid,
    val parentOid: Oid?,
    val alkuPvm: LocalDate,
    val yhteystiedot: List<OrganisaatioYhteystieto>,
    val kayntiosoite: OrganisaatioYhteystieto? = null,
    val postiosoite: OrganisaatioYhteystieto? = null,
    val kotipaikkaUri: KoodiviiteUri,
    val maaUri: KoodiviiteUri,
)

data class OrganisaatioYhteystieto(
    val osoiteTyyppi: String?,
    val kieli: String?,
    val postinumeroUri: String?,
    val yhteystietoOid: String?,
    val postitoimipaikka: String?,
    val osoite: String?,
)

data class KoodiviiteUri(
    @JsonValue
    val viite: String,
) {
    private val tokens: List<String> by lazy {
        Regex("([a-zA-Z0-9]+)_([a-zA-Z0-9]+)#?(\\d)*")
            .find(viite)
            ?.groupValues
            ?: emptyList()
    }

    val koodistoUri: String by lazy { tokens[1] }
    val koodiarvo: String by lazy { tokens[2] }
    val versio: Int? by lazy { tokens.getOrNull(3)?.toIntOrNull() }
}
