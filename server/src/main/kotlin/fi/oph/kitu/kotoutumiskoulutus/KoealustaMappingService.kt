package fi.oph.kitu.kotoutumiskoulutus

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult
import fi.oph.kitu.TypedResult.Failure
import fi.oph.kitu.TypedResult.Success
import fi.oph.kitu.kotoutumiskoulutus.KoealustaMappingService.Error
import fi.oph.kitu.kotoutumiskoulutus.KoealustaSuorituksetResponse.User
import fi.oph.kitu.kotoutumiskoulutus.KoealustaSuorituksetResponse.User.Completion
import fi.oph.kitu.oppijanumero.Oppija
import fi.oph.kitu.oppijanumero.OppijanumeroException
import fi.oph.kitu.oppijanumero.OppijanumeroService
import io.opentelemetry.instrumentation.annotations.WithSpan
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

    @WithSpan
    fun responseStringToEntity(body: String) =
        convertToEntity(tryParseMoodleResponse<KoealustaSuorituksetResponse>(body))

    fun convertToEntity(
        suorituksetResponse: KoealustaSuorituksetResponse,
    ): Pair<List<KielitestiSuoritus>, ValidationFailure?> {
        val oppijanumeroExceptions = mutableListOf<Error>()
        val validationErrors = mutableListOf<Error>()

        val suoritukset =
            suorituksetResponse.users.flatMap { user ->
                val oppija =
                    toOppija(user)
                        .onFailure(validationErrors::add)
                        .getOrNull()

                val oppijanumero =
                    oppija
                        ?.let(oppijanumeroService::getOppijanumero)
                        ?.mapFailure {
                            Error.OppijanumeroFailure(
                                it,
                                "Oppijanumeron haku epäonnistui: Jotkin Moodle-käyttäjän tunnistetiedoista (hetu, etunimet, kutsumanimi, sukunimi) ovat virheellisiä. (${it.message})",
                                Oid.parse(user.completions.first().schoolOID).getOrNull(),
                                moodleId = user.userid.toString(),
                                user.completions.first().teacheremail,
                            )
                        }?.onFailure { oppijanumeroExceptions.add(it) }
                        ?.getOrNull()

                user.completions.mapNotNull { completion ->
                    completionToEntity(user, oppijanumero, completion)
                        .onFailure {
                            validationErrors.add(it)
                        }.getOrNull()
                }
            }

        val validationFailure =
            if (oppijanumeroExceptions.isNotEmpty() || validationErrors.isNotEmpty()) {
                ValidationFailure(
                    message =
                        "Parsing KielitestiSuoritus failed: There were ${validationErrors.size} validation errors and ${oppijanumeroExceptions.size} oppijanumero failures.",
                    oppijanumeroExceptions = oppijanumeroExceptions,
                    validationErrors = validationErrors,
                )
            } else {
                null
            }

        return Pair(suoritukset, validationFailure)
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
                    schoolOid = Oid.parse(koealustaUser.completions.first().schoolOID).getOrNull(),
                    teacherEmail = koealustaUser.completions.first().teacheremail,
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
    ): TypedResult<Completion.Result, Error.Validation> {
        val result =
            completion
                .results
                .find { it.name == resultName }

        return if (result?.quiz_grade.isNullOrEmpty()) {
            Failure(
                Error.Validation.MissingGrade(
                    userId,
                    completion.coursename,
                    resultName,
                ),
            )
        } else {
            Success(result)
        }
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
            validate("puhuminen", user.userid, completion)
                .onFailure { errors.add(it) }
                .getOrNull()
        val kirjoittaminen =
            validate("kirjoittaminen", user.userid, completion)
                .onFailure { errors.add(it) }
                .getOrNull()

        val schoolOid =
            validate("schoolOID", user.userid, completion.schoolOID ?: "")
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
                    schoolOid = Oid.parse(completion.schoolOID).getOrNull(),
                    teacherEmail = completion.teacheremail,
                    koealustaUser = user,
                    validationErrors = errors,
                ),
            )
        }

        checkNotNull(luetunYmmartaminen?.quiz_grade)
        checkNotNull(kuullunYmmartaminen?.quiz_grade)
        checkNotNull(puhe?.quiz_grade)
        checkNotNull(kirjoittaminen?.quiz_grade)
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
                luetunYmmartaminenResult = luetunYmmartaminen.quiz_grade,
                kuullunYmmartaminenResult = kuullunYmmartaminen.quiz_grade,
                puheResult = puhe.quiz_grade,
                kirjoittaminenResult = kirjoittaminen.quiz_grade,
                teacherEmail = completion.teacheremail,
            ),
        )
    }

    fun convertErrors(errors: Iterable<Error>): Iterable<KielitestiSuoritusError> = errors.flatMap(::convertError)

    fun convertError(error: Error): List<KielitestiSuoritusError> {
        val now = Instant.now()
        return when (error) {
            is Error.ValidationFailure ->
                error.validationErrors.map { validationError ->
                    val (field, value) = parseValidationError(validationError)
                    KielitestiSuoritusError(
                        id = null,
                        suorittajanOid = error.koealustaUser.oppijanumero,
                        hetu = error.koealustaUser.SSN,
                        nimi = "${error.koealustaUser.lastname} ${error.koealustaUser.firstnames}",
                        schoolOid = error.schoolOid,
                        teacherEmail = error.teacherEmail,
                        virheenLuontiaika = now,
                        viesti = validationError.message,
                        virheellinenKentta = field,
                        virheellinenArvo = value,
                    )
                }

            is Error.OppijanumeroFailure ->
                listOf(
                    KielitestiSuoritusError(
                        id = null,
                        suorittajanOid = null,
                        hetu = error.oppijanumeroException.request.hetu,
                        nimi =
                            "${error.oppijanumeroException.request.sukunimi} ${error.oppijanumeroException.request.etunimet}",
                        schoolOid = error.schoolOid,
                        teacherEmail = error.teacherEmail,
                        virheenLuontiaika = now,
                        viesti = error.oppijanumeroException.message ?: error.message ?: "Unknown ONR error",
                        virheellinenKentta = null,
                        virheellinenArvo = null,
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
            is Error.Validation.MissingGrade -> FieldInfo(validationError.resultName, null)
        }

    class ValidationFailure(
        message: String,
        val oppijanumeroExceptions: List<Error>,
        val validationErrors: List<Error>,
    ) : Exception(message)

    sealed class Error(
        message: String,
        val schoolOid: Oid?,
        val teacherEmail: String?,
    ) : Exception(message) {
        class OppijanumeroFailure(
            val oppijanumeroException: OppijanumeroException,
            message: String = "ONR error",
            schoolOid: Oid?,
            moodleId: String?,
            teacherEmail: String?,
        ) : Error(message, schoolOid, teacherEmail)

        abstract class ValidationFailure(
            message: String,
            schoolOid: Oid?,
            teacherEmail: String?,
            val koealustaUser: User,
            val validationErrors: List<Validation>,
        ) : Error(message, schoolOid, teacherEmail)

        class OppijaValidationFailure(
            message: String,
            schoolOid: Oid?,
            teacherEmail: String?,
            koealustaUser: User,
            validationErrors: List<Validation>,
        ) : ValidationFailure(message, schoolOid, teacherEmail, koealustaUser, validationErrors)

        class SuoritusValidationFailure(
            message: String,
            schoolOid: Oid?,
            teacherEmail: String?,
            koealustaUser: User,
            validationErrors: List<Validation>,
        ) : ValidationFailure(message, schoolOid, teacherEmail, koealustaUser, validationErrors)

        sealed class Validation(
            val userId: Int,
            val message: String,
        ) {
            class MissingGrade(
                userId: Int,
                courseName: String,
                val resultName: String,
            ) : Validation(
                    userId,
                    """Unexpectedly missing quiz grade "$resultName" on course "$courseName" for user "$userId"""",
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
