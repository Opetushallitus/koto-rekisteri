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

    /**
     * @param aiemmatTilat kielikohtainen tila ennestaan olevilta riveilta. Lomake ei kanna tilaa,
     *   joten ilman tata muokkaus aktivoisi passivoidun merkinnan uudelleen.
     */
    fun toEntity(
        ensimmainenRekisterointipaiva: LocalDate = kaudenAlkupaiva,
        aiemmatTilat: Map<Tutkintokieli, YkiArvioijaTila> = emptyMap(),
    ): YkiArvioijaEntity =
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
                        tila = aiemmatTilat[oikeus.kieli] ?: tila,
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

/**
 * Henkilotiedot tulevat ONR:sta, rekisterimerkinnan tiedot kitusta. Jos arvioija loytyy jo
 * rekisterista, [olemassaolevaMerkinta] kantaa hanen nykyiset arviointioikeutensa, ASHA-numeronsa
 * ja kautensa lomakkeelle — muuten tallennus pyyhkisi ne.
 */
data class ArvioijanEsitaytto(
    val arvioijaOid: Oid,
    val sukunimi: String,
    val etunimet: String,
    val sahkopostiosoite: String?,
    val katuosoite: String?,
    val postinumero: String?,
    val postitoimipaikka: String?,
    val turvakielto: Turvakieltotieto,
    val olemassaolevaMerkinta: YkiArvioijaEntity?,
)
