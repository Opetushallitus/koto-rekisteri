package fi.oph.kitu.organisaatiot

import com.fasterxml.jackson.annotation.JsonValue
import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult
import fi.oph.kitu.i18n.LocalizedString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.LocalDate

abstract class OrganisaatioService {
    private val logger = LoggerFactory.getLogger(javaClass)

    abstract fun getOrganisaatio(oid: Oid): TypedResult<GetOrganisaatioResponse, OrganisaatiopalveluException>

    abstract fun getOrganisaatiohierarkia(
        aktiiviset: Boolean = true,
        suunnitellut: Boolean = false,
        lakkautetut: Boolean = false,
    ): TypedResult<GetOrganisaatiohierarkiaResponse, OrganisaatiopalveluException>

    val nimet: Map<Oid, LocalizedString> by lazy {
        logger.info("Populating organisaatioService.nimet cache")
        val result = getOrganisaatiohierarkia().fold({ it.getNames() }, { emptyMap() })
        logger.info("Populated organisaatioService.nimet cache")
        result
    }
}

@Service
@Profile("!test ")
class OrganisaatioServiceImpl(
    val client: OrganisaatiopalveluClient,
) : OrganisaatioService() {
    override fun getOrganisaatio(oid: Oid): TypedResult<GetOrganisaatioResponse, OrganisaatiopalveluException> =
        client.get("api/$oid", responseType = GetOrganisaatioResponse::class.java)

    override fun getOrganisaatiohierarkia(
        aktiiviset: Boolean,
        suunnitellut: Boolean,
        lakkautetut: Boolean,
    ): TypedResult<GetOrganisaatiohierarkiaResponse, OrganisaatiopalveluException> =
        client.get(
            endpoint = "api/hierarkia/hae",
            query =
                mapOf(
                    "aktiiviset" to aktiiviset,
                    "suunnitellut" to suunnitellut,
                    "lakkautetut" to lakkautetut,
                ),
            responseType = GetOrganisaatiohierarkiaResponse::class.java,
        )
}

@Component
@Profile("!local") // Ei ladata koko organisaatiohierarkiaa joka kerta kun lokaali instanssi käynnistyy
class OrganisaatioPalveluStartupTasks(
    val organisaatioService: OrganisaatioService,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        CoroutineScope(Dispatchers.Default).launch {
            organisaatioService.nimet
        }
    }
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

data class GetOrganisaatiohierarkiaResponse(
    val numHits: Int,
    val organisaatiot: List<Organisaatiohierarkia>,
) {
    fun getNames(): Map<Oid, LocalizedString> = organisaatiot.flatMap { it.getNames() }.toMap()
}

data class Organisaatiohierarkia(
    val aliOrganisaatioMaara: Int,
    val alkuPvm: Long,
    val children: List<Organisaatiohierarkia>,
    val kieletUris: List<String>,
    val kotipaikkaUri: String,
    val lyhytNimi: LocalizedString,
    val match: Boolean,
    val nimi: LocalizedString,
    val oid: Oid,
    val organisaatiotyypit: List<String>,
    val parentOid: Oid?,
    val parentOidPath: String?,
    val status: String,
    val toimipistekoodi: String?,
    val tyypit: List<String>,
    val yTunnus: String?,
) {
    fun getNames(): List<Pair<Oid, LocalizedString>> = listOf(oid to nimi) + children.flatMap { it.getNames() }
}
