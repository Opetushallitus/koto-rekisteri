package fi.oph.kitu.yki.suoritukset.error

enum class YkiSuoritusErrorColumn(
    val entityName: String,
    val uiHeaderValue: String,
) {
    Message(entityName = "message", uiHeaderValue = "Viesti"),
    Context(entityName = "context", uiHeaderValue = "Konteksti"),
    ExceptionMessage(entityName = "exception", uiHeaderValue = "virhe"),
    StackTrace(entityName = "stackTrace", uiHeaderValue = "stack trace"),
    Created(entityName = "created", uiHeaderValue = "luontiaika"),
    ;

    fun lowercaseName(): String = name.lowercase()
}
