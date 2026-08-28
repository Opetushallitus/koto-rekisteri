package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.html.table.Nimetty
import fi.oph.kitu.i18n.LocalizedString
import fi.oph.kitu.i18n.UiText
import java.time.LocalDate

/**
 * Arviointioikeuden tila laskettuna rekisterointikauden paivista. Jarjestys vastaa [SQL]:n
 * palauttamaa tekstijarjestysta, jotta listanakyman lajittelu on sama molemmissa.
 */
enum class Rekisterointitila : Nimetty {
    AKTIIVINEN,
    PASSIVOITU,
    TULEVAISUUDESSA,
    ;

    override val nimi: LocalizedString
        get() =
            when (this) {
                AKTIIVINEN -> UiText.Yki.ArvioijaTila.aktiivinen
                PASSIVOITU -> UiText.Yki.ArvioijaTila.passivoitu
                TULEVAISUUDESSA -> UiText.Yki.ArvioijaTila.tulevaisuudessa
            }

    companion object {
        /**
         * Tallennetuista tiloista vain [YkiArvioijaTila.PASSIVOITU] ohittaa laskennan: se on
         * Solkin nimenomainen kannanotto. Tallennettu AKTIIVINEN on V117:n taytearvo, jonka
         * kunnioittaminen jattaisi koko rekisterin laskennan ulkopuolelle.
         */
        fun laske(
            tallennettu: YkiArvioijaTila?,
            kaudenAlkupaiva: LocalDate?,
            kaudenPaattymispaiva: LocalDate?,
            tanaan: LocalDate,
        ): Rekisterointitila =
            when {
                tallennettu == YkiArvioijaTila.PASSIVOITU -> PASSIVOITU
                kaudenPaattymispaiva != null && kaudenPaattymispaiva < tanaan -> PASSIVOITU
                kaudenAlkupaiva != null && kaudenAlkupaiva > tanaan -> TULEVAISUUDESSA
                else -> AKTIIVINEN
            }

        fun laske(
            oikeus: YkiArviointioikeusEntity,
            tanaan: LocalDate,
        ): Rekisterointitila = laske(oikeus.tila, oikeus.kaudenAlkupaiva, oikeus.kaudenPaattymispaiva, tanaan)

        fun laske(
            kausi: YkiArvioijaKausiEntity,
            tanaan: LocalDate,
        ): Rekisterointitila = laske(kausi.tila, kausi.kaudenAlkupaiva, kausi.kaudenPaattymispaiva, tanaan)

        /**
         * [laske] SQL:na. Jokainen haara palauttaa text-literaalin: jos ensimmainen palauttaisi
         * sarakkeen, CASE ratkeaisi tyypiksi yki_arvioija_tila eika 'TULEVAISUUDESSA' olisi
         * validi enum-labeli. Paiva sidotaan parametrina, koska CURRENT_DATE on kannan
         * aikavyohykkeessa eika kiinnitetty testikello ohjaa sita.
         */
        const val SQL = """
            CASE
                WHEN yki_arviointioikeus.tila = 'PASSIVOITU'
                    THEN 'PASSIVOITU'
                WHEN yki_arviointioikeus.kauden_paattymispaiva IS NOT NULL
                     AND yki_arviointioikeus.kauden_paattymispaiva < :tanaan
                    THEN 'PASSIVOITU'
                WHEN yki_arviointioikeus.kauden_alkupaiva IS NOT NULL
                     AND yki_arviointioikeus.kauden_alkupaiva > :tanaan
                    THEN 'TULEVAISUUDESSA'
                ELSE 'AKTIIVINEN'
            END
        """
    }
}
