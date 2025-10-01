package fi.oph.kitu.tiedonsiirtoschema

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.yki.YkiSuoritus
import java.time.LocalDate
import java.time.OffsetDateTime

data class Henkilosuoritus<T : KielitutkinnonSuoritus>(
    val henkilo: Henkilo,
    val suoritus: T,
    val lisatty: OffsetDateTime? = null,
) {
    inline fun <reified A> toEntity(): A? =
        when (suoritus) {
            is VktSuoritus -> suoritus.toVktSuoritusEntity(henkilo)
            is YkiSuoritus -> TODO()
            else -> null
        } as? A
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
