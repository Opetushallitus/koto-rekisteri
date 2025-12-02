package fi.oph.kitu.oppijanumero

import fi.oph.kitu.removeAtIndex
import fi.oph.kitu.splitAt

object OppijaPermutations {
    fun getPermutations(oppija: Oppija): List<Oppija> =
        getNamePermutations("${oppija.etunimet} ${oppija.sukunimi}".split(" "))
            .flatMap { getSplitPermutations(it) }
            .flatMap { (etunimet, sukunimi) ->
                etunimet.map { kutsumanimi ->
                    Oppija(
                        etunimet = etunimet.joinToString(" "),
                        sukunimi = sukunimi.joinToString(" "),
                        kutsumanimi = kutsumanimi,
                        hetu = oppija.hetu,
                    )
                }
            }

    fun getNamePermutations(names: List<String>): List<List<String>> =
        if (names.size < 2) {
            listOf(names)
        } else {
            names.flatMapIndexed { index, name ->
                val otherNames = names.removeAtIndex(index)
                val permutations = getNamePermutations(otherNames)
                permutations.map { listOf(name) + it }
            }
        }

    fun getSplitPermutations(names: List<String>): List<Pair<List<String>, List<String>>> =
        (1..(names.size - 1)).map { names.splitAt(it) }
}
