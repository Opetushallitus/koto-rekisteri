package fi.oph.kitu.schema

import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.tiedontuontischema.Henkilo
import fi.oph.kitu.tiedontuontischema.Henkilosuoritus
import fi.oph.kitu.tiedontuontischema.Lahdejarjestelma
import fi.oph.kitu.tiedontuontischema.LahdejarjestelmanTunniste
import fi.oph.kitu.tiedontuontischema.VktSuoritus
import fi.oph.kitu.tiedontuontischema.YkiJarjestaja
import fi.oph.kitu.tiedontuontischema.YkiOsa
import fi.oph.kitu.tiedontuontischema.YkiSuoritus
import fi.oph.kitu.util.defaultObjectMapper
import fi.oph.kitu.util.result.getOrThrow
import fi.oph.kitu.vkt.VktKirjoittamisenKoe
import fi.oph.kitu.vkt.VktPuheenYmmartamisenKoe
import fi.oph.kitu.vkt.VktPuhumisenKoe
import fi.oph.kitu.vkt.VktSuoritusEntity
import fi.oph.kitu.vkt.VktTekstinYmmartamisenKoe
import fi.oph.kitu.yki.Arviointitila
import fi.oph.kitu.yki.Sukupuoli
import fi.oph.kitu.yki.TutkinnonOsa
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import fi.oph.kitu.yki.suoritukset.Todistuskieli
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.time.LocalDate
import kotlin.test.assertEquals

class SchemaTests {
    companion object {
        val vktTutkintopaiva = LocalDate.of(2025, 1, 1)
        val vktHenkilosuoritus =
            Henkilosuoritus(
                henkilo =
                    Henkilo(
                        oid = Oid.parse("1.2.246.562.24.10691606777").getOrThrow(),
                        etunimet = "Kalle",
                        sukunimi = "Testaaja",
                    ),
                suoritus =
                    VktSuoritus(
                        osat =
                            listOf(
                                VktKirjoittamisenKoe(vktTutkintopaiva),
                                VktTekstinYmmartamisenKoe(vktTutkintopaiva),
                                VktPuhumisenKoe(vktTutkintopaiva),
                                VktPuheenYmmartamisenKoe(vktTutkintopaiva),
                            ),
                        suorituksenVastaanottaja = null,
                        suorituspaikkakunta = null,
                        taitotaso = Koodisto.VktTaitotaso.Erinomainen,
                        kieli = Koodisto.Tutkintokieli.FIN,
                        lahdejarjestelmanId =
                            LahdejarjestelmanTunniste(
                                id = "123",
                                lahde = Lahdejarjestelma.KIOS,
                            ),
                    ),
            )

        val ykiHenkilosuoritus =
            Henkilosuoritus(
                henkilo =
                    Henkilo(
                        oid = Oid.parse("1.2.246.562.24.20281155246").getOrThrow(),
                        hetu = "010180-9026",
                        etunimet = "Ranja Testi",
                        sukunimi = "Öhman-Testi",
                        sukupuoli = Sukupuoli.N,
                        kansalaisuus = "EST",
                        katuosoite = "Testikuja 5",
                        postinumero = "41000",
                        postitoimipaikka = "Testilä",
                        email = "testi@test.fi",
                    ),
                suoritus =
                    YkiSuoritus(
                        tutkintotaso = Tutkintotaso.KT,
                        kieli = Tutkintokieli.FIN,
                        todistuskieli = Todistuskieli.FIN,
                        jarjestaja =
                            YkiJarjestaja(
                                oid = Oid.parse("1.2.246.562.10.14893989377").getOrThrow(),
                                nimi = "Soveltavan kielentutkimuksen keskus",
                            ),
                        tutkintopaiva = LocalDate.of(2020, 1, 1),
                        arviointipaiva = LocalDate.of(2020, 1, 1),
                        arviointitila = Arviointitila.ARVIOITU,
                        osat =
                            listOf(
                                YkiOsa(
                                    tyyppi = TutkinnonOsa.puheenYmmartaminen,
                                    arvosana = 3,
                                ),
                                YkiOsa(
                                    tyyppi = TutkinnonOsa.puhuminen,
                                    arvosana = 3,
                                ),
                            ),
                        tarkistusarviointi = null,
                        lahdejarjestelmanId =
                            LahdejarjestelmanTunniste(
                                id = "666",
                                lahde = Lahdejarjestelma.Solki,
                            ),
                        internalId = null,
                        koskiOpiskeluoikeusOid = null,
                        koskiSiirtoKasitelty = false,
                    ),
            )
    }

    @Test
    fun `VKTSuoritus can be serialized and deserialized`() {
        val json = defaultObjectMapper.writeValueAsString(vktHenkilosuoritus)

        print(json)

        val decodedData = defaultObjectMapper.readValue(json, Henkilosuoritus::class.java)

        assertEquals(expected = vktHenkilosuoritus, actual = decodedData)
    }

    @Test
    fun `Conversion from VKT Henkilösuoritus to database entity and back does not change content`() {
        val entity = VktSuoritusEntity.from(vktHenkilosuoritus)

        val restoredData = entity.toHenkilosuoritus()
        assertEquals(expected = vktHenkilosuoritus, actual = restoredData)
    }

    @Test
    fun `YKI-json deserialisoituu Henkilosuoritukseksi`() {
        val json = ClassPathResource("./yki-tiedonsiirto-example.json").file
        val data = defaultObjectMapper.readValue(json, Henkilosuoritus::class.java)
        val suoritus = data.suoritus as YkiSuoritus

        assertEquals("010180-9026", data.henkilo.hetu)
        assertEquals(Lahdejarjestelma.Solki, suoritus.lahdejarjestelmanId.lahde)
        assertEquals(6, suoritus.osat.size)
    }

    @Test
    fun `YKI-henkilosuoritus pysyy samana jsonin kautta kaytyaan`() {
        val json = defaultObjectMapper.writeValueAsString(ykiHenkilosuoritus)
        val data = defaultObjectMapper.readValue(json, Henkilosuoritus::class.java)

        assertEquals(ykiHenkilosuoritus, data)
    }
}
