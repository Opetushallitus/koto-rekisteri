package fi.oph.kitu.random

import fi.oph.kitu.Oid
import fi.oph.kitu.yki.Sukupuoli
import kotlin.random.Random

data class Person(
    val oppijanumero: Oid,
    val hetu: String,
    val sukupuoli: Sukupuoli,
    val sukunimi: String,
    val etunimet: String,
    val kutsumanimi: String,
    val kansalaisuus: String,
    val katuosoite: String,
    val postinumero: String,
    val postitoimipaikka: String,
    val email: String,
)

fun generateRandomPerson(): Person {
    val sukupuoli = Sukupuoli.entries.toTypedArray().random()
    val oppijanumero = generateRandomUserOid()
    val etunimet = generateRandomFirstnames(sukupuoli)
    val kutsumanimi = if (Random.nextBoolean()) etunimet.first else etunimet.second
    val sukunimi = surnames.random()

    val address = addresses.random()
    val kansalaisuus = countryCodes.random()
    val katunumero = (1..20).random()
    val randomId = (1..10).random()
    val hetu =
        generateRandomSsn(
            sex =
                if (sukupuoli == Sukupuoli.M || sukupuoli == Sukupuoli.N) {
                    sukupuoli
                } else {
                    listOf(Sukupuoli.M, Sukupuoli.N).random()
                },
        )

    return Person(
        oppijanumero = oppijanumero,
        hetu = hetu,
        sukupuoli = sukupuoli,
        sukunimi = sukunimi,
        etunimet = "${etunimet.first} ${etunimet.second}",
        kutsumanimi = kutsumanimi,
        kansalaisuus = kansalaisuus,
        katuosoite = "${address.first} $katunumero",
        postinumero = address.third,
        postitoimipaikka = address.second,
        email = "${etunimet.first}.$sukunimi.$randomId@mock.oph.fi",
    )
}

fun generateRandomFirstnames(sukupuoli: Sukupuoli): Pair<String, String> {
    if (sukupuoli == Sukupuoli.N) {
        return Pair(femaleNames.random(), femaleNames.random())
    } else if (sukupuoli == Sukupuoli.M) {
        return Pair(maleNames.random(), maleNames.random())
    }

    val bothnames =
        mutableListOf<String>().apply {
            addAll(femaleNames)
            addAll(maleNames)
        }

    return Pair(bothnames.random(), bothnames.random())
}
