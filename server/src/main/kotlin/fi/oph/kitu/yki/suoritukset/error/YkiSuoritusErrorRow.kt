package fi.oph.kitu.yki.suoritukset.error

import java.time.Instant

/**
 * Represents the YKI Suoritus Error - row in UI.
 */
data class YkiSuoritusErrorRow(
    val oid: String,
    val hetu: String,
    val nimi: String,
    val virheellinenKentta: String,
    val virheellinenArvo: String,
    val virheellinenSarake: String,
    val virheenLuontiaika: Instant,
)
