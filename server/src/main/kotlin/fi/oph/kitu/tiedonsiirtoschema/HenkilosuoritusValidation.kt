package fi.oph.kitu.tiedonsiirtoschema

import fi.oph.kitu.Oid
import fi.oph.kitu.oppijanumero.OppijanumeroValidation
import fi.oph.kitu.validation.Validation
import fi.oph.kitu.validation.ValidationResult
import org.springframework.stereotype.Service

@Service
class HenkilosuoritusValidation(
    val onr: OppijanumeroValidation,
) : Validation<Henkilosuoritus<*>> {
    override fun validationBeforeEnrichment(value: Henkilosuoritus<*>): ValidationResult<Henkilosuoritus<*>> =
        Validation.fold(
            value,
            validateInternalId,
            validateKoskiSiirtoKasitelty,
            validateKoskiOpiskeluoikeusOid,
            { validateHenkiloOid(it) },
        )

    private fun validateHenkiloOid(s: Henkilosuoritus<*>): ValidationResult<Henkilosuoritus<*>> =
        onr.validateOppijanumero(s.henkilo.oid, listOf("henkilo", "oid")).map { s }

    private val validateInternalId =
        Validation.assertEquals<Henkilosuoritus<*>, Int?>(
            null,
            { it.suoritus.internalId },
            listOf("suoritus", "internalId"),
            "internalId on sisäinen kenttä, eikä sitä voi asettaa",
        )

    private val validateKoskiSiirtoKasitelty =
        Validation.assertNotEquals<Henkilosuoritus<*>, Boolean?>(
            true,
            { it.suoritus.koskiSiirtoKasitelty },
            listOf("suoritus", "koskiSiirtoKasitelty"),
            "koskiSiirtoKasitelty on sisäinen kenttä, eikä sitä voi asettaa arvoon true",
        )

    private val validateKoskiOpiskeluoikeusOid =
        Validation.assertEquals<Henkilosuoritus<*>, Oid?>(
            null,
            { it.suoritus.koskiOpiskeluoikeusOid },
            listOf("suoritus", "koskiOpiskeluoikeusOid"),
            "koskiOpiskeluoikeusOid on sisäinen kenttä, eikä sitä voi asettaa",
        )
}
