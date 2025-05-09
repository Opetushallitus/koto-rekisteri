package fi.oph.kitu.schema

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.vkt.VktSuoritusEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class Henkilosuoritus<T : KielitutkinnonSuoritus>(
    @get:JsonProperty("henkilö")
    val henkilo: OidOppija,
    val suoritus: T,
) {
    fun toVktSuoritusEntity(): VktSuoritusEntity? =
        when (suoritus) {
            is VktSuoritus -> suoritus.toVktSuoritusEntity(henkilo)
            else -> null
        }

    companion object {
        fun getDefaultObjectMapper(): ObjectMapper {
            val mapper = ObjectMapper()

            val javaTime =
                JavaTimeModule()
                    .addSerializer(LocalDate::class.java, LocalDateSerializer(DateTimeFormatter.ISO_LOCAL_DATE))
                    .addSerializer(
                        LocalDateTime::class.java,
                        LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    ).addSerializer(
                        ZonedDateTime::class.java,
                        ZonedDateTimeSerializer(DateTimeFormatter.ISO_ZONED_DATE_TIME),
                    )

            mapper.registerKotlinModule()
            mapper.registerModule(javaTime)
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            mapper.enable(SerializationFeature.INDENT_OUTPUT)

            return mapper
        }

        fun from(entity: VktSuoritusEntity) =
            Henkilosuoritus(
                henkilo = OidOppija.from(entity),
                suoritus = VktSuoritus.from(entity),
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

    @get:JsonProperty("päivämäärä")
    val paivamaara: LocalDate
}
