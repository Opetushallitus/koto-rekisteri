package fi.oph.kitu.koski

import arrow.core.Either
import arrow.core.left
import arrow.core.right
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
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.tiedontuontischema.VktHenkilosuoritus
import fi.oph.kitu.util.result.getOrThrow
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class KoskiVktRequestMapper {
    @Value($$"${kitu.oids.valtionhallinnonkielitutkinnot}")
    lateinit var vktOrganisaatioOid: String

    @WithSpan
    fun vktSuoritusToKoskiRequest(henkilosuoritus: VktHenkilosuoritus): Either<List<String>, KoskiRequest> {
        val henkilo = henkilosuoritus.henkilo
        val suoritus = henkilosuoritus.suoritus

        val kaikkiOsakokeetArvioitu = suoritus.osat.all { it.arviointi != null }

        val organisaatio: Organisaatio =
            suoritus.osat.firstNotNullOfOrNull { osa -> osa.oppilaitos?.let { Organisaatio(it) } }
                ?: Oid.parse(vktOrganisaatioOid).map { Organisaatio(it) }.getOrThrow()

        val arviointipaiva =
            suoritus.osat
                .mapNotNull { it.arviointi?.paivamaara }
                .maxOrNull()

        val valmiitTutkinnot =
            suoritus.tutkinnot
                .filter { it.puuttuvatOsakokeet().isEmpty() }
                .filter { it.puuttuvatArvioinnit().isEmpty() }

        val vahvistus: Either<List<String>, KielitutkintoSuoritus.VahvistusPaikkakunnalla> =
            if (kaikkiOsakokeetArvioitu &&
                arviointipaiva != null &&
                suoritus.suorituspaikkakunta != null &&
                valmiitTutkinnot.isNotEmpty()
            ) {
                KielitutkintoSuoritus
                    .VahvistusPaikkakunnalla(
                        päivä = arviointipaiva,
                        myöntäjäOrganisaatio = Organisaatio(organisaatio.oid),
                        paikkakunta = KoskiKoodiviite(suoritus.suorituspaikkakunta, "kunta"),
                    ).right()
            } else {
                listOfNotNull(
                    if (!kaikkiOsakokeetArvioitu) "Arviointi puuttuu" else null,
                    if (arviointipaiva == null) "Viimeisintä arviointipäivää ei voida päätellä" else null,
                    if (suoritus.suorituspaikkakunta == null) "Suorituspaikkakunta puuttuu" else null,
                    if (valmiitTutkinnot.isEmpty()) "Ei valmiita tutkintoja" else null,
                ).left()
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
