package fi.oph.kitu.koski

import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult
import fi.oph.kitu.koodisto.Koodisto
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
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class KoskiVktRequestMapper {
    @Value("\${kitu.oids.valtionhallinnonkielitutkinnot}")
    lateinit var vktOrganisaatioOid: String

    @WithSpan
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
}
