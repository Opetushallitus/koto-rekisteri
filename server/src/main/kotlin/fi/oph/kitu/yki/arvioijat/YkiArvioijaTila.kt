package fi.oph.kitu.yki.arvioijat

/**
 * Kantaan tallennettu tila. Vanhentunut: kitu kirjoittaa aina NULLin ja tila lasketaan
 * kauden paivista, ks. [Rekisterointitila]. Arvon kirjoittaa enaa Solkin sisaantuleva push,
 * ja laskennassa siita vaikuttaa vain [PASSIVOITU].
 */
enum class YkiArvioijaTila {
    AKTIIVINEN,
    PASSIVOITU,
}
