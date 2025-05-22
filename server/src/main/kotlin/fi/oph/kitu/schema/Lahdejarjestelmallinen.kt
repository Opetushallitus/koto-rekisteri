package fi.oph.kitu.schema

interface Lahdejarjestelmallinen {
    val lahdejarjestelmanId: LahdejarjestelmanTunniste
}

data class LahdejarjestelmanTunniste(
    val id: String,
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
