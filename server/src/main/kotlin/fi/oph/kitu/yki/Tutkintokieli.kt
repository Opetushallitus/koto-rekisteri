package fi.oph.kitu.yki

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import fi.oph.kitu.html.table.HideInTableFilter
import java.sql.ResultSet

/** ISO 639-2 Alpha 3
 *  Legacy langugage codes:
 *  10,Svenska,svenska,Swedish
 *  11,Kaupallinen englanti,företagsengelska,English for business
 *  12,Tekninen englanti,teknisk engelska,English for technology
 *  */
enum class Tutkintokieli(
    @get:JsonValue
    val solkiCode: String,
) {
    DEU("deu"),
    ENG("eng"),
    FIN("fin"),
    FRA("fra"),
    ITA("ita"),
    RUS("rus"),
    SME("sme"),
    SPA("spa"),
    SWE("swe"),

    @HideInTableFilter
    SWE10("swe10"),

    @HideInTableFilter
    ENG11("eng11"),

    @HideInTableFilter
    ENG12("eng12"),
    ;

    fun isLegacy(): Boolean = this in legacyEntries

    companion object {
        val legacyEntries = setOf(SWE10, ENG11, ENG12)
    }
}
