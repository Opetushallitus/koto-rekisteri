package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.Oid
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import java.time.LocalDate
import java.time.OffsetDateTime

data class YkiArvioija(
    val arvioijaOid: Oid,
    val henkilotunnus: String? = null,
    val sukunimi: String,
    val etunimet: String,
    val sahkopostiosoite: String? = null,
    val katuosoite: String? = null,
    val postinumero: String? = null,
    val postitoimipaikka: String? = null,
    val ensimmainenRekisterointipaiva: LocalDate,
    val kaudenAlkupaiva: LocalDate?,
    val kaudenPaattymispaiva: LocalDate?,
    val jatkorekisterointi: Boolean,
    val tila: YkiArvioijaTila,
    val kieli: Tutkintokieli,
    val tasot: Set<Tutkintotaso>,
) {
    fun toEntity(): YkiArvioijaEntity =
        YkiArvioijaEntity(
            id = null,
            rekisteriintuontiaika = OffsetDateTime.now(),
            arvioijanOppijanumero = arvioijaOid,
            henkilotunnus = henkilotunnus,
            sukunimi = sukunimi,
            etunimet = etunimet,
            sahkopostiosoite = sahkopostiosoite,
            katuosoite = katuosoite,
            postinumero = postinumero,
            postitoimipaikka = postitoimipaikka,
            ensimmainenRekisterointipaiva = ensimmainenRekisterointipaiva,
            kaudenAlkupaiva = kaudenAlkupaiva,
            kaudenPaattymispaiva = kaudenPaattymispaiva,
            jatkorekisterointi = jatkorekisterointi,
            tila = tila,
            kieli = kieli,
            tasot = tasot,
        )
}
