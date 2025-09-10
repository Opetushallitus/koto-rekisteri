package fi.oph.kitu.vkt.tiedonsiirtoschema

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import fi.oph.kitu.Validation
import fi.oph.kitu.ValidationResult
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.oppijanumero.OppijanumeroService
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.vkt.VktSuoritusEntity
import fi.oph.kitu.vkt.VktValidation
import java.time.LocalDate
import java.time.OffsetDateTime

data class Henkilosuoritus<T : KielitutkinnonSuoritus>(
    val henkilo: OidOppija,
    val suoritus: T,
    val lisatty: OffsetDateTime? = null,
) {
    fun fill(onr: OppijanumeroService): Henkilosuoritus<T>? = henkilo.fill(onr)?.let { copy(it, suoritus) }

    fun toVktSuoritusEntity(): VktSuoritusEntity? =
        when (suoritus) {
            is VktSuoritus -> suoritus.toVktSuoritusEntity(henkilo)
            else -> null
        }

    companion object {
        fun from(entity: VktSuoritusEntity) =
            Henkilosuoritus(
                henkilo = OidOppija.from(entity),
                suoritus = VktSuoritus.from(entity),
                lisatty = entity.createdAt,
            )
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
)
interface KielitutkinnonSuoritus :
    PolymorphicByTyyppi,
    Lahdejarjestelmallinen {
    override val tyyppi: Koodisto.SuorituksenTyyppi

    companion object {
        fun validateAndEnrich(
            suoritus: KielitutkinnonSuoritus,
            vktValidation: VktValidation,
        ): ValidationResult<KielitutkinnonSuoritus> =
            when (suoritus) {
                is VktSuoritus -> vktValidation.validateAndEnrich(suoritus)
                else -> Validation.ok(suoritus)
            }
    }
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
