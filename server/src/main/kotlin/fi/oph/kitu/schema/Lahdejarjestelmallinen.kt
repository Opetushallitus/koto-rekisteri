package fi.oph.kitu.schema

import com.fasterxml.jackson.annotation.JsonProperty

interface Lahdejarjestelmallinen {
    @get:JsonProperty("lähdejärjestelmänId")
    val lahdejarjestelmanId: LahdejarjestelmanTunniste
}

data class LahdejarjestelmanTunniste(
    val id: String,
    @get:JsonProperty("lähde")
    val lahde: Lahdejarjestelma,
) {
    override fun toString() = "$lahde:$id"

    companion object {
        fun from(s: String): LahdejarjestelmanTunniste {
            val tokens = s.split(":", limit = 2)
            return if (tokens.size > 1) {
                LahdejarjestelmanTunniste(tokens[1], Lahdejarjestelma.valueOf(tokens[0]))
            } else {
                LahdejarjestelmanTunniste(s, Lahdejarjestelma.Unknown)
            }
        }
    }
}

enum class Lahdejarjestelma {
    KIOS,
    Unknown,
}
