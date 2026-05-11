package fi.oph.kitu.ilmoittautumisjarjestelma

import arrow.core.Either
import arrow.core.right
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("test")
class IlmoittautumisjarjestelmaClientMock : IlmoittautumisjarjestelmaClient {
    val requests = mutableListOf<IlmoittautumisjarjestelmaRequest>()
    val defaultResponse: Either<IlmoittautumisjarjestelmaException, IlmoittautumisjarjestelmaResponse> =
        IlmoittautumisjarjestelmaResponse.ok(1).right()

    var response: Either<IlmoittautumisjarjestelmaException, IlmoittautumisjarjestelmaResponse> = defaultResponse

    fun reset() {
        requests.clear()
        response = defaultResponse
    }

    fun latestRequest() = requests.lastOrNull()

    override fun <T> post(
        endpoint: String,
        body: IlmoittautumisjarjestelmaRequest,
        responseType: Class<T>,
    ): Either<IlmoittautumisjarjestelmaException, T> {
        requests.add(body)
        @Suppress("UNCHECKED_CAST")
        return response as Either<IlmoittautumisjarjestelmaException, T>
    }
}
