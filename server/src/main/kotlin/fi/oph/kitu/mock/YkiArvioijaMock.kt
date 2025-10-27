package fi.oph.kitu.mock

import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import fi.oph.kitu.yki.arvioijat.YkiArvioijaEntity
import fi.oph.kitu.yki.arvioijat.YkiArvioijaTila
import fi.oph.kitu.yki.arvioijat.YkiArviointioikeusEntity
import java.time.LocalDate
import kotlin.random.Random

fun generateRandomYkiArvioijaEntity(): YkiArvioijaEntity {
    val randomTeacher = generateRandomPerson()

    val (rekisteriintuontiaika, ensimmainenRekisterointipaiva, kaudenAlkupaiva, kaudenPaattymispaiva) =
        getRandomLocalDates(
            4,
            LocalDate.of(2000, 1, 1),
            LocalDate.now().minusDays(28),
        )

    return YkiArvioijaEntity(
        id = null,
        arvioijanOppijanumero = randomTeacher.oppijanumero,
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
                    kaudenPaattymispaiva = kaudenPaattymispaiva,
                    jatkorekisterointi = Random.nextBoolean(),
                    tila = YkiArvioijaTila.entries.random(),
                    kieli = Tutkintokieli.entries.random(),
                    tasot = List(Tutkintotaso.entries.size) { Tutkintotaso.entries.random() }.toSet(),
                    ensimmainenRekisterointipaiva = ensimmainenRekisterointipaiva,
                    rekisteriintuontiaika = rekisteriintuontiaika.toOffsetDateTime(),
                ),
            ),
    )
}
