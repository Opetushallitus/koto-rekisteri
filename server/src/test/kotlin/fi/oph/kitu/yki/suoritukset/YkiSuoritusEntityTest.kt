package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.oid.Oid
import fi.oph.kitu.tiedontuontischema.Henkilo
import fi.oph.kitu.tiedontuontischema.Henkilosuoritus
import fi.oph.kitu.tiedontuontischema.Lahdejarjestelma
import fi.oph.kitu.tiedontuontischema.LahdejarjestelmanTunniste
import fi.oph.kitu.tiedontuontischema.YkiJarjestaja
import fi.oph.kitu.tiedontuontischema.YkiOsa
import fi.oph.kitu.tiedontuontischema.YkiSuoritus
import fi.oph.kitu.util.result.getOrThrow
import fi.oph.kitu.yki.Arviointitila
import fi.oph.kitu.yki.Sukupuoli
import fi.oph.kitu.yki.TutkinnonOsa
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import org.junit.jupiter.api.assertAll
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class YkiSuoritusEntityTest {
    @Test
    fun `from säilyttää arvostelemattomat osakokeet ilmoittautumiselle`() {
        val henkilosuoritus =
            Henkilosuoritus(
                henkilo =
                    Henkilo(
                        oid = Oid.parse("1.2.246.562.24.20281155246").getOrThrow(),
                        etunimet = "Ranja Testi",
                        sukunimi = "Öhman-Testi",
                        hetu = "010180-9026",
                        sukupuoli = Sukupuoli.N,
                        kansalaisuus = "EST",
                        katuosoite = "Testikuja 5",
                        postinumero = "40100",
                        postitoimipaikka = "Testilä",
                        email = "testi@testi.fi",
                    ),
                suoritus =
                    YkiSuoritus(
                        tutkintotaso = Tutkintotaso.YT,
                        kieli = Tutkintokieli.FIN,
                        todistuskieli = Todistuskieli.FIN,
                        jarjestaja =
                            YkiJarjestaja(
                                oid = Oid.parse("1.2.246.562.10.14893989377").getOrThrow(),
                                nimi = "Jyväskylän yliopisto",
                            ),
                        tutkintopaiva = LocalDate.of(2024, 9, 1),
                        arviointipaiva = null,
                        arviointitila = Arviointitila.ILMOITTAUTUNUT,
                        osat =
                            listOf(
                                YkiOsa(tyyppi = TutkinnonOsa.tekstinYmmartaminen, arvosana = null),
                                YkiOsa(tyyppi = TutkinnonOsa.kirjoittaminen, arvosana = null),
                                YkiOsa(tyyppi = TutkinnonOsa.puhuminen, arvosana = null),
                            ),
                        lahdejarjestelmanId =
                            LahdejarjestelmanTunniste(
                                id = "183424",
                                lahde = Lahdejarjestelma.Solki,
                            ),
                    ),
            )

        val entity = YkiSuoritusEntity.from(henkilosuoritus)

        assertAll(
            fun() =
                assertEquals(
                    setOf(TutkinnonOsa.PU, TutkinnonOsa.KI, TutkinnonOsa.TY),
                    entity.ilmoitetutOsakokeet,
                    "kaikki ilmoittautumisen osakokeet säilyvät",
                ),
            fun() =
                assertEquals(
                    listOf(TutkinnonOsa.PU, TutkinnonOsa.KI, TutkinnonOsa.TY),
                    entity.osakokeet().map { it.tyyppi },
                    "osakokeet säilyvät vaikka arvosanaa ei ole",
                ),
            fun() = assertTrue(entity.osakokeet().all { it.arvosana == null }, "osakokeilla ei ole arvosanaa"),
            fun() = assertNull(entity.yleisarvosana),
            fun() = assertNull(entity.tekstinYmmartaminen),
        )
    }
}
