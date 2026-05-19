package fi.oph.kitu.oppijanumero

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.util.defaultObjectMapper
import fi.oph.kitu.util.result.getOrThrow
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.io.FileNotFoundException

@Service
@Profile("test | e2e | local-opintopolku")
class MockOppijanumeroService : OppijanumeroService {
    override fun getOppijanumero(oppija: Oppija): Either<OppijanumeroException, Oid> {
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
            "INVALID_HETU" -> {
                OppijanumeroException.BadRequest(request, response = ResponseEntity.badRequest().build()).left()
            }

            "WRONG_HETU" -> {
                OppijanumeroException.OppijaNotIdentifiedException(request).left()
            }

            else -> {
                oppijaToOid[oppija]?.let { oid ->
                    Oid.Companion
                        .parse(oid)
                        .getOrThrow()
                        .right()
                } ?: OppijanumeroException.OppijaNotFoundException(request, ResponseEntity.notFound().build()).left()
            }
        }
    }

    override fun getHenkilo(oid: Oid): Either<OppijanumeroException, OppijanumerorekisteriHenkilo> =
        try {
            val source = ClassPathResource("./opintopolku-mocks/oppijanumerorekisteri-service/henkilo/$oid.json").file
            defaultObjectMapper.readValue(source, OppijanumerorekisteriHenkilo::class.java).right()
        } catch (_: FileNotFoundException) {
            OppijanumeroException.OppijaNotFoundException(EmptyRequest(), ResponseEntity.notFound().build()).left()
        }

    companion object {
        val oppijaToOid =
            mapOf(
                Oppija(
                    "Ranja Testi",
                    "010180-9026",
                    "Ranja",
                    "Öhman-Testi",
                ) to "1.2.246.562.24.33342764709",
                Oppija(
                    "Minerva Alli Aniitta",
                    "040265-9985",
                    "Aniitta",
                    "Marttila",
                ) to "1.2.246.562.24.92472049678",
            )
    }
}
