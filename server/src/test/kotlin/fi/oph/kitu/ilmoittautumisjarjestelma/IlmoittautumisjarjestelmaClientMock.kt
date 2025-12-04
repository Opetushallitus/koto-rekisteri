package fi.oph.kitu.ilmoittautumisjarjestelma

import fi.oph.kitu.TypedResult
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("test")
class IlmoittautumisjarjestelmaClientMock : IlmoittautumisjarjestelmaClient {
    val requests = mutableListOf<IlmoittautumisjarjestelmaRequest>()

    fun reset() {
        requests.clear()
    }

    fun latestRequest() = requests.lastOrNull()

    override fun <T> post(
        endpoint: String,
        body: IlmoittautumisjarjestelmaRequest,
        responseType: Class<T>,
    ): TypedResult<T, out IlmoittautumisjarjestelmaException> {
        requests.add(body)
        @Suppress("UNCHECKED_CAST")
        return TypedResult.Success(null as T)
    }
}
