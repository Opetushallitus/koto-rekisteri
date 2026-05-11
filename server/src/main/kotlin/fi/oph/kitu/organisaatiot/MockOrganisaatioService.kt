package fi.oph.kitu.organisaatiot

import arrow.core.Either
import arrow.core.right
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.util.cache.PersistentCache
import fi.oph.kitu.util.defaultObjectMapper
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

@Service
@Profile("test || e2e || local-opintopolku")
class MockOrganisaatioService(
    val cache: PersistentCache,
) : OrganisaatioService {
    override fun getOrganisaatioCache(): PersistentCache = cache

    override fun getOrganisaatio(oid: Oid): Either<OrganisaatiopalveluException, GetOrganisaatioResponse> {
        val json = ClassPathResource("./opintopolku-mocks/organisaatio-service/api/GET-$oid.json").file
        return defaultObjectMapper.readValue(json, GetOrganisaatioResponse::class.java).right()
    }

    override fun getOrganisaatiohierarkia(
        aktiiviset: Boolean,
        suunnitellut: Boolean,
        lakkautetut: Boolean,
    ): Either<OrganisaatiopalveluException, GetOrganisaatiohierarkiaResponse> {
        val json = ClassPathResource("./opintopolku-mocks/organisaatio-service/api/hierarkia/hae/GET.json").file
        return defaultObjectMapper.readValue(json, GetOrganisaatiohierarkiaResponse::class.java).right()
    }
}
