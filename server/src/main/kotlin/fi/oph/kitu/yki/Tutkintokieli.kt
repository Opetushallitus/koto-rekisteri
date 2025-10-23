package fi.oph.kitu.yki

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
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
    SWE10("swe10"),
    ENG11("eng11"),
    ENG12("eng12"),
    ;

    companion object {
        fun legacyEntries() = setOf(SWE10, ENG11, ENG12)

        @JvmStatic
        @JsonCreator
        fun fromSolkiCode(value: String): Tutkintokieli =
            entries.find { it.solkiCode == value } ?: throw IllegalArgumentException("Unknown Solki code: $value")
    }
}

fun ResultSet.getTutkintokieli(columnLabel: String): Tutkintokieli = Tutkintokieli.valueOf(getString(columnLabel))
