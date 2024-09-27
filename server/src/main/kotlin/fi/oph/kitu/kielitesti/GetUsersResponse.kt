package fi.oph.kitu.kielitesti

import fi.oph.kitu.oppija.Oppija

sealed class GetUsersResponse {
    data class Success(
        val users: List<Oppija>,
    ) : GetUsersResponse()

    data class Failure(
        val error: MoodleError,
    ) : GetUsersResponse()
}
