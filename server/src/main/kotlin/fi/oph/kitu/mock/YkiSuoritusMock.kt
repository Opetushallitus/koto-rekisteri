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

    return YkiSuoritusEntity(
        id = null,
        suorittajanOID = randomPerson.oppijanumero.toString(),
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
        tutkintokieli = Tutkintokieli.entries.toTypedArray().random(),
        tutkintotaso = Tutkintotaso.entries.random(),
        jarjestajanTunnusOid = generateRandomOrganizationOid().toString(),
        jarjestajanNimi = "${randomPerson.postitoimipaikka}n yliopisto",
        arviointipaiva = arviointipaiva,
        tekstinYmmartaminen = (1..5).random(),
        kirjoittaminen = (1..5).random(),
        rakenteetJaSanasto = (1..5).random(),
        puheenYmmartaminen = (1..5).random(),
        puhuminen = (1..5).random(),
        yleisarvosana = (1..5).random(),
        tarkistusarvioinninSaapumisPvm = tarkistusarvioinninSaapumisPvm,
        tarkistusarvioinninAsiatunnus = (0..9999999999999).random().toString(),
        tarkistusarvioidutOsakokeet = 2,
        arvosanaMuuttui = 1,
        perustelu = listOf("Erinomainen", "Hyvä", "Ihan hyvä", "Tyydyttävä", "Huono").random(),
        tarkistusarvioinninKasittelyPvm = tarkistusarvioinninKasittelyPvm,
    )
}
