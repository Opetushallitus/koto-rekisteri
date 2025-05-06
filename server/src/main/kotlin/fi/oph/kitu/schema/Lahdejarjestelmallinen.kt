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
            return LahdejarjestelmanTunniste(tokens[1], Lahdejarjestelma.valueOf(tokens[0]))
        }
    }
}

enum class Lahdejarjestelma {
    KIOS,
}
