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
        val exceptions = mutableListOf<Throwable>()

        val suoritukset =
            suorituksetResponse.users.flatMap { user ->
                val oppijanumero =
                    try {
                        oppijanumeroService.getOppijanumero(toOppija(user))
                    } catch (ex: OppijanumeroException) {
                        exceptions.add(ex)
                        // The value is irrelevant, because if (any) error was thrown here,
                        // the conversion will throw custom exception in this method.
                        ""
                    } catch (ex: Error) {
                        exceptions.add(ex)
                        ""
                    }

                user.completions.flatMap { completion ->
                    try {
                        listOf(
                            completionToEntity(
                                user,
                                oppijanumero,
                                completion,
                            ),
                        )
                    } catch (ex: Error) {
                        exceptions.add(ex)
                        emptyList()
                    }
                }
            }

        if (exceptions.isNotEmpty()) {
            throw Error.ValidationFailure(
                "Unable to convert into list of KielitestiSuoritus, because there were ${exceptions.size} validation errors.",
                exceptions
                    .flatMap {
                        if (it is OppijanumeroException) {
                            listOf(it)
                        } else {
                            emptyList()
                        }
                    }.toList(),
                exceptions
                    .flatMap {
                        if (it is Error.Validation) {
                            listOf(it)
                        } else {
                            emptyList()
                        }
                    }.toList(),
            )
        }

        return suoritukset
    }

    fun toOppija(koealustaUser: User): Oppija {
        if (koealustaUser.SSN.isNullOrEmpty()) {
            throw Error.Validation.MissingField("SSN", koealustaUser.userid)
        }
        if (koealustaUser.preferredname.isNullOrEmpty()) {
            throw Error.Validation.MissingField("preferredname", koealustaUser.userid)
        }

        return Oppija(
            etunimet = koealustaUser.firstnames,
            hetu = koealustaUser.SSN,
            kutsumanimi = koealustaUser.preferredname,
            sukunimi = koealustaUser.lastname,
            oppijanumero = koealustaUser.oppijanumero?.ifEmpty { null }, // Nullify empty values
        )
    }

    fun getLuetunYmmartaminen(
        userId: Int,
        completion: Completion,
    ): Completion.Result {
        val result =
            completion
                .results
                .find { it.name == "luetun ymm\u00e4rt\u00e4minen" }!!
        if (result.quiz_result_system.isNullOrEmpty()) {
            throw Error.Validation.MissingSystemResult(
                userId,
                completion.coursename,
            )
        }
        if (result.quiz_result_teacher.isNullOrEmpty()) {
            throw Error.Validation.MissingTeacherResult(
                userId,
                completion.coursename,
            )
        }

        return result
    }

    fun getKuullunYmmartaminen(
        userId: Int,
        completion: Completion,
    ): Completion.Result {
        val result =
            completion
                .results
                .find { it.name == "kuullun ymm\u00e4rt\u00e4minen" }!!
        if (result.quiz_result_system.isNullOrEmpty()) {
            throw Error.Validation.MissingSystemResult(
                userId,
                completion.coursename,
            )
        }
        if (result.quiz_result_teacher.isNullOrEmpty()) {
            throw Error.Validation.MissingTeacherResult(
                userId,
                completion.coursename,
            )
        }

        return result
    }

    fun getPuhe(
        userId: Int,
        completion: Completion,
    ): Completion.Result {
        val result =
            completion
                .results
                .find { it.name == "puhe" }!!
        // NOTE: It is OK to have null system result on "puhe"
        if (result.quiz_result_teacher.isNullOrEmpty()) {
            throw Error.Validation.MissingTeacherResult(
                userId,
                completion.coursename,
            )
        }

        return result
    }

    fun getKirjoittaminen(
        userId: Int,
        completion: Completion,
    ): Completion.Result {
        val result =
            completion
                .results
                .find { it.name == "kirjoittaminen" }!!
        // NOTE: It is OK to have null system result on "kirjoittaminen"
        if (result.quiz_result_teacher.isNullOrEmpty()) {
            throw Error.Validation.MissingTeacherResult(
                userId,
                completion.coursename,
            )
        }

        return result
    }

    fun completionToEntity(
        user: User,
        oppijanumero: String,
        completion: Completion,
    ): KielitestiSuoritus {
        val luetunYmmartaminen = getLuetunYmmartaminen(user.userid, completion)
        val kuullunYmmartaminen = getKuullunYmmartaminen(user.userid, completion)
        val puhe = getPuhe(user.userid, completion)
        val kirjoittaminen = getKirjoittaminen(user.userid, completion)

        if (user.preferredname.isNullOrEmpty()) {
            throw Error.Validation.MissingField("preferred name", user.userid)
        }

        return KielitestiSuoritus(
            firstNames = user.firstnames,
            lastName = user.lastname,
            preferredname = user.preferredname,
            email = user.email,
            oppijanumero = oppijanumero,
            timeCompleted = Instant.ofEpochSecond(completion.timecompleted),
            schoolOid = completion.schoolOID,
            courseid = completion.courseid,
            coursename = completion.coursename,
            luetunYmmartaminenResultSystem = luetunYmmartaminen.quiz_result_system!!,
            luetunYmmartaminenResultTeacher = luetunYmmartaminen.quiz_result_teacher!!,
            kuullunYmmartaminenResultSystem = kuullunYmmartaminen.quiz_result_system!!,
            kuullunYmmartaminenResultTeacher = kuullunYmmartaminen.quiz_result_teacher!!,
            puheResultSystem = puhe.quiz_result_system,
            puheResultTeacher = puhe.quiz_result_teacher!!,
            kirjoittaminenResultSystem = kirjoittaminen.quiz_result_system,
            kirjottaminenResultTeacher = kirjoittaminen.quiz_result_teacher!!,
            totalEvaluationTeacher = completion.total_evaluation_teacher,
            totalEvaluationSystem = completion.total_evaluation_system,
        )
    }

    sealed class Error(
        message: String,
    ) : Exception(message) {
        class ValidationFailure(
            message: String,
            val oppijanumeroExceptions: List<OppijanumeroException>,
            val validationErrors: List<Validation>,
        ) : Error(message)

        sealed class Validation(
            val userId: Int,
            message: String,
        ) : Error(message) {
            class MissingSystemResult(
                userId: Int,
                courseName: String,
            ) : Validation(
                    userId,
                    "Unexpectedly missing quiz system result on course \"$courseName\" for user \"$userId\"",
                )

            class MissingTeacherResult(
                userId: Int,
                courseName: String,
            ) : Validation(
                    userId,
                    "Unexpectedly missing quiz teacher result on course \"$courseName\" for user \"$userId\"",
                )

            class MissingField(
                field: String,
                userId: Int,
            ) : Validation(userId, "Missing student \"$field\" for user \"$userId\"")
        }
    }
}
