package fi.oph.kitu

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import fi.oph.kitu.TypedResult.Failure
import fi.oph.kitu.TypedResult.Success
import org.ietf.jgss.GSSException
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.sql.ResultSet

@ConsistentCopyVisibility
@JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer::class)
data class Oid private constructor(
    private var value: org.ietf.jgss.Oid,
) : Serializable {
    override fun toString(): String = value.toString()

    constructor(valueString: String) : this(org.ietf.jgss.Oid(valueString))

    @Throws(IOException::class)
    private fun writeObject(out: ObjectOutputStream) {
        out.writeUTF(value.toString())
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(inp: ObjectInputStream) {
        val str = inp.readUTF()
        value = org.ietf.jgss.Oid(str)
    }

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
}

data class MalformedOidError(
    val source: String?,
) : Exception("Malformed Oid \"$source\"")

fun ResultSet.getOid(columnLabel: String): Oid = Oid.parse(getString(columnLabel)).getOrThrow()
