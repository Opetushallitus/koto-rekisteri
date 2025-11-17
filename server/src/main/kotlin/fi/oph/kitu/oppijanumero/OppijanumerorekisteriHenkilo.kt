package fi.oph.kitu.oppijanumero

import java.time.LocalDate
import java.time.OffsetDateTime

data class OppijanumerorekisteriHenkilo(
    val oidHenkilo: String?,
    val hetu: String?,
    val kaikkiHetut: List<String>?,
    val passivoitu: Boolean?,
    val etunimet: String?,
    val kutsumanimi: String?,
    val sukunimi: String?,
    val aidinkieli: Kieli?,
    val asiointiKieli: Kieli?,
    val kansalaisuus: List<Kansalaisuus>?,
    val kasittelijaOid: String?,
    val syntymaaika: LocalDate?,
    val sukupuoli: String?,
    val kotikunta: String?,
    val oppijanumero: String?,
    val turvakielto: Boolean?,
    val eiSuomalaistaHetua: Boolean?,
    val yksiloity: Boolean?,
    val yksiloityVTJ: Boolean?,
    val yksilointiYritetty: Boolean?,
    val duplicate: Boolean?,
    val created: OffsetDateTime?,
    val modified: OffsetDateTime?,
    val vtjsynced: OffsetDateTime?,
    val yhteystiedotRyhma: List<Yhteystietoryhma>?,
    val yksilointivirheet: List<Yksilointivirhe>?,
    val passinumerot: List<String>?,
) {
    fun hetut() = listOfNotNull(hetu) + (kaikkiHetut.orEmpty())

    fun kokoNimi() = "$etunimet $sukunimi"

    data class Kieli(
        val kieliKoodi: String?,
        val kieliTyyppi: String?,
    )

    data class Kansalaisuus(
        val kansalaisuusKoodi: String?,
    )

    data class Yhteystietoryhma(
        val id: Long?,
        val ryhmaKuvaus: String?,
        val ryhmaAlkuperaTieto: String?,
        val readOnly: Boolean?,
        val yhteystieto: List<Yhteystieto>?,
    ) {
        data class Yhteystieto(
            val yhteystietoTyyppi: String?,
            val yhteystietoArvo: String?,
        )
    }

    data class Yksilointivirhe(
        val yksilointivirheTila: String?,
        val uudelleenyritysAikaleima: OffsetDateTime?,
    )
}
