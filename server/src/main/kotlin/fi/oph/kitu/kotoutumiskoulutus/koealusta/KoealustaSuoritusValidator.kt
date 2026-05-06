package fi.oph.kitu.kotoutumiskoulutus.koealusta

import fi.oph.kitu.kotoutumiskoulutus.koealusta.KoealustaSuorituksetResponse.User
import fi.oph.kitu.kotoutumiskoulutus.koealusta.KoealustaSuorituksetResponse.User.Completion
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.Arvosana
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.KielitestiSuoritus
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.Testikieli
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.oppijanumero.Oppija
import fi.oph.kitu.result.TypedResult
import fi.oph.kitu.result.TypedResult.Failure
import fi.oph.kitu.result.TypedResult.Success
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class KoealustaSuoritusValidator {
    fun toOppija(koealustaUser: User): TypedResult<Oppija, KoealustaMappingError.OppijaValidationFailure> {
        val errors = mutableListOf<KoealustaMappingError.Validation>()
        if (koealustaUser.SSN.isNullOrEmpty()) {
            errors.add(KoealustaMappingError.Validation.MissingField("SSN", koealustaUser.userid))
        }
        if (koealustaUser.preferredname.isNullOrEmpty()) {
            errors.add(KoealustaMappingError.Validation.MissingField("preferredname", koealustaUser.userid))
        }

        if (errors.isNotEmpty()) {
            return Failure(
                KoealustaMappingError.OppijaValidationFailure(
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

    fun completionToEntity(
        user: User,
        oppijanumero: Oid?,
        completion: Completion,
    ): TypedResult<KielitestiSuoritus, KoealustaMappingError.SuoritusValidationFailure>? {
        val errors = mutableListOf<KoealustaMappingError.Validation>()
        val luetunYmmartaminen =
            validate("luetun ymmärtäminen", user.userid, completion)
                .onFailure { errors.add(it) }
                .getOrNull()
        val kuullunYmmartaminen =
            validate("kuullun ymmärtäminen", user.userid, completion)
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

        val testikieli =
            validate(user, completion.lang)
                .onFailure { errors.add(it) }
                .getOrNull()

        if (errors.isNotEmpty()) {
            return Failure(
                KoealustaMappingError.SuoritusValidationFailure(
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

        checkNotNull(luetunYmmartaminen)
        checkNotNull(kuullunYmmartaminen)
        checkNotNull(kirjoittaminen)
        checkNotNull(puhe)
        checkNotNull(schoolOid)

        return Success(
            KielitestiSuoritus(
                etunimet = user.firstnames.trim(),
                sukunimi = user.lastname.trim(),
                kutsumanimi = user.preferredname.trim(),
                email = user.email,
                oppijanumero = oppijanumero,
                suoritusaika = Instant.ofEpochSecond(completion.timecompleted),
                oppilaitosOid = schoolOid,
                kurssiId = completion.courseid,
                kurssi = completion.coursename,
                luetunYmmartaminen = luetunYmmartaminen,
                kuullunYmmartaminen = kuullunYmmartaminen,
                puhe = puhe,
                kirjoittaminen = kirjoittaminen,
                testikieli = testikieli,
                opettajanEmail = completion.teacheremail,
                tehtavapaketti = completion.questionbank,
            ),
        )
    }

    private fun validate(
        resultName: String,
        userId: Int,
        completion: Completion,
    ): TypedResult<Arvosana, KoealustaMappingError.Validation> {
        val result =
            completion
                .results
                .find { it.name == resultName }

        return if (result?.quizGrade.isNullOrEmpty()) {
            Failure(
                KoealustaMappingError.Validation.MissingGrade(
                    userId,
                    completion.coursename,
                    resultName,
                ),
            )
        } else {
            try {
                Success(Arvosana.Companion.fromString(result.quizGrade))
            } catch (_: IllegalArgumentException) {
                Failure(
                    KoealustaMappingError.Validation.MalformedField(
                        userId,
                        resultName,
                        result.quizGrade,
                    ),
                )
            }
        }
    }

    private fun validate(
        user: User,
        lang: String?,
    ): TypedResult<Testikieli?, KoealustaMappingError.Validation> =
        if (lang == null) {
            Failure(
                KoealustaMappingError.Validation.MissingField(
                    "lang",
                    user.userid,
                ),
            )
        } else {
            try {
                Success(Testikieli.Companion.fromString(lang))
            } catch (_: IllegalArgumentException) {
                Failure(
                    KoealustaMappingError.Validation.MalformedField(
                        userId = user.userid,
                        field = "lang",
                        value = lang,
                    ),
                )
            }
        }

    private fun validate(
        fieldName: String,
        userId: Int,
        oid: String,
    ): TypedResult<Oid, KoealustaMappingError.Validation> =
        Oid
            .parseTyped(oid)
            .mapFailure { KoealustaMappingError.Validation.MalformedField(userId, fieldName, oid) }
}
