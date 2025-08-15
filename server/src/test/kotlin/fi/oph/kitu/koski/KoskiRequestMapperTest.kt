package fi.oph.kitu.koski

import com.fasterxml.jackson.databind.JsonNode
import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.Oid
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.mock.VktSuoritusMockGenerator
import fi.oph.kitu.mock.generateRandomOppijaOid
import fi.oph.kitu.mock.generateRandomYkiSuoritusEntity
import fi.oph.kitu.mock.getRandomLocalDate
import fi.oph.kitu.oppijanumero.MockOppijanumeroService
import fi.oph.kitu.oppijanumero.OppijanumerorekisteriHenkilo
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.vkt.tiedonsiirtoschema.Henkilosuoritus
import fi.oph.kitu.vkt.tiedonsiirtoschema.Lahdejarjestelma
import fi.oph.kitu.vkt.tiedonsiirtoschema.LahdejarjestelmanTunniste
import fi.oph.kitu.vkt.tiedonsiirtoschema.OidOppija
import fi.oph.kitu.vkt.tiedonsiirtoschema.OidString
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.core.io.ClassPathResource
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDate
import kotlin.random.Random
import kotlin.test.assertEquals

@SpringBootTest
@Import(DBContainerConfiguration::class)
class KoskiRequestMapperTest(
    @Autowired private val postgres: PostgreSQLContainer<*>,
) {
    @Autowired
    lateinit var koskiRequestMapper: KoskiRequestMapper
    private val objectMapper = KoskiRequestMapper.getObjectMapper()

    private val oid: Oid = Oid.parse("1.2.246.562.24.12345678910").getOrThrow()
    private val jarjestajanOrganisaatio = Oid.parse("1.2.246.562.10.12345678910").getOrThrow()

    @Test
    fun `map yki suoritus to koski request`() {
        val suoritus =
            generateRandomYkiSuoritusEntity().copy(
                suorittajanOID = oid,
                suoritusId = 123456,
                tutkintopaiva = LocalDate.of(2025, 1, 1),
                arviointipaiva = LocalDate.of(2025, 1, 3),
                tutkintotaso = Tutkintotaso.PT,
                tutkintokieli = Tutkintokieli.ENG,
                jarjestajanTunnusOid = jarjestajanOrganisaatio,
                tekstinYmmartaminen = 2,
                kirjoittaminen = 2,
                puheenYmmartaminen = 2,
                puhuminen = 2,
                rakenteetJaSanasto = null,
                yleisarvosana = null,
            )
        val koskiRequest = koskiRequestMapper.ykiSuoritusToKoskiRequest(suoritus)
        val expectedJson =
            objectMapper
                .readValue(
                    ClassPathResource("./koski-request-example.json").file,
                    JsonNode::class.java,
                ).toString()
        val koskiRequestJson = objectMapper.writeValueAsString(koskiRequest)
        assertEquals(expectedJson, koskiRequestJson)
    }

    @Test
    fun `map yki suoritus with yleisarvosana to koski request`() {
        val suoritus =
            generateRandomYkiSuoritusEntity().copy(
                suorittajanOID = oid,
                suoritusId = 123456,
                tutkintopaiva = LocalDate.of(2025, 1, 1),
                arviointipaiva = LocalDate.of(2025, 1, 3),
                tutkintotaso = Tutkintotaso.PT,
                tutkintokieli = Tutkintokieli.ENG,
                jarjestajanTunnusOid = jarjestajanOrganisaatio,
                tekstinYmmartaminen = 2,
                kirjoittaminen = 2,
                puheenYmmartaminen = 2,
                puhuminen = 2,
                rakenteetJaSanasto = null,
                yleisarvosana = 2,
            )
        val koskiRequest = koskiRequestMapper.ykiSuoritusToKoskiRequest(suoritus)
        val expectedJson =
            objectMapper
                .readValue(
                    ClassPathResource("./koski-request-with-yleisarvosana.json").file,
                    JsonNode::class.java,
                ).toString()
        val koskiRequestJson = objectMapper.writeValueAsString(koskiRequest)
        assertEquals(expectedJson, koskiRequestJson)
    }

    @Test
    fun `map ykiarvosana for tutkintotaso PT correctly`() {
        val suoritus =
            generateRandomYkiSuoritusEntity().copy(
                tutkintokieli = Tutkintokieli.ENG,
                tutkintotaso = Tutkintotaso.PT,
                tekstinYmmartaminen = 0,
                kirjoittaminen = 0,
                puheenYmmartaminen = 0,
                puhuminen = 0,
                rakenteetJaSanasto = 2,
                yleisarvosana = 1,
            )
        val koskiSuoritus =
            koskiRequestMapper
                .ykiSuoritusToKoskiRequest(
                    suoritus,
                )!!
                .opiskeluoikeudet
                .first()
                .suoritukset
                .first()
        assertEquals(Koodisto.YkiArvosana.PT1.toKoski(), koskiSuoritus.yleisarvosana)
        val arvosanat =
            koskiSuoritus.osasuoritukset.associate {
                it.koulutusmoduuli.tunniste.koodiarvo to it.arviointi.first().arvosana
            }
        assertEquals(Koodisto.YkiArvosana.ALLE1.toKoski(), arvosanat["tekstinymmartaminen"])
        assertEquals(Koodisto.YkiArvosana.ALLE1.toKoski(), arvosanat["kirjoittaminen"])
        assertEquals(Koodisto.YkiArvosana.ALLE1.toKoski(), arvosanat["puheenymmartaminen"])
        assertEquals(Koodisto.YkiArvosana.ALLE1.toKoski(), arvosanat["puhuminen"])
        assertEquals(Koodisto.YkiArvosana.PT2.toKoski(), arvosanat["rakenteetjasanasto"])
    }

    @Test
    fun `map ykiarvosana for tutkintotaso KT correctly`() {
        val suoritus =
            generateRandomYkiSuoritusEntity().copy(
                tutkintokieli = Tutkintokieli.SWE,
                tutkintotaso = Tutkintotaso.KT,
                tekstinYmmartaminen = 0,
                kirjoittaminen = 2,
                puheenYmmartaminen = 3,
                puhuminen = 4,
                rakenteetJaSanasto = 1,
                yleisarvosana = 0,
            )
        val koskiSuoritus =
            koskiRequestMapper
                .ykiSuoritusToKoskiRequest(
                    suoritus,
                )!!
                .opiskeluoikeudet
                .first()
                .suoritukset
                .first()
        assertEquals(Koodisto.YkiArvosana.ALLE3.toKoski(), koskiSuoritus.yleisarvosana)
        val arvosanat =
            koskiSuoritus.osasuoritukset.associate {
                it.koulutusmoduuli.tunniste.koodiarvo to it.arviointi.first().arvosana
            }

        assertEquals(Koodisto.YkiArvosana.ALLE3.toKoski(), arvosanat["tekstinymmartaminen"])
        assertEquals(Koodisto.YkiArvosana.ALLE3.toKoski(), arvosanat["kirjoittaminen"])
        assertEquals(Koodisto.YkiArvosana.KT3.toKoski(), arvosanat["puheenymmartaminen"])
        assertEquals(Koodisto.YkiArvosana.KT4.toKoski(), arvosanat["puhuminen"])
        assertEquals(Koodisto.YkiArvosana.ALLE3.toKoski(), arvosanat["rakenteetjasanasto"])
    }

    @Test
    fun `map ykiarvosana for tutkintotaso YT correctly`() {
        val suoritus =
            generateRandomYkiSuoritusEntity().copy(
                tutkintokieli = Tutkintokieli.SME,
                tutkintotaso = Tutkintotaso.YT,
                tekstinYmmartaminen = 5,
                kirjoittaminen = 6,
                puheenYmmartaminen = 2,
                puhuminen = 3,
                rakenteetJaSanasto = 4,
                yleisarvosana = 0,
            )
        val koskiSuoritus =
            koskiRequestMapper
                .ykiSuoritusToKoskiRequest(
                    suoritus,
                )!!
                .opiskeluoikeudet
                .first()
                .suoritukset
                .first()
        assertEquals(Koodisto.YkiArvosana.ALLE5.toKoski(), koskiSuoritus.yleisarvosana)
        val arvosanat =
            koskiSuoritus.osasuoritukset.associate {
                it.koulutusmoduuli.tunniste.koodiarvo to it.arviointi.first().arvosana
            }

        assertEquals(Koodisto.YkiArvosana.YT5.toKoski(), arvosanat["tekstinymmartaminen"])
        assertEquals(Koodisto.YkiArvosana.YT6.toKoski(), arvosanat["kirjoittaminen"])
        assertEquals(Koodisto.YkiArvosana.ALLE5.toKoski(), arvosanat["puheenymmartaminen"])
        assertEquals(Koodisto.YkiArvosana.ALLE5.toKoski(), arvosanat["puhuminen"])
        assertEquals(Koodisto.YkiArvosana.ALLE5.toKoski(), arvosanat["rakenteetjasanasto"])
    }

    @Test
    fun `drop whole suoritus if even osakoe is vilppi`() {
        val suoritus =
            generateRandomYkiSuoritusEntity().copy(
                tutkintokieli = Tutkintokieli.SME,
                tutkintotaso = Tutkintotaso.YT,
                tekstinYmmartaminen = 10,
            )
        val koskiSuoritus = koskiRequestMapper.ykiSuoritusToKoskiRequest(suoritus)
        assertEquals(null, koskiSuoritus)
    }

    @Test
    fun `drop whole suoritus if even osakoe is keskeytetty`() {
        val suoritus =
            generateRandomYkiSuoritusEntity().copy(
                tutkintokieli = Tutkintokieli.SME,
                tutkintotaso = Tutkintotaso.YT,
                tekstinYmmartaminen = 11,
            )
        val koskiSuoritus = koskiRequestMapper.ykiSuoritusToKoskiRequest(suoritus)
        assertEquals(null, koskiSuoritus)
    }

    @Test
    fun `map vkt erinomainen suoritus to koski`() {
        val generator = VktSuoritusMockGenerator(0)
        val random = Random(0)

        val suoritus =
            VktSuoritus(
                taitotaso = Koodisto.VktTaitotaso.Erinomainen,
                kieli = Koodisto.Tutkintokieli.FIN,
                suorituksenVastaanottaja = null,
                suorituspaikkakunta = "091",
                osat =
                    generator.randomOsakokeet(
                        Koodisto.VktTaitotaso.Erinomainen,
                        getRandomLocalDate(
                            LocalDate.of(2000, 1, 1),
                            LocalDate.of(2025, 1, 1),
                            random,
                        ),
                    ),
                lahdejarjestelmanId =
                    LahdejarjestelmanTunniste(
                        "vkt.0",
                        Lahdejarjestelma.KIOS,
                    ),
            )

        val henkiloOid = generateRandomOppijaOid(Random(0))
        val henkilosuoritus =
            Henkilosuoritus(
                henkilo =
                    OidOppija(
                        oid = OidString.from(henkiloOid),
                        etunimet = "Keijo",
                        sukunimi = "Keijunen",
                    ),
                suoritus = suoritus,
            )

        val onr = MockOppijanumeroService.build()
        val koskiSuoritus = koskiRequestMapper.vktSuoritusToKoskiRequest(henkilosuoritus, onr)

        assertNotNull(koskiSuoritus)

        val vahvistus =
            koskiSuoritus
                .opiskeluoikeudet
                .firstOrNull()
                ?.suoritukset
                ?.firstOrNull()
                ?.vahvistus as? KoskiRequest.Opiskeluoikeus.KielitutkintoSuoritus.VahvistusHenkiloilla

        assertEquals(
            "Kielitutkintolautakunta",
            vahvistus?.myöntäjäHenkilöt?.firstOrNull()?.nimi,
        )

        val json = objectMapper.writeValueAsString(koskiSuoritus)

        println("JSON: $json")
    }

    @Test
    fun `map vkt hyva ja tyydyttava suoritus to koski`() {
        val generator = VktSuoritusMockGenerator(0)
        val random = Random(0)
        val vastaanottajaOid = "1.2.246.562.24.27639300000"

        val suoritus =
            VktSuoritus(
                taitotaso = Koodisto.VktTaitotaso.HyväJaTyydyttävä,
                kieli = Koodisto.Tutkintokieli.FIN,
                suorituksenVastaanottaja = OidString(vastaanottajaOid),
                suorituspaikkakunta = "091",
                osat =
                    generator.randomOsakokeet(
                        Koodisto.VktTaitotaso.HyväJaTyydyttävä,
                        getRandomLocalDate(
                            LocalDate.of(2000, 1, 1),
                            LocalDate.of(2025, 1, 1),
                            random,
                        ),
                        oppilaitos = OidString("1.2.246.562.10.42456023292"),
                    ),
                lahdejarjestelmanId =
                    LahdejarjestelmanTunniste(
                        "vkt.0",
                        Lahdejarjestelma.KIOS,
                    ),
            )

        val henkiloOid = generateRandomOppijaOid(Random(0))
        val henkilosuoritus =
            Henkilosuoritus(
                henkilo =
                    OidOppija(
                        oid = OidString.from(henkiloOid),
                        etunimet = "Keijo",
                        sukunimi = "Keijunen",
                    ),
                suoritus = suoritus,
            )

        val onr =
            MockOppijanumeroService.build(
                henkiloResponse =
                    OppijanumerorekisteriHenkilo(
                        oidHenkilo = vastaanottajaOid,
                        hetu = null,
                        kaikkiHetut = null,
                        passivoitu = null,
                        etunimet = "Vallu",
                        kutsumanimi = "Vallu",
                        sukunimi = "Vastaanottaja",
                        aidinkieli = null,
                        asiointiKieli = null,
                        kansalaisuus = null,
                        kasittelijaOid = null,
                        syntymaaika = null,
                        sukupuoli = null,
                        kotikunta = null,
                        oppijanumero = null,
                        turvakielto = null,
                        eiSuomalaistaHetua = null,
                        yksiloity = null,
                        yksiloityVTJ = null,
                        yksilointiYritetty = null,
                        duplicate = null,
                        created = null,
                        modified = null,
                        vtjsynced = null,
                        yhteystiedotRyhma = null,
                        yksilointivirheet = null,
                        passinumero = null,
                    ),
            )

        val koskiSuoritus = koskiRequestMapper.vktSuoritusToKoskiRequest(henkilosuoritus, onr)

        assertNotNull(koskiSuoritus)

        val vahvistus =
            koskiSuoritus
                .opiskeluoikeudet
                .firstOrNull()
                ?.suoritukset
                ?.firstOrNull()
                ?.vahvistus as? KoskiRequest.Opiskeluoikeus.KielitutkintoSuoritus.VahvistusHenkiloilla

        assertEquals(
            "Vallu Vastaanottaja",
            vahvistus?.myöntäjäHenkilöt?.firstOrNull()?.nimi,
        )

        val json = objectMapper.writeValueAsString(koskiSuoritus)

        println("JSON: $json")
    }
}
