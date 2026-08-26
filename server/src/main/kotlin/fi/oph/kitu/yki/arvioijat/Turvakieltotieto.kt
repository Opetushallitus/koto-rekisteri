package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.i18n.LocalizedString
import fi.oph.kitu.i18n.UiText

/**
 * Turvakieltoa ei talleteta kituun vaan se kysytaan ONR:sta joka renderoinnissa. Kysely voi myos
 * epaonnistua, ja se on eri asia kuin "ei turvakieltoa": ilman erottelua ONR-katko nayttaisi
 * yhteystiedot ilman varoitusta.
 */
enum class Turvakieltotieto {
    EI,
    ON,
    EI_TIEDOSSA,
    ;

    val varoitus: LocalizedString?
        get() =
            when (this) {
                EI -> null
                ON -> UiText.Yki.Arvioija.turvakielto
                EI_TIEDOSSA -> UiText.Yki.Arvioija.turvakieltoEiTiedossa
            }
}
