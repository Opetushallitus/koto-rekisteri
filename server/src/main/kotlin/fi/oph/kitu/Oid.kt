package fi.oph.kitu

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import fi.oph.kitu.TypedResult.Failure
import fi.oph.kitu.TypedResult.Success
import org.ietf.jgss.GSSException

@ConsistentCopyVisibility
@JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer::class)
data class Oid private constructor(
    private val value: org.ietf.jgss.Oid,
) {
    companion object {
        fun parse(source: String?): Result<Oid> =
            try {
                Result.success(Oid(org.ietf.jgss.Oid(source)))
            } catch (_: GSSException) {
                Result.failure(MalformedOidError(source))
            }

        fun parseTyped(source: String): TypedResult<Oid, MalformedOidError> =
            try {
                Success(Oid(org.ietf.jgss.Oid(source)))
            } catch (_: GSSException) {
                Failure(MalformedOidError(source))
            }
    }

    fun unwrap(): org.ietf.jgss.Oid = value

    override fun toString(): String = value.toString()
}

data class MalformedOidError(
    val source: String?,
) : Exception("Malformed Oid \"$source\"")
