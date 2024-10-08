package fi.oph.kitu.oppija

import fi.oph.kitu.generated.api.OppijaControllerApi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class OppijaController : OppijaControllerApi {
    @Autowired
    private lateinit var oppijaService: OppijaService

    override fun getOppijat(): ResponseEntity<List<Oppija>> =
        ResponseEntity(oppijaService.getAll().toList(), HttpStatus.OK)

    override fun addOppija(
        @RequestBody oppija: Oppija,
    ): ResponseEntity<Oppija> {
        val inserted = oppijaService.insert(oppija)
        return ResponseEntity(
            inserted,
            if (inserted != null) HttpStatus.CREATED else HttpStatus.BAD_REQUEST,
        )
    }
}
