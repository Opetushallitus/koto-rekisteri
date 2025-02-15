package fi.oph.kitu

import org.ietf.jgss.GSSException

data class Oid(
    private val value: org.ietf.jgss.Oid,
) {
    companion object {
        fun valueOf(source: String): Oid? =
            try {
                Oid(org.ietf.jgss.Oid(source))
            } catch (_: GSSException) {
                null
            }
    }

    override fun toString(): String = value.toString()
}
