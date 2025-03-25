package fi.oph.kitu.kotoutumiskoulutus

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult
import fi.oph.kitu.TypedResult.Failure
import fi.oph.kitu.TypedResult.Success
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
                        .fold(
                            onSuccess = { oppijanumeroService.getOppijanumero(it) },
                            onFailure = {
                                validationErrors.addAll(it.validationErrors)
                                Success(null)
                            },
                        ).onFailure { oppijanumeroExceptions.add(it) }
                        .getOrNull()

                user.completions.mapNotNull { completion ->
                    completionToEntity(user, oppijanumero, completion)
                        .onFailure { validationErrors.addAll(it.validationErrors) }
                        .getOrNull()
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

    fun toOppija(koealustaUser: User): TypedResult<Oppija, Error.OppijaValidationFailure> {
        val errors = mutableListOf<Error.Validation>()
        if (koealustaUser.SSN.isNullOrEmpty()) {
            errors.add(Error.Validation.MissingField("SSN", koealustaUser.userid))
        }
        if (koealustaUser.preferredname.isNullOrEmpty()) {
            errors.add(Error.Validation.MissingField("preferredname", koealustaUser.userid))
        }

        if (errors.isNotEmpty()) {
            return Failure(
                Error.OppijaValidationFailure(
                    "Validation failure on converting user \"${koealustaUser.userid}\" to oppija",
                    errors,
                ),
            )
        }

        checkNotNull(koealustaUser.SSN)
        checkNotNull(koealustaUser.preferredname)

        return Success(
            Oppija(
                etunimet = koealustaUser.firstnames,
                hetu = koealustaUser.SSN,
                kutsumanimi = koealustaUser.preferredname,
                sukunimi = koealustaUser.lastname,
                oppijanumero = koealustaUser.oppijanumero?.ifEmpty { null }, // Nullify empty values
            ),
        )
    }

    private fun validate(
        resultName: String,
        userId: Int,
        completion: Completion,
        isSystemResultRequired: Boolean = true,
    ): TypedResult<Completion.Result, Error.Validation> {
        val result =
            completion
                .results
                .find { it.name == resultName }

        if (isSystemResultRequired && result?.quiz_result_system.isNullOrEmpty()) {
            return Failure(
                Error.Validation.MissingSystemResult(
                    userId,
                    completion.coursename,
                    resultName,
                ),
            )
        }
        if (result?.quiz_result_teacher.isNullOrEmpty()) {
            return Failure(
                Error.Validation.MissingTeacherResult(
                    userId,
                    completion.coursename,
                    resultName,
                ),
            )
        }

        return Success(result)
    }

    private fun validate(
        fieldName: String,
        userId: Int,
        oid: String,
    ): TypedResult<Oid, Error.Validation> =
        Oid
            .parseTyped(oid)
            .mapFailure { Error.Validation.MalformedField(userId, fieldName, oid) }

    private fun validateNonEmpty(
        fieldName: String,
        userId: Int,
        value: String?,
    ): TypedResult<String, Error.Validation> =
        if (value.isNullOrEmpty()) {
            Failure(Error.Validation.MissingField(fieldName, userId))
        } else {
            Success(value)
        }

    fun completionToEntity(
        user: User,
        oppijanumero: String?,
        completion: Completion,
    ): TypedResult<KielitestiSuoritus, Error.SuoritusValidationFailure> {
        val errors = mutableListOf<Error.Validation>()
        val luetunYmmartaminen =
            validate("luetun ymm\u00e4rt\u00e4minen", user.userid, completion)
                .onFailure { errors.add(it) }
                .getOrNull()
        val kuullunYmmartaminen =
            validate("kuullun ymm\u00e4rt\u00e4minen", user.userid, completion)
                .onFailure { errors.add(it) }
                .getOrNull()
        val puhe =
            validate("puhe", user.userid, completion, isSystemResultRequired = false)
                .onFailure { errors.add(it) }
                .getOrNull()
        val kirjoittaminen =
            validate("kirjoittaminen", user.userid, completion, isSystemResultRequired = false)
                .onFailure { errors.add(it) }
                .getOrNull()

        val schoolOid =
            validate("schoolOID", user.userid, completion.schoolOID)
                .onFailure { errors.add(it) }
                .getOrNull()
        val preferredName =
            validateNonEmpty("preferredname", user.userid, user.preferredname)
                .onFailure { errors.add(it) }
                .getOrNull()
        val validOppijanumero =
            validateNonEmpty("oppijanumero", user.userid, oppijanumero)
                .onFailure { errors.add(it) }
                .getOrNull()

        if (errors.isNotEmpty()) {
            return Failure(
                Error.SuoritusValidationFailure(
                    "Validation failure on course completion on \"${completion.coursename}\" for user \"${user.userid}\"",
                    errors,
                ),
            )
        }

        checkNotNull(luetunYmmartaminen?.quiz_result_system)
        checkNotNull(luetunYmmartaminen.quiz_result_teacher)
        checkNotNull(kuullunYmmartaminen?.quiz_result_system)
        checkNotNull(kuullunYmmartaminen.quiz_result_teacher)
        checkNotNull(puhe?.quiz_result_teacher)
        checkNotNull(kirjoittaminen?.quiz_result_teacher)
        checkNotNull(schoolOid)
        checkNotNull(preferredName)
        checkNotNull(validOppijanumero)

        return Success(
            KielitestiSuoritus(
                firstNames = user.firstnames,
                lastName = user.lastname,
                preferredname = preferredName,
                email = user.email,
                oppijanumero = validOppijanumero,
                timeCompleted = Instant.ofEpochSecond(completion.timecompleted),
                schoolOid = schoolOid,
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
            ),
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

        class SuoritusValidationFailure(
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
                resultName: String,
            ) : Validation(
                    userId,
                    "Unexpectedly missing quiz system result \"$resultName\" on course \"$courseName\" for user \"$userId\"",
                )

            class MissingTeacherResult(
                userId: Int,
                courseName: String,
                resultName: String,
            ) : Validation(
                    userId,
                    "Unexpectedly missing quiz teacher result \"$resultName\" on course \"$courseName\" for user \"$userId\"",
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
