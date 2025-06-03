package fi.oph.kitu.vkt.tiedonsiirtoschema

import fi.oph.kitu.vkt.VktSuoritusEntity

data class OidOppija(
    val oid: OidString,
    val etunimet: String? = null,
    val sukunimi: String? = null,
) {
    fun kokoNimi(): String = "$sukunimi, $etunimet"

    companion object {
        fun from(entity: VktSuoritusEntity) =
            OidOppija(
                oid = OidString.from(entity.suorittajanOppijanumero),
                etunimet = entity.etunimi,
                sukunimi = entity.sukunimi,
            )
    }
}
