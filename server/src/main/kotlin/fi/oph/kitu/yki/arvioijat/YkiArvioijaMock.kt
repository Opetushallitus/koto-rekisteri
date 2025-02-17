package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.random.generateRandomPerson
import fi.oph.kitu.random.getRandomLocalDates
import fi.oph.kitu.random.getRandomOffsetDateTime
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.random.Random

fun generateRandomYkiArviointiEntity(): YkiArvioijaEntity {
    val randomTeacher = generateRandomPerson()

    val rekisteriintuontiaika =
        getRandomOffsetDateTime(
            min = OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.of("Europe/Helsinki")),
            max = OffsetDateTime.now(),
        )

    val (ensimmainenRekisterointipaiva, kaudenAlkupaiva, kaudenPaattymispaiva) =
        getRandomLocalDates(
            3,
            LocalDate.of(2000, 1, 1),
            LocalDate.now().minusDays(28),
        )

    return YkiArvioijaEntity(
        id = null,
        rekisteriintuontiaika = rekisteriintuontiaika,
        arvioijanOppijanumero = randomTeacher.oppijanumero.toString(),
        henkilotunnus = randomTeacher.hetu,
        sukunimi = randomTeacher.sukunimi,
        etunimet = randomTeacher.etunimet,
        sahkopostiosoite = randomTeacher.email,
        katuosoite = randomTeacher.katuosoite,
        postinumero = randomTeacher.postinumero,
        postitoimipaikka = randomTeacher.postitoimipaikka,
        ensimmainenRekisterointipaiva = ensimmainenRekisterointipaiva,
        kaudenAlkupaiva = kaudenAlkupaiva,
        kaudenPaattymispaiva = kaudenPaattymispaiva,
        jatkorekisterointi = Random.nextBoolean(),
        tila = YkiArvioijaTila.entries.random(),
        kieli = Tutkintokieli.entries.random(),
        tasot = List(Tutkintotaso.entries.size) { Tutkintotaso.entries.random() }.toSet(),
    )
}
