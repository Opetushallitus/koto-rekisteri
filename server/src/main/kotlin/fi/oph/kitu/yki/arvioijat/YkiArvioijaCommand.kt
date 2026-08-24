package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.oid.Oid
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import java.time.LocalDate

data class TallennaArvioija(
    val arvioijaOid: Oid,
    val sukunimi: String,
    val etunimet: String,
    val sahkopostiosoite: String?,
    val katuosoite: String,
    val postinumero: String,
    val postitoimipaikka: String,
    val kaudenAlkupaiva: LocalDate,
    val jatkorekisterointi: Boolean,
    val tila: YkiArvioijaTila,
    val ashaNumero: String?,
    val arviointioikeudet: List<Arviointioikeus>,
) {
    val kaudenPaattymispaiva: LocalDate get() = Rekisterikausi.paattymispaiva(kaudenAlkupaiva)

    fun toEntity(ensimmainenRekisterointipaiva: LocalDate = kaudenAlkupaiva): YkiArvioijaEntity =
        YkiArvioijaEntity(
            id = null,
            arvioijaOid = arvioijaOid,
            henkilotunnus = null,
            sukunimi = sukunimi,
            etunimet = etunimet,
            sahkopostiosoite = sahkopostiosoite,
            katuosoite = katuosoite,
            postinumero = postinumero,
            postitoimipaikka = postitoimipaikka,
            ashaNumero = ashaNumero,
            arviointioikeudet =
                arviointioikeudet.map { oikeus ->
                    YkiArviointioikeusEntity(
                        id = null,
                        arvioijaId = null,
                        kieli = oikeus.kieli,
                        tasot = oikeus.tasot,
                        tila = tila,
                        kaudenAlkupaiva = kaudenAlkupaiva,
                        kaudenPaattymispaiva = kaudenPaattymispaiva,
                        jatkorekisterointi = jatkorekisterointi,
                        ensimmainenRekisterointipaiva = ensimmainenRekisterointipaiva,
                        rekisteriintuontiaika = null,
                    )
                },
        )

    data class Arviointioikeus(
        val kieli: Tutkintokieli,
        val tasot: Set<Tutkintotaso>,
    )
}

data class OnrHaku(
    val tapa: ArvioijaHakutapa = ArvioijaHakutapa.HETU,
    val hetu: String? = null,
    val etunimet: String? = null,
    val sukunimi: String? = null,
    val kutsumanimi: String? = null,
    val oppijanumero: String? = null,
)

data class ArvioijanEsitaytto(
    val arvioijaOid: Oid,
    val sukunimi: String,
    val etunimet: String,
    val sahkopostiosoite: String?,
    val katuosoite: String?,
    val postinumero: String?,
    val postitoimipaikka: String?,
    val turvakielto: Boolean,
    val jatkorekisterointi: Boolean,
)
