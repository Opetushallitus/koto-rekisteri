package fi.oph.kitu.organisaatiot

import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult
import fi.oph.kitu.defaultObjectMapper
import org.springframework.core.io.ClassPathResource

class MockOrganisaatioService : OrganisaatioService {
    override fun getOrganisaatio(oid: Oid): TypedResult<GetOrganisaatioResponse, OrganisaatiopalveluException> {
        val json = ClassPathResource("./opintopolku-mocks/organisaatio-service/api/GET-$oid.json").file
        return TypedResult.Success(defaultObjectMapper.readValue(json, GetOrganisaatioResponse::class.java))
    }
}
