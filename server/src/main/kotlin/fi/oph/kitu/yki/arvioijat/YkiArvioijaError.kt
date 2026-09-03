package fi.oph.kitu.yki.arvioijat

import arrow.core.NonEmptyList
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.oppijanumero.OppijanumeroException
import fi.oph.kitu.util.validation.Validation.ValidationError

sealed interface YkiArvioijaError {
    data class Validointivirheet(
        val virheet: NonEmptyList<ValidationError>,
    ) : YkiArvioijaError

    data class OppijanumeroaEiSaatu(
        val syy: OppijanumeroException,
    ) : YkiArvioijaError

    data class OppijaaEiYksiloity(
        val oid: Oid?,
    ) : YkiArvioijaError

    data object ArvioijaaEiLoydy : YkiArvioijaError

    data object KauttaEiLoydy : YkiArvioijaError

    /** Toinen kayttaja ehti tallentaa muutoksensa sen jalkeen kun lomake avattiin. */
    data object MuokattuSamanaikaisesti : YkiArvioijaError
}
