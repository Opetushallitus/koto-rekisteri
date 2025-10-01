package fi.oph.kitu.tiedonsiirtoschema

import fi.oph.kitu.Oid
import fi.oph.kitu.oppijanumero.OppijanumeroService
import fi.oph.kitu.vkt.VktSuoritusEntity

data class OidOppija(
    val oid: Oid,
    val etunimet: String? = null,
    val sukunimi: String? = null,
) {
    fun kokoNimi(): String = "$sukunimi, $etunimet"

    fun fill(onr: OppijanumeroService): OidOppija? =
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

    companion object {
        fun from(entity: VktSuoritusEntity) =
            OidOppija(
                oid = entity.suorittajanOppijanumero,
                etunimet = entity.etunimi,
                sukunimi = entity.sukunimi,
            )
    }
}
