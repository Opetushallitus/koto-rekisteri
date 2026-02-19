package fi.oph.kitu.yki.suoritukset

import com.fasterxml.jackson.annotation.JsonValue

enum class Todistuskieli(
    @get:JsonValue
    val solkiCode: String,
) {
    FIN("fin"),
    SWE("swe"),
    ENG("eng"),
}
