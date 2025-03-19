package fi.oph.kitu.kotoutumiskoulutus

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.oph.kitu.Oid
import fi.oph.kitu.flatMap
import fi.oph.kitu.kotoutumiskoulutus.KoealustaSuorituksetResponse.User
import fi.oph.kitu.kotoutumiskoulutus.KoealustaSuorituksetResponse.User.Completion
import fi.oph.kitu.oppijanumero.Oppija
import fi.oph.kitu.oppijanumero.OppijanumeroException
import fi.oph.kitu.oppijanumero.OppijanumeroService
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class KoealustaMappingService(
    private val jacksonObjectMapper: ObjectMapper,
    private val oppijanumeroService: OppijanumeroService,
) {
    private inline fun <reified T> tryParseMoodleResponse(json: String): T {
        try {
            return jacksonObjectMapper.enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION).readValue<T>(json)
        } catch (e: Throwable) {
            throw tryParseMoodleError(json, e)
        }
    }

    private fun tryParseMoodleError(
        json: String,
        originalException: Throwable,
    ): MoodleException {
        try {
            return MoodleException(jacksonObjectMapper.readValue<MoodleErrorMessage>(json))
        } catch (e: Throwable) {
            throw RuntimeException(
                "Could not parse Moodle error message: ${e.message} while handling parsing error",
                originalException,
            )
        }
    }

    fun responseStringToEntity(body: String): Iterable<KielitestiSuoritus> =
        convertToEntity(tryParseMoodleResponse<KoealustaSuorituksetResponse>(body))

    fun convertToEntity(suorituksetResponse: KoealustaSuorituksetResponse): Iterable<KielitestiSuoritus> {
        val oppijanumeroExceptions = mutableListOf<OppijanumeroException>()
        val validationErrors = mutableListOf<Error.Validation>()

        val suoritukset =
            suorituksetResponse.users.flatMap { user ->
                val oppijanumero =
                    toOppija(user)
                        .flatMap { oppijanumeroService.getOppijanumero(it) }
                        .onFailure {
                            when (it) {
                                // Only catch expected types of exceptions. Other kinds are internal programming errors,
                                // and should fail-fast.
                                is Error.OppijaValidationFailure -> validationErrors.addAll(it.validationErrors)
                                is OppijanumeroException -> oppijanumeroExceptions.add(it)
                                else -> throw it
                            }
                        }
                        // Recover as null, to indicate a missing oppijanumero. This intentionally fails the validation
                        // during the `completionToEntity` conversion. We do not throw the error (yet), as we want to
                        // gather any/all validation errors before failing.
                        .getOrNull()

                user.completions.flatMap { completion ->
                    try {
                        listOf(
                            completionToEntity(
                                user,
                                oppijanumero,
                                completion,
                            ),
                        )
                    } catch (ex: Error.Validation) {
                        validationErrors.add(ex)
                        emptyList()
                    }
                }
            }

        if (oppijanumeroExceptions.isNotEmpty() || validationErrors.isNotEmpty()) {
            throw Error.ValidationFailure(
                "Parsing KielitestiSuoritus failed: There were ${validationErrors.size}" +
                    " validation errors and ${oppijanumeroExceptions.size} oppijanumero failures.",
                oppijanumeroExceptions,
                validationErrors,
            )
        }

        return suoritukset
    }

    fun toOppija(koealustaUser: User): Result<Oppija> {
        if (koealustaUser.SSN.isNullOrEmpty()) {
            return Result.failure(Error.Validation.MissingField("SSN", koealustaUser.userid))
        }
        if (koealustaUser.preferredname.isNullOrEmpty()) {
            return Result.failure(Error.Validation.MissingField("preferredname", koealustaUser.userid))
        }

        return Result.success(
            Oppija(
                etunimet = koealustaUser.firstnames,
                hetu = koealustaUser.SSN,
                kutsumanimi = koealustaUser.preferredname,
                sukunimi = koealustaUser.lastname,
                oppijanumero = koealustaUser.oppijanumero?.ifEmpty { null }, // Nullify empty values
            ),
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

    fun getSchoolOid(
        userId: Int,
        oid: String,
    ): Oid =
        Oid
            .parse(oid)
            .onFailure {
                throw Error.Validation.MalformedField(
                    userId,
                    "schoolOID",
                    oid,
                )
            }.getOrThrow()

    fun completionToEntity(
        user: User,
        oppijanumero: String?,
        completion: Completion,
    ): KielitestiSuoritus {
        val luetunYmmartaminen = getLuetunYmmartaminen(user.userid, completion)
        val kuullunYmmartaminen = getKuullunYmmartaminen(user.userid, completion)
        val puhe = getPuhe(user.userid, completion)
        val kirjoittaminen = getKirjoittaminen(user.userid, completion)
        val schoolOid = getSchoolOid(user.userid, completion.schoolOID)

        if (user.preferredname.isNullOrEmpty()) {
            throw Error.Validation.MissingField("preferred name", user.userid)
        }

        if (oppijanumero.isNullOrEmpty()) {
            throw Error.Validation.MissingField("oppijanumero", user.userid)
        }

        return KielitestiSuoritus(
            firstNames = user.firstnames,
            lastName = user.lastname,
            preferredname = user.preferredname,
            email = user.email,
            oppijanumero = oppijanumero,
            timeCompleted = Instant.ofEpochSecond(completion.timecompleted),
            schoolOid = schoolOid,
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

        class OppijaValidationFailure(
            message: String,
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

            class MalformedField(
                userId: Int,
                field: String,
                value: String,
            ) : Validation(userId, "Malformed value \"$value\" in \"$field\" for user \"$userId\"")
        }
    }
}
