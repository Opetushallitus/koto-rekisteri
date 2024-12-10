package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.kotoutumiskoulutus.KoealustaSuorituksetResponse.User
import fi.oph.kitu.kotoutumiskoulutus.KoealustaSuorituksetResponse.User.Completion
import fi.oph.kitu.oppijanumero.OppijanumeroService
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class KoealustaMappingService(
    private val oppijanumeroService: OppijanumeroService,
) {
    fun suorituksetToEntity(suorituksetResponse: KoealustaSuorituksetResponse): List<KielitestiSuoritus> =
        suorituksetResponse.users.flatMap { user ->
            val oppijanumero = getOppijanumero(user)
            user.completions.map { completion ->
                completionToEntity(
                    user,
                    oppijanumero,
                    completion,
                )
            }
        }

    fun getOppijanumero(user: User) =
        oppijanumeroService.getOppijanumero(
            etunimet = user.firstname,
            sukunimi = user.lastname,
            hetu = user.SSN,
            kutsumanimi = user.preferredname,
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
}
