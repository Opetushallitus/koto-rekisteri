package fi.oph.kitu.vkt.tiedonsiirtoschema

import fi.oph.kitu.oppijanumero.OppijanumeroService
import fi.oph.kitu.vkt.VktSuoritusEntity

data class OidOppija(
    val oid: OidString,
    val etunimet: String? = null,
    val sukunimi: String? = null,
) {
    fun kokoNimi(): String = "$sukunimi, $etunimet"

    fun fill(onr: OppijanumeroService): OidOppija? =
        oid.toOid().getOrNull()?.let { oid ->
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

    companion object {
        fun from(entity: VktSuoritusEntity) =
            OidOppija(
                oid = OidString.from(entity.suorittajanOppijanumero),
                etunimet = entity.etunimi,
                sukunimi = entity.sukunimi,
            )
    }
}
