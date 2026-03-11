package fi.oph.kitu.yki.suoritukset

import com.fasterxml.jackson.annotation.JsonValue
import fi.oph.kitu.koodisto.KoskiKoodiviite

enum class Todistuskieli(
    @get:JsonValue
    val solkiCode: String,
    val kieliKoodistoarvo: String,
) {
    FIN("fin", "FI"),
    SWE("swe", "SV"),
    ENG("eng", "EN"),
    ;

    fun toKoski() = KoskiKoodiviite(kieliKoodistoarvo, "kieli")
}
