package fi.oph.kitu.yki

import java.sql.ResultSet

/** ISO 639-2 Alpha 3
 *  Legacy langugage codes:
 *  10,Svenska,svenska,Swedish
 *  11,Kaupallinen englanti,företagsengelska,English for business
 *  12,Tekninen englanti,teknisk engelska,English for technology
 *  */
enum class Tutkintokieli {
    DEU,
    ENG,
    FIN,
    FRA,
    ITA,
    RUS,
    SME,
    SPA,
    SWE,
    SWE10,
    ENG11,
    ENG12,
}

fun ResultSet.getTutkintokieli(columnLabel: String): Tutkintokieli = Tutkintokieli.valueOf(getString(columnLabel))
