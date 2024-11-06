package fi.oph.kitu.yki

import java.sql.ResultSet

/** ISO 649-2 Alpha 3 */
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
}

fun ResultSet.getTutkintokieli(columnLabel: String): Tutkintokieli = Tutkintokieli.valueOf(getString(columnLabel))
