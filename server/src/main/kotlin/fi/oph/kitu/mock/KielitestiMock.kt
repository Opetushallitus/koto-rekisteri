package fi.oph.kitu.mock

import fi.oph.kitu.kotoutumiskoulutus.suoritukset.Arvosana
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.KielitestiSuoritus
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.Testikieli
import java.time.LocalDate

val kotoCourses =
    listOf(
        // Some values are duplicated in order to give them more weight for random selection
        "suomen kielen alkeet",
        "suomen kielen alkeet",
        "suomen kielen alkeet",
        "suomen kielen jatkokurssi",
        "suomen kielen jatkokurssi",
        "suomen kielen jatkokurssi",
        "ruotsin kielen alkeet",
        "ruotsin kielen jatkokurssi",
        "suomen kieli työelämään",
    )

val tehtavapaketit =
    listOf(
        "fi_suomi",
        "sv_svenska",
        null,
    )

fun generateRandomKielitestiSuoritus(): KielitestiSuoritus {
    val oppilas = generateRandomPerson()
    val teacher = generateRandomPerson()

    return KielitestiSuoritus(
        id = null,
        etunimet = oppilas.etunimet,
        sukunimi = oppilas.sukunimi,
        kutsumanimi = oppilas.kutsumanimi,
        oppijanumero = oppilas.oppijanumero,
        email = oppilas.email,
        suoritusaika = getRandomInstant(LocalDate.of(2000, 1, 1).toInstant()),
        oppilaitosOid = generateRandomOrganizationOid(),
        kurssiId = (0..999999999).random(),
        kurssi = kotoCourses.random(),
        luetunYmmartaminen = Arvosana.fromString(cefrLanguageLevels.random()),
        kuullunYmmartaminen = Arvosana.fromString(cefrLanguageLevels.random()),
        puhe = Arvosana.fromString(cefrLanguageLevels.random()),
        kirjoittaminen = Arvosana.fromString(cefrLanguageLevels.random()),
        testikieli = Testikieli.FIN,
        opettajanEmail = teacher.email,
        tehtavapaketti = tehtavapaketit.random(),
    )
}
