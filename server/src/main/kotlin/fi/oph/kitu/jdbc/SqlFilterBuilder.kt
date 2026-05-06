package fi.oph.kitu.jdbc

class SqlFilterBuilder {
    private val clauses = mutableListOf<String>()
    private val params = mutableMapOf<String, Any?>()

    fun add(
        condition: String?,
        vararg bindings: Pair<String, Any?>,
    ) {
        if (condition != null) {
            clauses.add(condition)
            bindings.forEach { (name, value) -> params[name] = value }
        }
    }

    fun add(
        condition: String?,
        bindings: Map<String, Any?>,
    ) {
        if (condition != null) {
            clauses.add(condition)
            params.putAll(bindings)
        }
    }

    fun whereClauseOrNull(): String? =
        if (clauses.isEmpty()) null else "WHERE ${clauses.joinToString(" AND ") { "($it)" }}"

    fun params(): Map<String, Any?> = params.toMap()
}
