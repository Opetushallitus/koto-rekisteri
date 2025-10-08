package fi.oph.kitu.tiedonsiirtoschema

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.JsonMappingException
import fi.oph.kitu.Oid
import fi.oph.kitu.defaultObjectMapper
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.validation.Validation
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.yki.suoritukset.YkiSuoritus
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDate
import java.time.OffsetDateTime

data class Henkilosuoritus<T : KielitutkinnonSuoritus>(
    val henkilo: Henkilo,
    val suoritus: T,
    val lisatty: OffsetDateTime? = null,
) {
    fun modifySuoritus(f: (T) -> T): Henkilosuoritus<T> = copy(suoritus = f(suoritus))

    inline fun <reified A> toEntity(): A? =
        when (suoritus) {
            is VktSuoritus -> suoritus.toVktSuoritusEntity(henkilo)
            is YkiSuoritus -> suoritus.toYkiSuoritusEntity(henkilo)
            else -> null
        } as? A

    companion object {
        inline fun <reified T : KielitutkinnonSuoritus> deserializationAtEndpoint(
            json: String,
            save: (data: Henkilosuoritus<T>) -> Unit,
        ): ResponseEntity<*> =
            try {
                val data = defaultObjectMapper.readValue(json, Henkilosuoritus::class.java)
                when (data.suoritus) {
                    is T -> {
                        save(Henkilosuoritus(data.henkilo, data.suoritus))
                        TiedonsiirtoSuccess()
                    }
                    else ->
                        TiedonsiirtoFailure.badRequest(
                            "Vain ${T::class.simpleName} mukainen kielitutkinnon siirto sallittu",
                        )
                }
            } catch (e: JsonMappingException) {
                TiedonsiirtoFailure.badRequest(e.message ?: "JSON mapping failed for unknown reason")
            } catch (e: Validation.ValidationException) {
                TiedonsiirtoFailure(statusCode = HttpStatus.BAD_REQUEST, errors = e.errors.map { it.toString() })
            }.toResponseEntity()
    }
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "tyyppi",
    visible = true,
)
interface PolymorphicByTyyppi {
    val tyyppi: Koodisto.Koodiviite
}

@JsonSubTypes(
    JsonSubTypes.Type(value = VktSuoritus::class, name = "valtionhallinnonkielitutkinto"),
    JsonSubTypes.Type(value = YkiSuoritus::class, name = "yleinenkielitutkinto"),
)
interface KielitutkinnonSuoritus :
    PolymorphicByTyyppi,
    Lahdejarjestelmallinen {
    override val tyyppi: Koodisto.SuorituksenTyyppi

    val internalId: Int?
    val koskiOpiskeluoikeusOid: Oid?
    val koskiSiirtoKasitelty: Boolean
}

interface Osasuoritus : PolymorphicByTyyppi

interface Osasuorituksellinen : PolymorphicByTyyppi {
    val osat: List<Osasuoritus>
}

interface Arvioitava {
    val arviointi: Arviointi?
}

interface Arviointi {
    val arvosana: Koodisto.Koodiviite
    val paivamaara: LocalDate
}
