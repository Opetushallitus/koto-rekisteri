package fi.oph.kitu.vkt.tiedonsiirtoschema

import com.fasterxml.jackson.annotation.JsonValue
import fi.oph.kitu.Oid

data class OidString(
    @JsonValue
    val oid: String,
) {
    fun toOid(): Result<Oid> = Oid.parse(oid)

    override fun toString() = oid

    companion object {
        fun from(o: Oid): OidString = OidString(o.toString())
    }
}
