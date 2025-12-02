package fi.oph.kitu.tiedonsiirtoschema

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import fi.oph.kitu.Oid
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.yki.suoritukset.YkiSuoritus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.OffsetDateTime

data class Henkilosuoritus<T : KielitutkinnonSuoritus>(
    val henkilo: Henkilo,
    val suoritus: T,
    @field:Schema(hidden = true)
    val lisatty: OffsetDateTime? = null,
) {
    fun modifySuoritus(f: (T) -> T): Henkilosuoritus<T> = copy(suoritus = f(suoritus))

    inline fun <reified A> toEntity(): A =
        when (suoritus) {
            is VktSuoritus -> suoritus.toVktSuoritusEntity(henkilo)

            is YkiSuoritus -> suoritus.toYkiSuoritusEntity(henkilo)

            else -> throw RuntimeException(
                "Failed to convert to entity: not implemented for Henkilosuoritus<${suoritus::class.simpleName}>",
            )
        } as A
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

    @get:Schema(hidden = true)
    val internalId: Int?

    @get:Schema(hidden = true)
    val koskiOpiskeluoikeusOid: Oid?

    @get:Schema(hidden = true)
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
