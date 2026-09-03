package fi.oph.kitu.dev.mockdata

import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import fi.oph.kitu.yki.arvioijat.Arviointikausi
import fi.oph.kitu.yki.arvioijat.YkiArvioijaEntity
import fi.oph.kitu.yki.arvioijat.YkiArviointioikeusEntity
import java.time.LocalDate
import kotlin.random.Random

fun generateRandomYkiArvioijaEntity(): YkiArvioijaEntity {
    val randomTeacher = generateRandomPerson()

    val (rekisteriintuontiaika, ensimmainenRekisterointipaiva) =
        getRandomLocalDates(
            2,
            LocalDate.of(2000, 1, 1),
            LocalDate.now().minusDays(28),
        )

    // Tila lasketaan kaudesta, joten kauden on oltava johdonmukainen: alkupaiva arvotaan
    // menneisyyden ja tulevaisuuden valilta, jotta dev-datassa esiintyvat kaikki kolme tilaa.
    val kaudenAlkupaiva =
        getRandomLocalDate(
            LocalDate.now().minusYears(Arviointikausi.KAUDEN_PITUUS_VUOSINA + 1),
            LocalDate.now().plusMonths(6),
        )

    return YkiArvioijaEntity(
        id = null,
        arvioijaOid = randomTeacher.oppijanumero,
        henkilotunnus = randomTeacher.hetu,
        sukunimi = randomTeacher.sukunimi,
        etunimet = randomTeacher.etunimet,
        sahkopostiosoite = randomTeacher.email,
        katuosoite = randomTeacher.katuosoite,
        postinumero = randomTeacher.postinumero,
        postitoimipaikka = randomTeacher.postitoimipaikka,
        arviointioikeudet =
            listOf(
                YkiArviointioikeusEntity(
                    id = null,
                    arvioijaId = null,
                    kaudenAlkupaiva = kaudenAlkupaiva,
                    kaudenPaattymispaiva = Arviointikausi.paattymispaiva(kaudenAlkupaiva),
                    jatkorekisterointi = Random.nextBoolean(),
                    tila = null,
                    kieli = Tutkintokieli.entries.random(),
                    tasot = List(Tutkintotaso.entries.size) { Tutkintotaso.entries.random() }.toSet(),
                    ensimmainenRekisterointipaiva = ensimmainenRekisterointipaiva,
                    rekisteriintuontiaika = rekisteriintuontiaika.toOffsetDateTime(),
                ),
            ),
    )
}
