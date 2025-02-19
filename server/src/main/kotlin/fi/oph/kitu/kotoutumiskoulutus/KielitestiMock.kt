package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.random.cefrLanguages
import fi.oph.kitu.random.cefrOptionalLanguages
import fi.oph.kitu.random.generateRandomOrganizationOid
import fi.oph.kitu.random.generateRandomPerson
import fi.oph.kitu.random.getRandomInstant
import fi.oph.kitu.random.toInstant
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

    return KielitestiSuoritus(
        id = null,
        firstNames = oppilas.etunimet,
        lastName = oppilas.sukunimi,
        preferredname = oppilas.kutsumanimi,
        oppijanumero = oppilas.oppijanumero.toString(),
        email = oppilas.email,
        timeCompleted = getRandomInstant(LocalDate.of(2000, 1, 1).toInstant()),
        schoolOid = generateRandomOrganizationOid(),
        courseid = (0..999999999).random(),
        coursename = kotoCourses.random(),
        luetunYmmartaminenResultSystem = cefrLanguages.random(),
        luetunYmmartaminenResultTeacher = cefrLanguages.random(),
        kuullunYmmartaminenResultSystem = cefrLanguages.random(),
        kuullunYmmartaminenResultTeacher = cefrLanguages.random(),
        puheResultSystem = cefrOptionalLanguages.random(),
        puheResultTeacher = cefrLanguages.random(),
        kirjoittaminenResultSystem = cefrOptionalLanguages.random(),
        kirjottaminenResultTeacher = cefrLanguages.random(),
        totalEvaluationTeacher = cefrLanguages.random(),
        totalEvaluationSystem = cefrLanguages.random(),
    )
}
