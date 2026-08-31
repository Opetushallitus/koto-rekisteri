package fi.oph.kitu.yki.arvioijat

/**
 * Arvioijarekisterimerkinnan sailytysaika. Laskenta alkaa hetkesta, jona merkinta muuttui
 * passiiviseksi: hallintopaatoksella passivoidulla se on [YkiArvioijaEntity.passivoitu], kauden
 * umpeutumiseen paattyneella kauden viimeinen voimassaolopaiva. Jos kumpaakaan ei ole tiedossa,
 * merkinta ei vanhene lainkaan.
 */
object Sailytysaika {
    const val VUOSIA = 5L

    /** Alkuhetki SQL:na; sama COALESCE molemmissa kyselyissa, jottei suoja ja poisto eroa. */
    const val ALKUHETKI_SQL = """
        COALESCE(
            yki_arvioija.passivoitu::date,
            (
                SELECT max(kauden_paattymispaiva)
                FROM yki_arviointioikeus
                WHERE yki_arviointioikeus.arvioija_id = yki_arvioija.id
            )
        )
    """
}
