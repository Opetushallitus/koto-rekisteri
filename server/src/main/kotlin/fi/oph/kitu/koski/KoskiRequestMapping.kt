package fi.oph.kitu.koski

import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import java.time.LocalDate

class KoskiRequestMapping {
    fun convertToKoskiRequest(ykiSuoritus: YkiSuoritusEntity): KoskiRequest =
        KoskiRequest(
            henkilö = Henkilo(oid = ykiSuoritus.suorittajanOID),
            opiskeluoikeudet =
                listOf(
                    KoskiOpiskeluoikeus(
                        lähdejärjestelmänId =
                            LahdeJarjestelmaId(
                                id = ykiSuoritus.suoritusId.toString(),
                                lähdejärjestelmä =
                                    KoodistokoodiViite(
                                        koodiarvo = "kielitutkintorekisteri",
                                        koodistoUri = "lahdejarjestelma",
                                    ),
                            ),
                        tyyppi =
                            KoodistokoodiViite(
                                koodiarvo = "kielitutkinto",
                                koodistoUri = "opiskeluoikeudentyyppi",
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
                                                    koodiarvo = "pt",
                                                    koodistoUri = "ykitutkintotaso",
                                                ),
                                            kieli =
                                                KoodistokoodiViite(
                                                    koodiarvo = "ENG",
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

    private fun convertYkiSuoritusToKoskiOsasuoritukset(suoritusEntity: YkiSuoritusEntity): List<Osasuoritus> {
        val osasuoritukset = mutableListOf<Osasuoritus>()
        if (suoritusEntity.tekstinYmmartaminen != null) {
            osasuoritukset.add(
                yleisenKielitutkinnonOsa(
                    "tekstinymmartaminen",
                    suoritusEntity.tekstinYmmartaminen,
                    suoritusEntity.arviointipaiva,
                ),
            )
        }
        if (suoritusEntity.kirjoittaminen != null) {
            osasuoritukset.add(
                yleisenKielitutkinnonOsa(
                    "kirjoittaminen",
                    suoritusEntity.kirjoittaminen,
                    suoritusEntity.arviointipaiva,
                ),
            )
        }
        if (suoritusEntity.rakenteetJaSanasto != null) {
            osasuoritukset.add(
                yleisenKielitutkinnonOsa(
                    "rakenteetjasanasto",
                    suoritusEntity.rakenteetJaSanasto,
                    suoritusEntity.arviointipaiva,
                ),
            )
        }
        if (suoritusEntity.puheenYmmartaminen != null) {
            osasuoritukset.add(
                yleisenKielitutkinnonOsa(
                    "puheenymmartaminen",
                    suoritusEntity.puheenYmmartaminen,
                    suoritusEntity.arviointipaiva,
                ),
            )
        }
        if (suoritusEntity.puhuminen != null) {
            osasuoritukset.add(
                yleisenKielitutkinnonOsa(
                    "puhuminen",
                    suoritusEntity.puhuminen,
                    suoritusEntity.arviointipaiva,
                ),
            )
        }
        if (suoritusEntity.yleisarvosana != null) {
            osasuoritukset.add(
                yleisenKielitutkinnonOsa(
                    "yleisarvosana",
                    suoritusEntity.yleisarvosana,
                    suoritusEntity.arviointipaiva,
                ),
            )
        }
        return osasuoritukset
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
