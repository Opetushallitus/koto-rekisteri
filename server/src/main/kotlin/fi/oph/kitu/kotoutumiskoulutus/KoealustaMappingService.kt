package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult
import fi.oph.kitu.TypedResult.Failure
import fi.oph.kitu.TypedResult.Success
import fi.oph.kitu.defaultObjectMapper
import fi.oph.kitu.kotoutumiskoulutus.KoealustaSuorituksetResponse.User
import fi.oph.kitu.kotoutumiskoulutus.KoealustaSuorituksetResponse.User.Completion
import fi.oph.kitu.oppijanumero.Oppija
import fi.oph.kitu.oppijanumero.OppijanumeroException
import fi.oph.kitu.oppijanumero.OppijanumeroService
import fi.oph.kitu.oppijanumero.OppijanumeroServiceError
import fi.oph.kitu.oppijanumero.OppijanumerorekisteriRequest
import fi.oph.kitu.oppijanumero.YleistunnisteHaeRequest
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import java.time.Instant

@Service
class KoealustaMappingService(
    private val jacksonObjectMapper: JsonMapper,
    private val oppijanumeroService: OppijanumeroService,
) {
    private inline fun <reified T> tryParseMoodleResponse(json: String): T {
        try {
            return jacksonObjectMapper.readValue(json)
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
                            val debugInfo =
                                OppijanumerorekisteriDebugInfo
                                    .from(
                                        it.request,
                                        if (it is OppijanumeroException.HasResponse) it.response else null,
                                    )
                            Error.OppijanumeroFailure(
                                it,
                                "Oppijanumeron haku epäonnistui: ${debugInfo.message() ?: it.oppijanumeroServiceError?.error ?: it.message ?: "ei tarkempia tietoja"}",
                                Oid.parse(user.completions.first().schoolOID).getOrNull(),
                                moodleId = user.userid.toString(),
                                user.completions.first().teacheremail,
                                debugInfo.toString(),
                            )
                        }?.onFailure { oppijanumeroExceptions.add(it) }
                        ?.getOrNull()

                user.completions.mapNotNull { completion ->
                    completionToEntity(user, oppijanumero, completion)
                        ?.onFailure {
                            validationErrors.add(it)
                        }?.getOrNull()
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
                etunimet = koealustaUser.firstnames.trim(),
                hetu = koealustaUser.SSN.trim(),
                kutsumanimi = koealustaUser.preferredname.trim(),
                sukunimi = koealustaUser.lastname.trim(),
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
    ): TypedResult<KielitestiSuoritus, Error.SuoritusValidationFailure>? {
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
            validate("schoolOID", user.userid, completion.schoolOID.orEmpty())
                .onFailure { errors.add(it) }
                .getOrNull()

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
                    oppijanumero = oppijanumero,
                ),
            )
        }

        if (user.preferredname == null || oppijanumero == null) return null

        checkNotNull(luetunYmmartaminen?.quiz_grade)
        checkNotNull(kuullunYmmartaminen?.quiz_grade)
        checkNotNull(puhe?.quiz_grade)
        checkNotNull(kirjoittaminen?.quiz_grade)
        checkNotNull(schoolOid)

        return Success(
            KielitestiSuoritus(
                firstNames = user.firstnames.trim(),
                lastName = user.lastname.trim(),
                preferredname = user.preferredname.trim(),
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
            is Error.ValidationFailure -> {
                error.validationErrors.map { validationError ->
                    val (field, value) = parseValidationError(validationError)
                    KielitestiSuoritusError(
                        id = null,
                        suorittajanOid = error.oppijanumero.toString(),
                        hetu = error.koealustaUser.SSN,
                        nimi = "${error.koealustaUser.lastname} ${error.koealustaUser.firstnames}",
                        etunimet = error.koealustaUser.firstnames,
                        sukunimi = error.koealustaUser.lastname,
                        kutsumanimi = error.koealustaUser.preferredname,
                        schoolOid = error.schoolOid,
                        teacherEmail = error.teacherEmail,
                        virheenLuontiaika = now,
                        viesti = validationError.message,
                        virheellinenKentta = field,
                        virheellinenArvo = value,
                        lisatietoja = null,
                        onrLisatietoja = null,
                    )
                }
            }

            is Error.OppijanumeroFailure -> {
                listOf(
                    KielitestiSuoritusError(
                        id = null,
                        suorittajanOid = null,
                        hetu = (error.oppijanumeroException.request as YleistunnisteHaeRequest).hetu,
                        nimi =
                            "${error.oppijanumeroException.request.sukunimi} ${error.oppijanumeroException.request.etunimet}",
                        etunimet = error.oppijanumeroException.request.etunimet,
                        sukunimi = error.oppijanumeroException.request.sukunimi,
                        kutsumanimi = error.oppijanumeroException.request.kutsumanimi,
                        schoolOid = error.schoolOid,
                        teacherEmail = error.teacherEmail,
                        virheenLuontiaika = now,
                        viesti = error.message,
                        virheellinenKentta = null,
                        virheellinenArvo = null,
                        lisatietoja = error.debugInfo,
                        onrLisatietoja = null,
                    ),
                )
            }
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
    ) : Exception(message) {
        fun isEmpty(): Boolean = oppijanumeroExceptions.isEmpty() && validationErrors.isEmpty()

        fun isNotEmpty(): Boolean = !isEmpty()
    }

    sealed class Error(
        message: String,
        val schoolOid: Oid?,
        val teacherEmail: String?,
    ) : Exception(message) {
        class OppijanumeroFailure(
            val oppijanumeroException: OppijanumeroException,
            override val message: String,
            schoolOid: Oid?,
            moodleId: String?,
            teacherEmail: String?,
            val debugInfo: String?,
        ) : Error(message, schoolOid, teacherEmail)

        abstract class ValidationFailure(
            message: String,
            schoolOid: Oid?,
            teacherEmail: String?,
            val koealustaUser: User,
            val validationErrors: List<Validation>,
            val oppijanumero: Oid? = null,
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
            oppijanumero: Oid?,
        ) : ValidationFailure(message, schoolOid, teacherEmail, koealustaUser, validationErrors, oppijanumero)

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

interface DebugInfo {
    val source: String
}

data class OppijanumerorekisteriDebugInfo(
    val request: OppijanumerorekisteriRequest,
    val detectedTypicalErrors: List<String>,
    val error: OppijanumeroServiceError?,
    val rawResponse: String?,
) : DebugInfo {
    override val source = "oppijanumerorekisteri"

    override fun toString(): String = defaultObjectMapper.writeValueAsString(this)

    fun message(): String? =
        when (detectedTypicalErrors.size) {
            0 -> null
            1 -> detectedTypicalErrors[0]
            2 -> "${detectedTypicalErrors[0]} ja 1 muu virhe"
            else -> "${detectedTypicalErrors[0]} ja ${detectedTypicalErrors.size - 1} muuta virhettä"
        }

    companion object {
        fun from(
            request: OppijanumerorekisteriRequest,
            response: ResponseEntity<String>?,
        ): OppijanumerorekisteriDebugInfo {
            val error =
                try {
                    response?.body?.let { defaultObjectMapper.readValue<OppijanumeroServiceError>(it) }
                } catch (_: Exception) {
                    null
                }

            val validationErrors: List<String> =
                error?.message?.let { msg ->
                    mapOf(
                        "Nick name must be one of the first names" to "Kutsumanimen on oltava yksi etunimistä",
                        "Invalid pattern. Must contain an alphabetic character" to
                            "Kutsumanimessä ei saa olla erikoismerkkejä, mukaanlukien välilyönti",
                    ).mapNotNull { if (msg.contains(it.key)) it.value else null }
                } ?: emptyList()

            val statusCodeMessages: List<String> =
                listOfNotNull(
                    when (response?.statusCode?.value()) {
                        401 -> "Kielitutkintorekisterin järjestelmätunnuksen käyttöoikeudet eivät ole riittävät"
                        404 -> "Henkilöä ei löydy Oppijanumerorekisteristä"
                        409 -> "Kirjoitusvirhe henkilötunnuksessa tai nimessä"
                        else -> null
                    },
                )

            return OppijanumerorekisteriDebugInfo(
                request = request,
                detectedTypicalErrors = validationErrors + statusCodeMessages,
                error = error,
                rawResponse = if (error == null) response?.body else null,
            )
        }
    }
}
