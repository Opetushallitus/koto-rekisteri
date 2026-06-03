package fi.oph.kitu.yki.suoritukset

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import fi.oph.kitu.auditlogs.AuditLogOperation
import fi.oph.kitu.auditlogs.AuditLogger
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.util.result.getOrThrow
import fi.oph.kitu.yki.Arviointitila
import fi.oph.kitu.yki.Sukupuoli
import fi.oph.kitu.yki.TutkinnonOsa
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate

data class PoikkeamaKey(
    val solkiId: Int,
    val kentta: String,
) {
    fun encode(): String = "$solkiId:$kentta"

    companion object {
        fun decode(s: String): PoikkeamaKey? {
            val colon = s.indexOf(':')
            if (colon <= 0 || colon == s.length - 1) return null
            val solkiId = s.substring(0, colon).toIntOrNull() ?: return null
            return PoikkeamaKey(solkiId, s.substring(colon + 1))
        }
    }
}

sealed interface PatchFailure {
    val key: PoikkeamaKey
    val message: String

    data class PoikkeamaNotFound(
        override val key: PoikkeamaKey,
    ) : PatchFailure {
        override val message = "Poikkeamaa ei löytynyt"
    }

    data class SuoritusNotFound(
        override val key: PoikkeamaKey,
    ) : PatchFailure {
        override val message = "Suoritusta ei löytynyt"
    }

    data class MissingSuoritusNotPatchable(
        override val key: PoikkeamaKey,
    ) : PatchFailure {
        override val message = "Puuttuvaa suoritusta ei voi patchata"
    }

    data class UnknownKentta(
        override val key: PoikkeamaKey,
    ) : PatchFailure {
        override val message = "Tuntematon kenttä"
    }

    data class ValueParseFailed(
        override val key: PoikkeamaKey,
        val cause: String,
    ) : PatchFailure {
        override val message = "Arvon parsiminen epäonnistui: $cause"
    }

    data class SaveFailed(
        override val key: PoikkeamaKey,
        val cause: String,
    ) : PatchFailure {
        override val message = "Tallennus epäonnistui: $cause"
    }
}

@Service
class YkiSuoritusPoikkeamaPatchService(
    private val poikkeamaRepository: YkiSuoritusPoikkeamaRepository,
    private val suoritusRepository: YkiSuoritusRepository,
    private val auditLogger: AuditLogger,
) {
    fun patch(keys: List<PoikkeamaKey>): List<Either<PatchFailure, PoikkeamaKey>> = keys.map(::patchOne)

    private fun patchOne(key: PoikkeamaKey): Either<PatchFailure, PoikkeamaKey> {
        val poikkeama =
            poikkeamaRepository.findByKey(key.solkiId, key.kentta)
                ?: return PatchFailure.PoikkeamaNotFound(key).left()

        if (poikkeama.kentta == YkiSuoritusPoikkeama.SUORITUS_PUUTTUU_KITUSTA) {
            return PatchFailure.MissingSuoritusNotPatchable(key).left()
        }

        val entity =
            suoritusRepository.findLatestBySolkiIds(listOf(key.solkiId)).firstOrNull()
                ?: return PatchFailure.SuoritusNotFound(key).left()

        val patched =
            try {
                applyArvoSolkissa(entity, key.kentta, poikkeama.arvoSolkissa)
            } catch (_: UnknownKenttaException) {
                return PatchFailure.UnknownKentta(key).left()
            } catch (e: Exception) {
                return PatchFailure.ValueParseFailed(key, e.message ?: e::class.simpleName.orEmpty()).left()
            }

        try {
            suoritusRepository.save(
                patched.copy(id = null, lastModified = Instant.now()),
                updateOnConflict = true,
            )
        } catch (e: Exception) {
            return PatchFailure.SaveFailed(key, e.message ?: e::class.simpleName.orEmpty()).left()
        }

        poikkeamaRepository.deleteByKey(key.solkiId, key.kentta)
        auditLogger.log(AuditLogOperation.YkiSuoritusPatched, oppijaHenkiloOid = entity.suorittajanOID)

        return key.right()
    }
}

private class UnknownKenttaException : RuntimeException()

