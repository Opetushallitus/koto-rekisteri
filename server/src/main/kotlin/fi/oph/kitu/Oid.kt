package fi.oph.kitu

import org.ietf.jgss.GSSException

@ConsistentCopyVisibility
data class Oid private constructor(
    private val value: org.ietf.jgss.Oid,
) {
    companion object {
        fun valueOf(source: String): Oid? =
            try {
                Oid(org.ietf.jgss.Oid(source))
            } catch (_: GSSException) {
                null
            }

        fun valueOfOrThrow(source: String): Oid = valueOf(source)!!
    }

    override fun toString(): String = value.toString()
}
