package fi.oph.kitu.organisaatiot

import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult
import fi.oph.kitu.defaultObjectMapper
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

@Service
@Profile("test")
class MockOrganisaatioService : OrganisaatioService() {
    override fun getOrganisaatio(oid: Oid): TypedResult<GetOrganisaatioResponse, OrganisaatiopalveluException> {
        val json = ClassPathResource("./opintopolku-mocks/organisaatio-service/api/GET-$oid.json").file
        return TypedResult.Success(defaultObjectMapper.readValue(json, GetOrganisaatioResponse::class.java))
    }

    override fun getOrganisaatiohierarkia(
        aktiiviset: Boolean,
        suunnitellut: Boolean,
        lakkautetut: Boolean,
    ): TypedResult<GetOrganisaatiohierarkiaResponse, OrganisaatiopalveluException> {
        val json = ClassPathResource("./opintopolku-mocks/organisaatio-service/api/hierarkia/hae/GET.json").file
        return TypedResult.Success(defaultObjectMapper.readValue(json, GetOrganisaatiohierarkiaResponse::class.java))
    }
}
