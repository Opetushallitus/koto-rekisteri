package fi.oph.kitu.kotoutumiskoulutus.koealusta

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import fi.oph.kitu.kotoutumiskoulutus.koealusta.KoealustaSuorituksetResponse.User
import fi.oph.kitu.kotoutumiskoulutus.koealusta.KoealustaSuorituksetResponse.User.Completion
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.Arvosana
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.KielitestiSuoritus
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.Testikieli
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.oppijanumero.Oppija
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class KoealustaSuoritusValidator {
    fun toOppija(koealustaUser: User): Either<KoealustaMappingError.OppijaValidationFailure, Oppija> {
        val errors = mutableListOf<KoealustaMappingError.Validation>()
        if (koealustaUser.SSN.isNullOrEmpty()) {
            errors.add(KoealustaMappingError.Validation.MissingField("SSN", koealustaUser.userid))
        }
        if (koealustaUser.preferredname.isNullOrEmpty()) {
            errors.add(KoealustaMappingError.Validation.MissingField("preferredname", koealustaUser.userid))
        }

        if (errors.isNotEmpty()) {
            return KoealustaMappingError
                .OppijaValidationFailure(
                    "Validation failure on converting user \"${koealustaUser.userid}\" to oppija",
                    schoolOid = Oid.parse(koealustaUser.completions.first().schoolOID).getOrNull(),
                    teacherEmail = koealustaUser.completions.first().teacheremail,
                    koealustaUser,
                    errors,
                ).left()
        }

        checkNotNull(koealustaUser.SSN)
        checkNotNull(koealustaUser.preferredname)

        return Oppija(
            etunimet = koealustaUser.firstnames.trim(),
            hetu = koealustaUser.SSN.trim(),
            kutsumanimi = koealustaUser.preferredname.trim(),
            sukunimi = koealustaUser.lastname.trim(),
        ).right()
    }

    fun completionToEntity(
        user: User,
        oppijanumero: Oid?,
        completion: Completion,
    ): Either<KoealustaMappingError.SuoritusValidationFailure, KielitestiSuoritus>? {
        val errors = mutableListOf<KoealustaMappingError.Validation>()
        val luetunYmmartaminen =
            validate("luetun ymmärtäminen", user.userid, completion)
                .onLeft { errors.add(it) }
                .getOrNull()
        val kuullunYmmartaminen =
            validate("kuullun ymmärtäminen", user.userid, completion)
                .onLeft { errors.add(it) }
                .getOrNull()
        val puhe =
            validate("puhuminen", user.userid, completion)
                .onLeft { errors.add(it) }
                .getOrNull()
        val kirjoittaminen =
            validate("kirjoittaminen", user.userid, completion)
                .onLeft { errors.add(it) }
                .getOrNull()

        val schoolOid =
            validate("schoolOID", user.userid, completion.schoolOID.orEmpty())
                .onLeft { errors.add(it) }
                .getOrNull()

        val testikieli =
            validate(user, completion.lang)
                .onLeft { errors.add(it) }
                .getOrNull()

        if (errors.isNotEmpty()) {
            return KoealustaMappingError
                .SuoritusValidationFailure(
                    message =
                        """
                        Validation failure on course completion on "${completion.coursename}" for user "${user.userid}"
                        """.trimIndent(),
                    schoolOid = Oid.parse(completion.schoolOID).getOrNull(),
                    teacherEmail = completion.teacheremail,
                    koealustaUser = user,
                    validationErrors = errors,
                    oppijanumero = oppijanumero,
                ).left()
        }

        if (user.preferredname == null || oppijanumero == null) return null

        checkNotNull(luetunYmmartaminen)
        checkNotNull(kuullunYmmartaminen)
        checkNotNull(kirjoittaminen)
        checkNotNull(puhe)
        checkNotNull(schoolOid)

        return KielitestiSuoritus(
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
        ).right()
    }

    private fun validate(
        resultName: String,
        userId: Int,
        completion: Completion,
    ): Either<KoealustaMappingError.Validation, Arvosana> {
        val result =
            completion
                .results
                .find { it.name == resultName }

        return if (result?.quizGrade.isNullOrEmpty()) {
            KoealustaMappingError.Validation
                .MissingGrade(
                    userId,
                    completion.coursename,
                    resultName,
                ).left()
        } else {
            try {
                Arvosana.Companion.fromString(result.quizGrade).right()
            } catch (_: IllegalArgumentException) {
                KoealustaMappingError.Validation
                    .MalformedField(
                        userId,
                        resultName,
                        result.quizGrade,
                    ).left()
            }
        }
    }

    private fun validate(
        user: User,
        lang: String?,
    ): Either<KoealustaMappingError.Validation, Testikieli?> =
        if (lang == null) {
            KoealustaMappingError.Validation
                .MissingField(
                    "lang",
                    user.userid,
                ).left()
        } else {
            try {
                Testikieli.Companion.fromString(lang).right()
            } catch (_: IllegalArgumentException) {
                KoealustaMappingError.Validation
                    .MalformedField(
                        userId = user.userid,
                        field = "lang",
                        value = lang,
                    ).left()
            }
        }

    private fun validate(
        fieldName: String,
        userId: Int,
        oid: String,
    ): Either<KoealustaMappingError.Validation, Oid> =
        Oid
            .parse(oid)
            .mapLeft { KoealustaMappingError.Validation.MalformedField(userId, fieldName, oid) }
}
