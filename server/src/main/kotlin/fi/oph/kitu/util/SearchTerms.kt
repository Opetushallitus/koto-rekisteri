package fi.oph.kitu.util

import fi.oph.kitu.dev.mockdata.OidClass
import fi.oph.kitu.oid.Oid.Companion.isOidOfClass

data class SearchTerms(
    val terms: Map<TermKind, List<String>>,
) {
    val allTerms: List<String> by lazy { terms.values.flatten() }

    fun henkiloOids(): List<String>? = terms[TermKind.HenkiloOid]

    fun orgOids(): List<String>? = terms[TermKind.OrgOid]

    fun numbers(): List<Int>? = terms[TermKind.Number]?.map { it.toInt() }

    fun texts(): List<String>? = terms[TermKind.Text]

    companion object {
        fun from(
            query: String?,
            extend: Map<TermKind, List<String>> = emptyMap(),
        ): SearchTerms =
            SearchTerms(
                query
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.split(Regex("[\\s,;]+"))
                    .orEmpty()
                    .groupBy { TermKind.from(it) }
                    .let { grouped ->
                        (grouped.keys + extend.keys).associateWith { key ->
                            (grouped[key].orEmpty() + extend[key].orEmpty())
                        }
                    },
            )

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
