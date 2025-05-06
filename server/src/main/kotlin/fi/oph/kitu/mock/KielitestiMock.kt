package fi.oph.kitu.mock

import fi.oph.kitu.kotoutumiskoulutus.KielitestiSuoritus
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

fun generateRandomKielitestiSuoritus(): KielitestiSuoritus {
    val oppilas = generateRandomPerson()
    val teacher = generateRandomPerson()

    return KielitestiSuoritus(
        id = null,
        firstNames = oppilas.etunimet,
        lastName = oppilas.sukunimi,
        preferredname = oppilas.kutsumanimi,
        oppijanumero = oppilas.oppijanumero,
        email = oppilas.email,
        timeCompleted = getRandomInstant(LocalDate.of(2000, 1, 1).toInstant()),
        schoolOid = generateRandomOrganizationOid(),
        courseid = (0..999999999).random(),
        coursename = kotoCourses.random(),
        luetunYmmartaminenResult = cefrLanguageLevels.random(),
        kuullunYmmartaminenResult = cefrLanguageLevels.random(),
        puheResult = cefrLanguageLevels.random(),
        kirjoittaminenResult = cefrLanguageLevels.random(),
        teacherEmail = teacher.email,
    )
}
