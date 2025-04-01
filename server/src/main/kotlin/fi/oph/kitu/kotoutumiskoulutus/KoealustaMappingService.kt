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

    /** Parse response JSON as a string to internal entities.
     *
     * @throws [MoodleException] or [RuntimeException] if an unexpected error occurs
     */
    fun responseStringToEntity(body: String): Iterable<TypedResult<KielitestiSuoritus, out Error>> =
        convertToEntity(tryParseMoodleResponse<KoealustaSuorituksetResponse>(body))

    fun convertToEntity(
        suorituksetResponse: KoealustaSuorituksetResponse,
    ): List<TypedResult<KielitestiSuoritus, out Error>> =
        suorituksetResponse.users.flatMap { user ->
            try {
                val oppija: Oppija = toOppija(user).getOrThrow()
                val oppijanumero: Oid = getOppijanumero(oppija).getOrThrow()
                user.completions.map { completion ->
                    completionToEntity(user, oppijanumero, completion)
                }
            } catch (ex: Throwable) {
                listOf(
                    Failure(
                        when (ex) {
                            is Error.OppijaValidationFailure -> ex
                            is Error.OppijanumeroFailure -> ex
                            else -> throw ex // throw unexpected errors up
                        },
                    ),
                )
            }
        }

    private fun getOppijanumero(oppija: Oppija): TypedResult<Oid, Error.OppijanumeroFailure> =
        oppijanumeroService.getOppijanumero(oppija).mapFailure(Error::OppijanumeroFailure)

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
                    koealustaUser,
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
        oppijanumero: Oid?,
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
        if (oppijanumero == null) {
            errors.add(Error.Validation.MissingField("oppijanumero", user.userid))
        }

        if (errors.isNotEmpty()) {
            return Failure(
                Error.SuoritusValidationFailure(
                    message =
                        """
                        Validation failure on course completion on "${completion.coursename}" for user "${user.userid}"
                        """.trimIndent(),
                    koealustaUser = user,
                    validationErrors = errors,
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
        checkNotNull(oppijanumero)

        return Success(
            KielitestiSuoritus(
                firstNames = user.firstnames,
                lastName = user.lastname,
                preferredname = preferredName,
                email = user.email,
                oppijanumero = oppijanumero,
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

    fun convertErrors(errors: Iterable<Error>): Iterable<KielitestiSuoritusError> = errors.flatMap(::convertError)

    fun convertError(error: Error): List<KielitestiSuoritusError> {
        val now = Instant.now()
        return when (error) {
            is Error.OppijaValidationFailure ->
                error.validationErrors.map(::parseValidationError).map { (field, value) ->
                    KielitestiSuoritusError(
                        id = null,
                        suorittajanOid = error.koealustaUser.oppijanumero,
                        hetu = error.koealustaUser.SSN,
                        nimi = "${error.koealustaUser.lastname} ${error.koealustaUser.firstnames}",
                        lastModified = null,
                        virheellinenKentta = field,
                        virheellinenArvo = value,
                        virheenLuontiaika = now,
                    )
                }
            is Error.SuoritusValidationFailure ->
                error.validationErrors.map(::parseValidationError).map { (field, value) ->
                    KielitestiSuoritusError(
                        id = null,
                        suorittajanOid = error.koealustaUser.oppijanumero,
                        hetu = error.koealustaUser.SSN,
                        nimi = "${error.koealustaUser.lastname} ${error.koealustaUser.firstnames}",
                        lastModified = null,
                        virheellinenKentta = field,
                        virheellinenArvo = value,
                        virheenLuontiaika = now,
                    )
                }
            is Error.OppijanumeroFailure ->
                listOf(
                    KielitestiSuoritusError(
                        id = null,
                        suorittajanOid = null,
                        hetu = error.oppijanumeroException.request.hetu,
                        nimi =
                            """
                                    |${error.oppijanumeroException.request.sukunimi}
                                    |${error.oppijanumeroException.request.etunimet}
                            """.trimMargin(),
                        lastModified = null,
                        virheellinenKentta = error.oppijanumeroException.oppijanumeroServiceError?.path,
                        virheellinenArvo = error.oppijanumeroException.oppijanumeroServiceError?.error,
                        virheenLuontiaika = now,
                    ),
                )
        }
    }

    data class FieldInfo(
        val fieldName: String,
        val fieldValue: String?,
    )

    fun parseValidationError(validationError: Error.Validation): FieldInfo =
        when (validationError) {
            is Error.Validation.MalformedField -> FieldInfo(validationError.field, validationError.value)
            is Error.Validation.MissingField -> FieldInfo(validationError.field, null)
            is Error.Validation.MissingSystemResult -> FieldInfo(validationError.resultName, null)
            is Error.Validation.MissingTeacherResult -> FieldInfo(validationError.resultName, null)
        }

    sealed class Error(
        message: String,
    ) : Exception(message) {
        class OppijanumeroFailure(
            val oppijanumeroException: OppijanumeroException,
        ) : Error("ONR error")

        class OppijaValidationFailure(
            message: String,
            val koealustaUser: User,
            val validationErrors: List<Validation>,
        ) : Error(message)

        class SuoritusValidationFailure(
            message: String,
            val koealustaUser: User,
            val validationErrors: List<Validation>,
        ) : Error(message)

        sealed class Validation(
            val userId: Int,
            val message: String,
        ) {
            class MissingSystemResult(
                userId: Int,
                courseName: String,
                val resultName: String,
            ) : Validation(
                    userId,
                    """Unexpectedly missing quiz system result "$resultName" on course "$courseName" for user "$userId"""",
                )

            class MissingTeacherResult(
                userId: Int,
                courseName: String,
                val resultName: String,
            ) : Validation(
                    userId,
                    """Unexpectedly missing quiz teacher result "$resultName" on course "$courseName" for user "$userId"""",
                )

            class MissingField(
                val field: String,
                userId: Int,
            ) : Validation(userId, """Missing student "$field" for user "$userId"""")

            class MalformedField(
                userId: Int,
                val field: String,
                val value: String,
            ) : Validation(userId, """Malformed value "$value" in "$field" for user "$userId"""")
        }
    }
}
