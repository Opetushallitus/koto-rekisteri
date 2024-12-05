package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.kotoutumiskoulutus.KoealustaSuorituksetResponse.User
import fi.oph.kitu.kotoutumiskoulutus.KoealustaSuorituksetResponse.User.Completion
import fi.oph.kitu.oppijanumero.Oppija
import fi.oph.kitu.oppijanumero.OppijanumeroException
import fi.oph.kitu.oppijanumero.OppijanumeroService
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class KoealustaMappingService(
    private val oppijanumeroService: OppijanumeroService,
) {
    fun convertToEntity(suorituksetResponse: KoealustaSuorituksetResponse): List<KielitestiSuoritus> {
        val exceptions = mutableListOf<OppijanumeroException>()

        val entity =
            suorituksetResponse.users.flatMap { user ->
                val oppijanumero =
                    try {
                        oppijanumeroService.getOppijanumero(toOppija(user))
                    } catch (ex: OppijanumeroException) {
                        exceptions.add(ex)
                        // The value is irrelevant, because if (any) error was thrown here,
                        // the conversion will throw custom exception in this method.
                        ""
                    }

                user.completions.map { completion ->
                    completionToEntity(
                        user,
                        oppijanumero,
                        completion,
                    )
                }
            }

        if (exceptions.isNotEmpty()) {
            throw KoealustaMappingServiceException(
                "Unable to convert into list of KielitestiSuoritus, because there were ${exceptions.size} exceptions from oppijanumero-service.",
                exceptions,
            )
        }

        return entity
    }

    fun toOppija(koealustaUser: User) =
        Oppija(
            etunimet = koealustaUser.firstname,
            hetu = koealustaUser.SSN,
            kutsumanimi = koealustaUser.preferredname,
            sukunimi = koealustaUser.lastname,
            oppijanumero = koealustaUser.OID.ifEmpty { null }, // Nullify empty values
        )

    fun getLuetunYmmartaminen(completion: Completion) =
        completion.results.find { it.name == "luetun ymm\u00e4rt\u00e4minen" }!!

    fun getKuullunYmmartaminen(completion: Completion) =
        completion.results.find { it.name == "kuullun ymm\u00e4rt\u00e4minen" }!!

    fun getPuhe(completion: Completion) = completion.results.find { it.name == "puhe" }!!

    fun getKirjoittaminen(completion: Completion) = completion.results.find { it.name == "kirjoittaminen" }!!

    fun completionToEntity(
        user: User,
        oppijanumero: String,
        completion: Completion,
    ): KielitestiSuoritus {
        val luetunYmmartaminen = getLuetunYmmartaminen(completion)
        val kuullunYmmartaminen = getKuullunYmmartaminen(completion)
        val puhe = getPuhe(completion)
        val kirjoittaminen = getKirjoittaminen(completion)

        return KielitestiSuoritus(
            firstName = user.firstname,
            lastName = user.lastname,
            preferredname = user.preferredname,
            email = user.email,
            oppijanumero = oppijanumero,
            timeCompleted = Instant.ofEpochSecond(completion.timecompleted),
            courseid = completion.courseid,
            coursename = completion.coursename,
            luetunYmmartaminenResultSystem = luetunYmmartaminen.quiz_result_system,
            luetunYmmartaminenResultTeacher = luetunYmmartaminen.quiz_result_teacher,
            kuullunYmmartaminenResultSystem = kuullunYmmartaminen.quiz_result_system,
            kuullunYmmartaminenResultTeacher = kuullunYmmartaminen.quiz_result_teacher,
            puheResultSystem = puhe.quiz_result_system,
            puheResultTeacher = puhe.quiz_result_teacher,
            kirjoittaminenResultSystem = kirjoittaminen.quiz_result_system,
            kirjottaminenResultTeacher = kirjoittaminen.quiz_result_teacher,
            totalEvaluationTeacher = completion.total_evaluation_teacher,
            totalEvaluationSystem = completion.total_evaluation_system,
        )
    }

    class KoealustaMappingServiceException(
        msg: String,
        val oppijanumeroExceptions: List<OppijanumeroException>,
    ) : Exception(msg)
}
