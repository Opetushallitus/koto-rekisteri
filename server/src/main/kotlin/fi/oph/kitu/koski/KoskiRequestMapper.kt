package fi.oph.kitu.koski

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.koodisto.Koodisto.YkiArvosana
import fi.oph.kitu.koodisto.KoskiKoodiviite
import fi.oph.kitu.koski.KoskiRequest.Henkilo
import fi.oph.kitu.koski.KoskiRequest.Opiskeluoikeus
import fi.oph.kitu.koski.KoskiRequest.Opiskeluoikeus.KielitutkintoSuoritus
import fi.oph.kitu.koski.KoskiRequest.Opiskeluoikeus.KielitutkintoSuoritus.KoulutusModuuli
import fi.oph.kitu.koski.KoskiRequest.Opiskeluoikeus.KielitutkintoSuoritus.Organisaatio
import fi.oph.kitu.koski.KoskiRequest.Opiskeluoikeus.LahdeJarjestelmanId
import fi.oph.kitu.koski.KoskiRequest.Opiskeluoikeus.Tila
import fi.oph.kitu.koski.KoskiRequest.Opiskeluoikeus.Tila.OpiskeluoikeusJakso
import fi.oph.kitu.vkt.VktHenkilosuoritus
import fi.oph.kitu.yki.Tutkintotaso
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class KoskiRequestMapper {
    @Value("\${kitu.oids.valtionhallinnonkielitutkinnot}")
    lateinit var vktOrganisaatioOid: String

    @WithSpan
    fun ykiSuoritusToKoskiRequest(ykiSuoritus: YkiSuoritusEntity): KoskiRequest? =
        if (isKoskiSiirtoEstetty(ykiSuoritus)) {
            null
        } else {
            KoskiRequest(
                henkilö = Henkilo(oid = ykiSuoritus.suorittajanOID),
                opiskeluoikeudet =
                    listOf(
                        Opiskeluoikeus(
                            oid = ykiSuoritus.koskiOpiskeluoikeus,
                            lähdejärjestelmänId =
                                LahdeJarjestelmanId(
                                    id = "yki.${ykiSuoritus.suoritusId}",
                                ),
                            tila =
                                Tila(
                                    opiskeluoikeusjaksot =
                                        listOfNotNull(
                                            OpiskeluoikeusJakso(
                                                alku = ykiSuoritus.tutkintopaiva,
                                                tila = Koodisto.OpiskeluoikeudenTila.Lasna,
                                            ),
                                            ykiSuoritus.arviointipaiva?.let {
                                                OpiskeluoikeusJakso(
                                                    alku = ykiSuoritus.arviointipaiva,
                                                    tila = Koodisto.OpiskeluoikeudenTila.Paattynyt,
                                                )
                                            },
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
                                        toimipiste = Organisaatio(oid = ykiSuoritus.jarjestajanTunnusOid),
                                        vahvistus =
                                            ykiSuoritus.arviointipaiva?.let {
                                                KielitutkintoSuoritus.VahvistusImpl(
                                                    päivä = ykiSuoritus.arviointipaiva,
                                                    myöntäjäOrganisaatio =
                                                        Organisaatio(ykiSuoritus.jarjestajanTunnusOid),
                                                )
                                            },
                                        osasuoritukset = convertYkiSuoritusToKoskiOsasuoritukset(ykiSuoritus),
                                        yleisarvosana =
                                            ykiSuoritus.yleisarvosana?.let {
                                                YkiArvosana.of(it, ykiSuoritus.tutkintotaso).toKoski()
                                            },
                                    ),
                                ),
                        ),
                    ),
            )
        }

    private fun convertYkiSuoritusToKoskiOsasuoritukset(suoritusEntity: YkiSuoritusEntity): List<Osasuoritus> =
        mapOf(
            Koodisto.YkiSuorituksenOsa.TekstinYmmartaminen to suoritusEntity.tekstinYmmartaminen,
            Koodisto.YkiSuorituksenOsa.Kirjoittaminen to suoritusEntity.kirjoittaminen,
            Koodisto.YkiSuorituksenOsa.PuheenYmmartaminen to suoritusEntity.puheenYmmartaminen,
            Koodisto.YkiSuorituksenOsa.Puhuminen to suoritusEntity.puhuminen,
            Koodisto.YkiSuorituksenOsa.RakenteetJaSanasto to suoritusEntity.rakenteetJaSanasto,
        ).mapNotNull { (suorituksenNimi, arvosana) ->
            arvosana?.let {
                suoritusEntity.arviointipaiva?.let {
                    yleisenKielitutkinnonOsa(
                        suorituksenNimi,
                        arvosana,
                        suoritusEntity.tutkintotaso,
                        suoritusEntity.arviointipaiva,
                    )
                }
            }
        }

    private fun isKoskiSiirtoEstetty(suoritusEntity: YkiSuoritusEntity): Boolean =
        !suoritusEntity.arviointitila.arviointiValmis() ||
            listOf(
                suoritusEntity.tekstinYmmartaminen,
                suoritusEntity.kirjoittaminen,
                suoritusEntity.puheenYmmartaminen,
                suoritusEntity.puhuminen,
                suoritusEntity.rakenteetJaSanasto,
            ).any { it == 10 || it == 11 }

    private fun yleisenKielitutkinnonOsa(
        suorituksenNimi: Koodisto.YkiSuorituksenOsa,
        arvosana: Int,
        tutkintotaso: Tutkintotaso,
        arviointipaiva: LocalDate,
    ) = YkiOsasuoritus(
        koulutusmoduuli =
            OsasuorituksenKoulutusmoduuli(
                tunniste = suorituksenNimi.toKoski(),
            ),
        arviointi =
            listOf(
                Arvosana(
                    arvosana = YkiArvosana.of(arvosana, tutkintotaso).toKoski(),
                    päivä = arviointipaiva,
                ),
            ),
        alkamispäivä = null,
    )

    fun vktSuoritusToKoskiRequest(henkilosuoritus: VktHenkilosuoritus): TypedResult<KoskiRequest, List<String>> {
        val henkilo = henkilosuoritus.henkilo
        val suoritus = henkilosuoritus.suoritus

        val kaikkiOsakokeetArvioitu = suoritus.osat.all { it.arviointi != null }

        val organisaatio: Organisaatio =
            suoritus.osat.firstNotNullOfOrNull { it.oppilaitos?.let { Organisaatio(it) } }
                ?: Oid.parse(vktOrganisaatioOid).map { Organisaatio(it) }.getOrThrow()

        val arviointipaiva =
            suoritus.osat
                .mapNotNull { it.arviointi?.paivamaara }
                .maxOrNull()

        val valmiitTutkinnot =
            suoritus.tutkinnot
                .filter { it.puuttuvatOsakokeet().isEmpty() }
                .filter { it.puuttuvatArvioinnit().isEmpty() }

        val vahvistus: TypedResult<KielitutkintoSuoritus.VahvistusPaikkakunnalla, List<String>> =
            if (kaikkiOsakokeetArvioitu &&
                arviointipaiva != null &&
                suoritus.suorituspaikkakunta != null &&
                valmiitTutkinnot.isNotEmpty()
            ) {
                TypedResult.Success(
                    KielitutkintoSuoritus.VahvistusPaikkakunnalla(
                        päivä = arviointipaiva,
                        myöntäjäOrganisaatio = Organisaatio(organisaatio.oid),
                        paikkakunta = KoskiKoodiviite(suoritus.suorituspaikkakunta, "kunta"),
                    ),
                )
            } else {
                TypedResult.Failure(
                    listOfNotNull(
                        if (!kaikkiOsakokeetArvioitu) "Arviointi puuttuu" else null,
                        if (arviointipaiva == null) "Viimeisintä arviointipäivää ei voida päätellä" else null,
                        if (suoritus.suorituspaikkakunta == null) "Suorituspaikkakunta puuttuu" else null,
                        if (valmiitTutkinnot.isEmpty()) "Ei valmiita tutkintoja" else null,
                    ),
                )
            }

        return vahvistus.map { vahvistus ->
            KoskiRequest(
                henkilö = Henkilo(oid = henkilo.oid),
                opiskeluoikeudet =
                    listOf(
                        Opiskeluoikeus(
                            oid = henkilosuoritus.suoritus.koskiOpiskeluoikeusOid,
                            lähdejärjestelmänId = LahdeJarjestelmanId(id = "vkt.${suoritus.internalId}"),
                            tyyppi = Koodisto.OpiskeluoikeudenTyyppi.Kielitutkinto,
                            tila =
                                Tila(
                                    opiskeluoikeusjaksot =
                                        listOfNotNull(
                                            OpiskeluoikeusJakso(
                                                alku = suoritus.osat.minOf { it.tutkintopaiva },
                                                tila = Koodisto.OpiskeluoikeudenTila.Lasna,
                                            ),
                                            arviointipaiva?.let {
                                                OpiskeluoikeusJakso(
                                                    alku = it,
                                                    tila = Koodisto.OpiskeluoikeudenTila.Paattynyt,
                                                )
                                            },
                                        ),
                                ),
                            suoritukset =
                                listOf(
                                    KielitutkintoSuoritus(
                                        tyyppi = Koodisto.SuorituksenTyyppi.ValtionhallinnonKielitutkinto,
                                        koulutusmoduuli =
                                            KoulutusModuuli(
                                                tunniste = suoritus.taitotaso.toKoski(),
                                                kieli = suoritus.kieli,
                                            ),
                                        toimipiste = organisaatio,
                                        vahvistus = vahvistus,
                                        osasuoritukset =
                                            valmiitTutkinnot.map { kielitaito ->
                                                VktKielitaito(
                                                    koulutusmoduuli =
                                                        OsasuorituksenKoulutusmoduuli(
                                                            tunniste = kielitaito.tyyppi.toKoski(),
                                                        ),
                                                    arviointi =
                                                        kielitaito.arviointi()?.let { arviointi ->
                                                            listOf(
                                                                Arvosana(
                                                                    arvosana = arviointi.arvosana.toKoski(),
                                                                    päivä = arviointi.paivamaara,
                                                                ),
                                                            )
                                                        } ?: emptyList(),
                                                    osasuoritukset =
                                                        kielitaito.osat.map { osakoe ->
                                                            VktOsakoe(
                                                                koulutusmoduuli =
                                                                    OsasuorituksenKoulutusmoduuli(
                                                                        tunniste = osakoe.tyyppi.toKoski(),
                                                                    ),
                                                                arviointi =
                                                                    osakoe.arviointi?.let { arviointi ->
                                                                        listOf(
                                                                            Arvosana(
                                                                                arvosana =
                                                                                    arviointi.arvosana
                                                                                        .toKoski(),
                                                                                päivä = arviointi.paivamaara,
                                                                            ),
                                                                        )
                                                                    } ?: emptyList(),
                                                                alkamispäivä = osakoe.tutkintopaiva,
                                                            )
                                                        },
                                                    alkamispäivä = kielitaito.osat.minOfOrNull { it.tutkintopaiva },
                                                )
                                            },
                                    ),
                                ),
                        ),
                    ),
            )
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
