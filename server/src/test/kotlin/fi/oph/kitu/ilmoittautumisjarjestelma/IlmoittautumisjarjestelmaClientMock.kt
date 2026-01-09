package fi.oph.kitu.ilmoittautumisjarjestelma

import fi.oph.kitu.TypedResult
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("test")
class IlmoittautumisjarjestelmaClientMock : IlmoittautumisjarjestelmaClient {
    val requests = mutableListOf<IlmoittautumisjarjestelmaRequest>()
    val defaultResponse: TypedResult<out IlmoittautumisjarjestelmaResponse, out IlmoittautumisjarjestelmaException> =
        TypedResult.Success(IlmoittautumisjarjestelmaResponse.ok(1))

    var response: TypedResult<out IlmoittautumisjarjestelmaResponse, out IlmoittautumisjarjestelmaException> =
        defaultResponse

    fun reset() {
        requests.clear()
        response = defaultResponse
    }

    fun latestRequest() = requests.lastOrNull()

    override fun <T> post(
        endpoint: String,
        body: IlmoittautumisjarjestelmaRequest,
        responseType: Class<T>,
    ): TypedResult<T, out IlmoittautumisjarjestelmaException> {
        requests.add(body)
        @Suppress("UNCHECKED_CAST")
        return response as TypedResult<T, out IlmoittautumisjarjestelmaException>
    }
}
