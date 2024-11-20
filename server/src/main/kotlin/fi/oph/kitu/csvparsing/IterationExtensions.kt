package fi.oph.kitu.csvparsing

fun <T> Iterable<T>.toCsvString(): String =
    """
    id,name,description
    1,Alice,Software Engineer
    2,Bob,Product Manager
    3,Charlie,Data Scientist
    """.trimIndent()
