package fi.oph.kitu.koski

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.koodisto.Koodisto.YkiArvosana
import fi.oph.kitu.koodisto.KoskiKoodiviite
import fi.oph.kitu.koski.KoskiRequest.Henkilo
import fi.oph.kitu.koski.KoskiRequest.Opiskeluoikeus
import fi.oph.kitu.koski.KoskiRequest.Opiskeluoikeus.KielitutkintoSuoritus
import fi.oph.kitu.koski.KoskiRequest.Opiskeluoikeus.KielitutkintoSuoritus.KoulutusModuuli
import fi.oph.kitu.koski.KoskiRequest.Opiskeluoikeus.KielitutkintoSuoritus.Organisaatio
import fi.oph.kitu.koski.KoskiRequest.Opiskeluoikeus.KielitutkintoSuoritus.Osasuoritus
import fi.oph.kitu.koski.KoskiRequest.Opiskeluoikeus.KielitutkintoSuoritus.Osasuoritus.Arvosana
import fi.oph.kitu.koski.KoskiRequest.Opiskeluoikeus.KielitutkintoSuoritus.Osasuoritus.OsasuoritusKoulutusModuuli
import fi.oph.kitu.koski.KoskiRequest.Opiskeluoikeus.KielitutkintoSuoritus.Vahvistus
import fi.oph.kitu.koski.KoskiRequest.Opiskeluoikeus.LahdeJarjestelmanId
import fi.oph.kitu.koski.KoskiRequest.Opiskeluoikeus.Tila
import fi.oph.kitu.koski.KoskiRequest.Opiskeluoikeus.Tila.OpiskeluoikeusJakso
import fi.oph.kitu.yki.Tutkintotaso
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class KoskiRequestMapper {
    @WithSpan
    fun ykiSuoritusToKoskiRequest(ykiSuoritus: YkiSuoritusEntity): KoskiRequest? =
        if (isVilpillinenTaiKeskeytettySuoritus(ykiSuoritus)) {
            null
        } else {
            KoskiRequest(
                henkilö = Henkilo(oid = ykiSuoritus.suorittajanOID.toString()),
                opiskeluoikeudet =
                    listOf(
                        Opiskeluoikeus(
                            lähdejärjestelmänId =
                                LahdeJarjestelmanId(
                                    id = ykiSuoritus.suoritusId.toString(),
                                ),
                            tila =
                                Tila(
                                    opiskeluoikeusjaksot =
                                        listOf(
                                            OpiskeluoikeusJakso(
                                                alku = ykiSuoritus.tutkintopaiva,
                                                tila = Koodisto.OpiskeluoikeudenTila.Lasna,
                                            ),
                                            OpiskeluoikeusJakso(
                                                alku = ykiSuoritus.arviointipaiva,
                                                tila = Koodisto.OpiskeluoikeudenTila.Paattynyt,
                                            ),
                                        ),
                                ),
                            suoritukset =
                                listOf(
                                    KielitutkintoSuoritus(
                                        tyyppi = Koodisto.SuorituksenTyyppi.YleinenKielitutkinto,
                                        koulutusmoduuli =
                                            KoulutusModuuli(
                                                tunniste =
                                                    Koodisto.YkiTutkintotaso
                                                        .valueOf(
                                                            ykiSuoritus.tutkintotaso.name,
                                                        ).toKoski(),
                                                kieli =
                                                    Koodisto.Tutkintokieli.valueOf(
                                                        ykiSuoritus.tutkintokieli.name,
                                                    ),
                                            ),
                                        toimipiste = Organisaatio(oid = ykiSuoritus.jarjestajanTunnusOid.toString()),
                                        vahvistus =
                                            Vahvistus(
                                                päivä = ykiSuoritus.arviointipaiva,
                                                myöntäjäOrganisaatio =
                                                    Organisaatio(
                                                        ykiSuoritus.jarjestajanTunnusOid.toString(),
                                                    ),
                                            ),
                                        osasuoritukset = convertYkiSuoritusToKoskiOsasuoritukset(ykiSuoritus),
                                        yleisarvosana =
                                            ykiSuoritus.yleisarvosana?.let {
                                                koodistoYkiArvosana(
                                                    it,
                                                    ykiSuoritus.tutkintotaso,
                                                ).toKoski()
                                            },
                                    ),
                                ),
                        ),
                    ),
            )
        }

    private fun convertYkiSuoritusToKoskiOsasuoritukset(suoritusEntity: YkiSuoritusEntity): List<Osasuoritus> =
        mapOf(
            Koodisto.YkiSuorituksenNimi.TekstinYmmartaminen to suoritusEntity.tekstinYmmartaminen,
            Koodisto.YkiSuorituksenNimi.Kirjoittaminen to suoritusEntity.kirjoittaminen,
            Koodisto.YkiSuorituksenNimi.PuheenYmmartaminen to suoritusEntity.puheenYmmartaminen,
            Koodisto.YkiSuorituksenNimi.Puhuminen to suoritusEntity.puhuminen,
            Koodisto.YkiSuorituksenNimi.RakenteetJaSanasto to suoritusEntity.rakenteetJaSanasto,
        ).mapNotNull { (suorituksenNimi, arvosana) ->
            arvosana?.let {
                yleisenKielitutkinnonOsa(
                    suorituksenNimi,
                    arvosana,
                    suoritusEntity.tutkintotaso,
                    suoritusEntity.arviointipaiva,
                )
            }
        }

    private fun isVilpillinenTaiKeskeytettySuoritus(suoritusEntity: YkiSuoritusEntity): Boolean =
        listOf(
            suoritusEntity.tekstinYmmartaminen,
            suoritusEntity.kirjoittaminen,
            suoritusEntity.puheenYmmartaminen,
            suoritusEntity.puhuminen,
            suoritusEntity.rakenteetJaSanasto,
        ).any { it == 10 || it == 11 }

    private fun yleisenKielitutkinnonOsa(
        suorituksenNimi: Koodisto.YkiSuorituksenNimi,
        arvosana: Int,
        tutkintotaso: Tutkintotaso,
        arviointipaiva: LocalDate,
    ) = Osasuoritus(
        koulutusmoduuli =
            OsasuoritusKoulutusModuuli(
                tunniste = suorituksenNimi.toKoski(),
            ),
        arviointi =
            listOf(
                Arvosana(
                    arvosana = koodistoYkiArvosana(arvosana, tutkintotaso).toKoski(),
                    päivä = arviointipaiva,
                ),
            ),
    )

    private fun koodistoYkiArvosana(
        arvosana: Int,
        tutkintotaso: Tutkintotaso,
    ) = when (tutkintotaso) {
        Tutkintotaso.PT ->
            when (arvosana) {
                0 -> YkiArvosana.ALLE1
                1 -> YkiArvosana.PT1
                2 -> YkiArvosana.PT2
                9 -> YkiArvosana.EiVoiArvioida
                10 -> YkiArvosana.Keskeytetty
                11 -> YkiArvosana.Vilppi
                else -> throw IllegalArgumentException("Invalid YKI arvosana $arvosana for tutkintotaso $tutkintotaso")
            }
        Tutkintotaso.KT ->
            when (arvosana) {
                3 -> YkiArvosana.KT3
                4 -> YkiArvosana.KT4
                0, 1, 2 -> YkiArvosana.ALLE3
                9 -> YkiArvosana.EiVoiArvioida
                10 -> YkiArvosana.Keskeytetty
                11 -> YkiArvosana.Vilppi
                else -> throw IllegalArgumentException("Invalid YKI arvosana $arvosana for tutkintotaso $tutkintotaso")
            }
        Tutkintotaso.YT ->
            when (arvosana) {
                5 -> YkiArvosana.YT5
                6 -> YkiArvosana.YT6
                0, 1, 2, 3, 4 -> YkiArvosana.ALLE5
                9 -> YkiArvosana.EiVoiArvioida
                10 -> YkiArvosana.Keskeytetty
                11 -> YkiArvosana.Vilppi
                else -> throw IllegalArgumentException("Invalid YKI arvosana $arvosana for tutkintotaso $tutkintotaso")
            }
    }

    companion object {
        fun getObjectMapper(): ObjectMapper {
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

            return jacksonObjectMapper()
                .registerKotlinModule()
                .registerModule(JavaTimeModule())
                .registerModule(KoskiKoodiviite.Companion.KoskiKoodiviiteModule())
                .registerModule(javaTime)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
}
