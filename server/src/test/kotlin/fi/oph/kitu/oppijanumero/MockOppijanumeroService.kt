package fi.oph.kitu.oppijanumero

import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult
import fi.oph.kitu.defaultObjectMapper
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.io.FileNotFoundException

@Service
@Profile("test")
class MockOppijanumeroService : OppijanumeroService {
    override fun getOppijanumero(oppija: Oppija): TypedResult<Oid, OppijanumeroException> {
        require(oppija.etunimet.isNotEmpty()) { "etunimet cannot be empty" }
        require(oppija.hetu.isNotEmpty()) { "hetu cannot be empty" }
        require(oppija.sukunimi.isNotEmpty()) { "sukunimi cannot be empty" }
        require(oppija.kutsumanimi.isNotEmpty()) { "kutsumanimi cannot be empty" }

        val request =
            YleistunnisteHaeRequest(
                etunimet = oppija.etunimet,
                hetu = oppija.hetu,
                kutsumanimi = oppija.kutsumanimi,
                sukunimi = oppija.sukunimi,
            )

        return when (oppija.hetu) {
            "INVALID_HETU" ->
                TypedResult.Failure(
                    OppijanumeroException.BadRequest(request, response = ResponseEntity.badRequest().build()),
                )
            "WRONG_HETU" ->
                TypedResult.Failure(OppijanumeroException.OppijaNotIdentifiedException(request))
            else ->
                hetuToOid[oppija.hetu]?.let { oid ->
                    TypedResult.Success(Oid.parse(oid).getOrThrow())
                } ?: TypedResult.Failure(
                    OppijanumeroException.OppijaNotFoundException(request),
                )
        }
    }

    override fun getHenkilo(oid: Oid): TypedResult<OppijanumerorekisteriHenkilo, OppijanumeroException> =
        try {
            val source = ClassPathResource("./opintopolku-mocks/oppijanumerorekisteri-service/henkilo/$oid.json").file
            TypedResult.Success(defaultObjectMapper.readValue(source, OppijanumerorekisteriHenkilo::class.java))
        } catch (_: FileNotFoundException) {
            TypedResult.Failure(OppijanumeroException.OppijaNotFoundException(EmptyRequest()))
        }

    companion object {
        val hetuToOid =
            mapOf(
                "010180-9026" to "1.2.246.562.24.33342764709",
            )
    }
}
