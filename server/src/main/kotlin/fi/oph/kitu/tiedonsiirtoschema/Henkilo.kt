package fi.oph.kitu.tiedonsiirtoschema

import fi.oph.kitu.Oid
import fi.oph.kitu.oppijanumero.OppijanumeroService
import fi.oph.kitu.yki.Sukupuoli

data class Henkilo(
    val oid: Oid,
    val etunimet: String? = null,
    val sukunimi: String? = null,
    val hetu: String? = null,
    val sukupuoli: Sukupuoli? = null,
    val kansalaisuus: String? = null,
    val katuosoite: String? = null,
    val postinumero: String? = null,
    val postitoimipaikka: String? = null,
    val email: String? = null,
) {
    fun kokoNimi(): String = "$sukunimi, $etunimet"

    fun fill(onr: OppijanumeroService): Henkilo? =
        if (etunimet != null || sukunimi != null) {
            onr
                .getHenkilo(oid)
                .map { tiedot ->
                    copy(
                        etunimet = tiedot.etunimet,
                        sukunimi = tiedot.sukunimi,
                    )
                }.getOrNull()
        } else {
            this
        }
}
