package fi.oph.kitu.kotoutumiskoulutus.suoritukset

import com.fasterxml.jackson.annotation.JsonValue

enum class Arvosana(
    @get:JsonValue
    val arvosana: String,
) {
    ALLEA1("Alle A1"),
    A1("A1"),
    A2("A2"),
    B1("B1"),
    YLIB1("Yli B1"),
    EVA("EVA"),
    ;

    override fun toString() = arvosana

    companion object {
        fun fromString(str: String): Arvosana =
            entries.find { it.arvosana == str } ?: throw IllegalArgumentException("Unknown arvosana $str")
    }
}
