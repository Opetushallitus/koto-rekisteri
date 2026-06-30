package fi.oph.kitu.jdbc

inline fun <reified T : Enum<T>> List<T>.sqlList() =
    joinToString(
        separator = ",",
        prefix = "'{",
        postfix = "}'",
    ) { it.name }

inline fun <reified T : Enum<T>> List<T>.sqlAll() = "all(${sqlList()})"
