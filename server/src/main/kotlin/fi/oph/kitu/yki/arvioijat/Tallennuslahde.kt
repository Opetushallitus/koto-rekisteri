package fi.oph.kitu.yki.arvioijat

/**
 * Kumpi jarjestelma tuotti tallennettavan muutoksen. Ratkaisee kaksi asiaa: poistetaanko
 * payloadista puuttuvat arviointioikeudet ja jaako rivi Solki-lahetysjonoon.
 */
enum class Tallennuslahde {
    /** Virkailijan syotto kitussa: kitu on master, ja muutos on lahetettava Solkiin. */
    KITU,

    /** Solkin sisaantuleva push: data on jo Solkissa, eika sita lahetata takaisin. */
    SOLKI,
}
