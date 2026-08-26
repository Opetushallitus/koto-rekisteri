package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.oid.Oid
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import java.time.LocalDate

enum class ArvioijaHakutapa {
    HETU,
    OPPIJANUMERO,
}

data class ArvioijaHakuFormData(
    val tapa: ArvioijaHakutapa = ArvioijaHakutapa.HETU,
    val hetu: String? = null,
    val sukunimi: String? = null,
    val etunimet: String? = null,
    val kutsumanimi: String? = null,
    val oppijanumero: String? = null,
) {
    fun toOnrHaku(): OnrHaku =
        OnrHaku(
            tapa = tapa,
            hetu = hetu,
            etunimet = etunimet,
            sukunimi = sukunimi,
            kutsumanimi = kutsumanimi,
            oppijanumero = oppijanumero,
        )
}

data class ArvioijaFormData(
    val arvioijaOid: String? = null,
    val sukunimi: String? = null,
    val etunimet: String? = null,
    val sahkopostiosoite: String? = null,
    val katuosoite: String? = null,
    val postinumero: String? = null,
    val postitoimipaikka: String? = null,
    val kaudenAlkupaiva: LocalDate? = null,
    val jatkorekisterointi: Boolean = false,
    val tila: YkiArvioijaTila = YkiArvioijaTila.AKTIIVINEN,
    val ashaNumero: String? = null,
    val turvakielto: Turvakieltotieto = Turvakieltotieto.EI,
    val onOlemassa: Boolean = false,
    val arviointioikeus: List<String>? = null,
) {
    fun laskettuPaattymispaiva(): LocalDate? = kaudenAlkupaiva?.let(Rekisterikausi::paattymispaiva)

    fun arviointioikeudet(): List<TallennaArvioija.Arviointioikeus> =
        arviointioikeus
            .orEmpty()
            .mapNotNull(::parseValinta)
            .groupBy({ it.first }, { it.second })
            .map { (kieli, tasot) -> TallennaArvioija.Arviointioikeus(kieli, tasot.toSet()) }

    fun toCommand(
        oid: Oid,
        alkupaiva: LocalDate,
    ): TallennaArvioija =
        TallennaArvioija(
            arvioijaOid = oid,
            sukunimi = sukunimi.orEmpty().trim(),
            etunimet = etunimet.orEmpty().trim(),
            sahkopostiosoite = sahkopostiosoite?.trim()?.takeIf { it.isNotEmpty() },
            katuosoite = katuosoite.orEmpty().trim(),
            postinumero = postinumero.orEmpty().trim(),
            postitoimipaikka = postitoimipaikka.orEmpty().trim(),
            kaudenAlkupaiva = alkupaiva,
            jatkorekisterointi = jatkorekisterointi,
            tila = tila,
            ashaNumero = ashaNumero?.trim()?.takeIf { it.isNotEmpty() },
            arviointioikeudet = arviointioikeudet(),
        )

    companion object {
        fun of(esitaytto: ArvioijanEsitaytto): ArvioijaFormData {
            val merkinta = esitaytto.olemassaolevaMerkinta
            val pohja =
                merkinta?.let { of(it, esitaytto.turvakielto) }
                    ?: ArvioijaFormData(turvakielto = esitaytto.turvakielto)

            // ONR omistaa henkilotiedot, mutta tyhja ONR-kentta ei saa pyyhkia rekisterin arvoa.
            return pohja.copy(
                arvioijaOid = esitaytto.arvioijaOid.toString(),
                sukunimi = esitaytto.sukunimi.tai(pohja.sukunimi),
                etunimet = esitaytto.etunimet.tai(pohja.etunimet),
                sahkopostiosoite = esitaytto.sahkopostiosoite.tai(pohja.sahkopostiosoite),
                katuosoite = esitaytto.katuosoite.tai(pohja.katuosoite),
                postinumero = esitaytto.postinumero.tai(pohja.postinumero),
                postitoimipaikka = esitaytto.postitoimipaikka.tai(pohja.postitoimipaikka),
                jatkorekisterointi = merkinta != null,
                onOlemassa = merkinta != null,
            )
        }

        private fun String?.tai(vara: String?): String? = this?.takeIf { it.isNotBlank() } ?: vara

        fun of(
            arvioija: YkiArvioijaEntity,
            turvakielto: Turvakieltotieto = Turvakieltotieto.EI,
        ): ArvioijaFormData {
            val oikeudet = arvioija.arviointioikeudet
            return ArvioijaFormData(
                arvioijaOid = arvioija.arvioijaOid.toString(),
                sukunimi = arvioija.sukunimi,
                etunimet = arvioija.etunimet,
                sahkopostiosoite = arvioija.sahkopostiosoite,
                katuosoite = arvioija.katuosoite,
                postinumero = arvioija.postinumero,
                postitoimipaikka = arvioija.postitoimipaikka,
                kaudenAlkupaiva = oikeudet.firstNotNullOfOrNull { it.kaudenAlkupaiva },
                jatkorekisterointi = oikeudet.any { it.jatkorekisterointi },
                tila = oikeudet.firstOrNull()?.tila ?: YkiArvioijaTila.AKTIIVINEN,
                ashaNumero = arvioija.ashaNumero,
                turvakielto = turvakielto,
                onOlemassa = true,
                arviointioikeus =
                    oikeudet.flatMap { oikeus ->
                        oikeus.tasot.map { taso -> valinta(oikeus.kieli, taso) }
                    },
            )
        }

        fun valinta(
            kieli: Tutkintokieli,
            taso: Tutkintotaso,
        ): String = "${kieli.name}:${taso.name}"

        private fun parseValinta(arvo: String): Pair<Tutkintokieli, Tutkintotaso>? {
            val osat = arvo.split(":")
            if (osat.size != 2) return null
            val kieli = Tutkintokieli.entries.firstOrNull { it.name == osat[0] } ?: return null
            val taso = Tutkintotaso.entries.firstOrNull { it.name == osat[1] } ?: return null
            return kieli to taso
        }
    }
}
