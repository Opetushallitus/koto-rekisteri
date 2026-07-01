package fi.oph.kitu.util

import fi.oph.kitu.dev.mockdata.OidClass
import fi.oph.kitu.oid.Oid.Companion.isOidOfClass

data class SearchTerms(
    val query: String?,
) {
    val allTerms by lazy {
        query
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.split(Regex("[\\s,;]+"))
            .orEmpty()
    }

    val termsByKind: Map<TermKind, List<String>> by lazy {
        allTerms.groupBy { TermKind.from(it) }
    }

    fun henkiloOids(): List<String>? = termsByKind[TermKind.HenkiloOid]

    fun orgOids(): List<String>? = termsByKind[TermKind.OrgOid]

    fun numbers(): List<Int>? = termsByKind[TermKind.Number]?.map { it.toInt() }

    fun texts(): List<String>? = termsByKind[TermKind.Text]

    companion object {
        enum class TermKind {
            HenkiloOid,
            OrgOid,
            Number,
            Text,
            ;

            companion object {
                fun from(term: String): TermKind =
                    when {
                        term.isOidOfClass(OidClass.OPPIJA) -> HenkiloOid
                        term.isOidOfClass(OidClass.ORG) -> OrgOid
                        term.toIntOrNull() != null -> Number
                        else -> Text
                    }
            }
        }
    }
}