private fun applyArvoSolkissa(
    entity: YkiSuoritusEntity,
    kentta: String,
    arvoSolkissa: String,
): YkiSuoritusEntity =
    when (kentta) {
        "suorittajanOID" -> {
            entity.copy(suorittajanOID = Oid.parse(arvoSolkissa).getOrThrow())
        }

        "sukupuoli" -> {
            entity.copy(sukupuoli = Sukupuoli.valueOf(arvoSolkissa))
        }

        "sukunimi" -> {
            entity.copy(sukunimi = arvoSolkissa)
        }

        "etunimet" -> {
            entity.copy(etunimet = arvoSolkissa)
        }

        "kansalaisuus" -> {
            entity.copy(kansalaisuus = arvoSolkissa)
        }

        "katuosoite" -> {
            entity.copy(katuosoite = arvoSolkissa)
        }

        "postinumero" -> {
            entity.copy(postinumero = arvoSolkissa)
        }

        "postitoimipaikka" -> {
            entity.copy(postitoimipaikka = arvoSolkissa)
        }

        "email" -> {
            entity.copy(email = arvoSolkissa.nullIfBlank())
        }

        "tutkintopaiva" -> {
            entity.copy(tutkintopaiva = LocalDate.parse(arvoSolkissa))
        }

        "tutkintokieli" -> {
            entity.copy(tutkintokieli = Tutkintokieli.valueOf(arvoSolkissa))
        }

        "tutkintotaso" -> {
            entity.copy(tutkintotaso = Tutkintotaso.valueOf(arvoSolkissa))
        }

        "jarjestajanTunnusOid" -> {
            entity.copy(jarjestajanTunnusOid = Oid.parse(arvoSolkissa).getOrThrow())
        }

        "jarjestajanNimi" -> {
            entity.copy(jarjestajanNimi = arvoSolkissa)
        }

        "arviointipaiva" -> {
            entity.copy(arviointipaiva = parseLocalDateOrNull(arvoSolkissa))
        }

        "tekstinYmmartaminen" -> {
            entity.copy(tekstinYmmartaminen = parseIntOrNull(arvoSolkissa))
        }

        "kirjoittaminen" -> {
            entity.copy(kirjoittaminen = parseIntOrNull(arvoSolkissa))
        }

        "rakenteetJaSanasto" -> {
            entity.copy(rakenteetJaSanasto = parseIntOrNull(arvoSolkissa))
        }

        "puheenYmmartaminen" -> {
            entity.copy(puheenYmmartaminen = parseIntOrNull(arvoSolkissa))
        }

        "puhuminen" -> {
            entity.copy(puhuminen = parseIntOrNull(arvoSolkissa))
        }

        "yleisarvosana" -> {
            entity.copy(yleisarvosana = parseIntOrNull(arvoSolkissa))
        }

        "tarkistusarvioinninSaapumisPvm" -> {
            entity.copy(tarkistusarvioinninSaapumisPvm = parseLocalDateOrNull(arvoSolkissa))
        }

        "tarkistusarvioinninAsiatunnus" -> {
            entity.copy(tarkistusarvioinninAsiatunnus = arvoSolkissa.nullIfBlank())
        }

        "tarkistusarvioidutOsakokeet" -> {
            entity.copy(tarkistusarvioidutOsakokeet = parseOsakoeSet(arvoSolkissa))
        }

        "arvosanaMuuttui" -> {
            entity.copy(arvosanaMuuttui = parseOsakoeSet(arvoSolkissa))
        }

        "perustelu" -> {
            entity.copy(perustelu = arvoSolkissa.nullIfBlank())
        }

        "tarkistusarvioinninKasittelyPvm" -> {
            entity.copy(tarkistusarvioinninKasittelyPvm = parseLocalDateOrNull(arvoSolkissa))
        }

        "tarkistusarviointiHyvaksyttyPvm" -> {
            entity.copy(tarkistusarviointiHyvaksyttyPvm = parseLocalDateOrNull(arvoSolkissa))
        }

        "arviointitila" -> {
            entity.copy(arviointitila = Arviointitila.valueOf(arvoSolkissa))
        }

        else -> {
            throw UnknownKenttaException()
        }
    }

private fun String.nullIfBlank(): String? = takeIf { it.isNotBlank() }

private fun parseIntOrNull(s: String): Int? = s.nullIfBlank()?.toInt()

private fun parseLocalDateOrNull(s: String): LocalDate? = s.nullIfBlank()?.let(LocalDate::parse)

private fun parseOsakoeSet(s: String): Set<TutkinnonOsa>? {
    val trimmed = s.trim()
    if (trimmed.isBlank() || trimmed == "null") return null
    val inner = trimmed.removeSurrounding("[", "]").trim()
    if (inner.isEmpty()) return emptySet()
    return inner
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { TutkinnonOsa.valueOf(it) }
        .toSet()
}
