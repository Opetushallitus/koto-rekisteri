package fi.oph.kitu.yki.arvioijat.solki

import org.springframework.http.ResponseEntity

sealed class SolkiArvioijaException(
    val oppijanumero: String,
    val response: ResponseEntity<String>?,
    message: String,
    cause: Throwable? = null,
) : Throwable(message, cause) {
    class BadRequest(
        oppijanumero: String,
        response: ResponseEntity<String>,
    ) : SolkiArvioijaException(oppijanumero, response, "Bad request")

    class Unauthorized(
        oppijanumero: String,
        response: ResponseEntity<String>,
    ) : SolkiArvioijaException(oppijanumero, response, "Unauthorized")

    class UnexpectedError(
        oppijanumero: String,
        response: ResponseEntity<String>,
    ) : SolkiArvioijaException(oppijanumero, response, "Unexpected error")

    class NullResponse(
        oppijanumero: String,
    ) : SolkiArvioijaException(oppijanumero, null, "Empty or unserializable response")

    /**
     * Poikkeama KIOS-mallista: pyyntorunkoa **ei** serialisoida. Osoite ja sahkoposti ovat
     * henkilotietoa, joka paatyisi lokeihin ja virhesarakkeeseen, josta se nakyy myos
     * virhenakymassa.
     */
    fun debugString(): String =
        listOfNotNull(
            message,
            "oppijanumero: $oppijanumero",
            response?.statusCode?.let { "response status: $it" },
            response?.body?.let { "response body: $it" },
            cause?.let { "cause: $it" },
        ).joinToString("; ")
}
