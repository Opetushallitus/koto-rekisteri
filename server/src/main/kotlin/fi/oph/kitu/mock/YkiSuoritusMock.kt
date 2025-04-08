package fi.oph.kitu.mock

import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.random.Random

fun generateRandomYkiSuoritusEntity(): YkiSuoritusEntity {
    val randomPerson = generateRandomPerson()

    val (
        tutkintopaiva,
        arviointipaiva,
        tarkistusarvioinninSaapumisPvm,
        tarkistusarvioinninKasittelyPvm,
        lastModifiedLocalDate,
    ) =
        getRandomLocalDates(
            5,
            LocalDate.of(2000, 1, 1),
            LocalDate.now().minusDays(28),
        ).sorted()

    val lastModified = lastModifiedLocalDate.atStartOfDay().toInstant(ZoneOffset.UTC)

    val tutkintotaso = Tutkintotaso.entries.random()
    val maxArvosana =
        when (tutkintotaso) {
            Tutkintotaso.PT -> 2
            Tutkintotaso.KT -> 4
            Tutkintotaso.YT -> 6
        }

    return YkiSuoritusEntity(
        id = null,
        suorittajanOID = randomPerson.oppijanumero,
        hetu = randomPerson.hetu,
        sukupuoli = randomPerson.sukupuoli,
        sukunimi = randomPerson.sukunimi,
        etunimet = randomPerson.etunimet,
        kansalaisuus = randomPerson.kansalaisuus,
        katuosoite = randomPerson.katuosoite,
        postinumero = randomPerson.postinumero,
        postitoimipaikka = randomPerson.postitoimipaikka,
        email = randomPerson.email,
        suoritusId = Random.nextInt(100000, 999999),
        lastModified = lastModified,
        tutkintopaiva = tutkintopaiva,
        tutkintokieli =
            Tutkintokieli.entries
                .minus(Tutkintokieli.legacyEntries())
                .toTypedArray()
                .random(),
        tutkintotaso = tutkintotaso,
        jarjestajanTunnusOid = generateRandomOrganizationOid(),
        jarjestajanNimi = "${randomPerson.postitoimipaikka}n yliopisto",
        arviointipaiva = arviointipaiva,
        tekstinYmmartaminen = (0..maxArvosana).random(),
        kirjoittaminen = (0..maxArvosana).random(),
        rakenteetJaSanasto = (0..maxArvosana).random(),
        puheenYmmartaminen = (0..maxArvosana).random(),
        puhuminen = (0..maxArvosana).random(),
        yleisarvosana = (0..maxArvosana).random(),
        tarkistusarvioinninSaapumisPvm = tarkistusarvioinninSaapumisPvm,
        tarkistusarvioinninAsiatunnus = (0..9999999999999).random().toString(),
        tarkistusarvioidutOsakokeet = 2,
        arvosanaMuuttui = 1,
        perustelu = listOf("Erinomainen", "Hyvä", "Ihan hyvä", "Tyydyttävä", "Huono").random(),
        tarkistusarvioinninKasittelyPvm = tarkistusarvioinninKasittelyPvm,
        koskiOpiskeluoikeus = null,
    )
}
