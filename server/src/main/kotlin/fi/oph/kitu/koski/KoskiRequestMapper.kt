package fi.oph.kitu.koski

import fi.oph.kitu.koski.KoskiRequest.Henkilo
import fi.oph.kitu.koski.KoskiRequest.KoodistokoodiViite
import fi.oph.kitu.koski.KoskiRequest.KoskiOpiskeluoikeus
import fi.oph.kitu.koski.KoskiRequest.KoskiOpiskeluoikeus.KoskiKielitutkintoSuoritus
import fi.oph.kitu.koski.KoskiRequest.KoskiOpiskeluoikeus.KoskiKielitutkintoSuoritus.KoulutusModuuli
import fi.oph.kitu.koski.KoskiRequest.KoskiOpiskeluoikeus.KoskiKielitutkintoSuoritus.Organisaatio
import fi.oph.kitu.koski.KoskiRequest.KoskiOpiskeluoikeus.KoskiKielitutkintoSuoritus.Osasuoritus
import fi.oph.kitu.koski.KoskiRequest.KoskiOpiskeluoikeus.KoskiKielitutkintoSuoritus.Osasuoritus.Arvosana
import fi.oph.kitu.koski.KoskiRequest.KoskiOpiskeluoikeus.KoskiKielitutkintoSuoritus.Osasuoritus.OsasuoritusKoulutusModuuli
import fi.oph.kitu.koski.KoskiRequest.KoskiOpiskeluoikeus.KoskiKielitutkintoSuoritus.Vahvistus
import fi.oph.kitu.koski.KoskiRequest.KoskiOpiskeluoikeus.LahdeJarjestelmanId
import fi.oph.kitu.koski.KoskiRequest.KoskiOpiskeluoikeus.Tila
import fi.oph.kitu.koski.KoskiRequest.KoskiOpiskeluoikeus.Tila.OpiskeluoikeusJakso
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import java.time.LocalDate

class KoskiRequestMapper {
    fun ykiSuoritusToKoskiRequest(ykiSuoritus: YkiSuoritusEntity): KoskiRequest =
        KoskiRequest(
            henkilö = Henkilo(oid = ykiSuoritus.suorittajanOID),
            opiskeluoikeudet =
                listOf(
                    KoskiOpiskeluoikeus(
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
                                            tila =
                                                KoodistokoodiViite(
                                                    koodiarvo = "lasna",
                                                    koodistoUri = "koskiopiskeluoikeudentila",
                                                ),
                                        ),
                                        OpiskeluoikeusJakso(
                                            alku = ykiSuoritus.arviointipaiva,
                                            tila =
                                                KoodistokoodiViite(
                                                    koodiarvo = "hyvaksytystisuoritettu",
                                                    koodistoUri = "koskiopiskeluoikeudentila",
                                                ),
                                        ),
                                    ),
                            ),
                        suoritukset =
                            listOf(
                                KoskiKielitutkintoSuoritus(
                                    tyyppi =
                                        KoodistokoodiViite(
                                            koodiarvo = "yleinenkielitutkinto",
                                            koodistoUri = "suorituksentyyppi",
                                        ),
                                    koulutusmoduuli =
                                        KoulutusModuuli(
                                            tunniste =
                                                KoodistokoodiViite(
                                                    koodiarvo = ykiSuoritus.tutkintotaso.name.lowercase(),
                                                    koodistoUri = "ykitutkintotaso",
                                                ),
                                            kieli =
                                                KoodistokoodiViite(
                                                    koodiarvo = ykiSuoritus.tutkintokieli.name,
                                                    koodistoUri = "ykitutkintokieli",
                                                ),
                                        ),
                                    toimipiste = Organisaatio(oid = ykiSuoritus.jarjestajanTunnusOid),
                                    vahvistus =
                                        Vahvistus(
                                            päivä = ykiSuoritus.arviointipaiva,
                                            myöntäjäOrganisaatio =
                                                Organisaatio(
                                                    ykiSuoritus.jarjestajanTunnusOid,
                                                ),
                                        ),
                                    osasuoritukset = convertYkiSuoritusToKoskiOsasuoritukset(ykiSuoritus),
                                ),
                            ),
                    ),
                ),
        )

    private fun convertYkiSuoritusToKoskiOsasuoritukset(suoritusEntity: YkiSuoritusEntity): List<Osasuoritus> =
        mapOf(
            "tekstinymmartaminen" to suoritusEntity.tekstinYmmartaminen,
            "kirjoittaminen" to suoritusEntity.kirjoittaminen,
            "puheenymmartaminen" to suoritusEntity.puheenYmmartaminen,
            "puhuminen" to suoritusEntity.puhuminen,
            "rakenteetjasanasto" to suoritusEntity.rakenteetJaSanasto,
            "yleisarvosana" to suoritusEntity.yleisarvosana,
        ).filterValues { it != null }
            .map { (suorituksenNimi, arvosana) ->
                yleisenKielitutkinnonOsa(
                    suorituksenNimi,
                    arvosana!!,
                    suoritusEntity.arviointipaiva,
                )
            }

    private fun yleisenKielitutkinnonOsa(
        suorituksenNimi: String,
        arvosana: Int,
        arviointipaiva: LocalDate,
    ) = Osasuoritus(
        tyyppi =
            KoodistokoodiViite(
                koodiarvo = "yleisenkielitutkinnonosa",
                koodistoUri = "suorituksentyyppi",
            ),
        koulutusmoduuli =
            OsasuoritusKoulutusModuuli(
                tunniste =
                    KoodistokoodiViite(
                        koodiarvo = suorituksenNimi,
                        koodistoUri = "ykisuorituksenosa",
                    ),
            ),
        arviointi =
            listOf(
                Arvosana(
                    arvosana =
                        KoodistokoodiViite(
                            koodiarvo = arvosana.toString(),
                            koodistoUri = "ykiarvosana",
                        ),
                    päivä = arviointipaiva,
                ),
            ),
    )
}
