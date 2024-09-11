package fi.oph.kitu.oppija

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class OppijaController {
    @Autowired
    private lateinit var oppijaService: OppijaService

    @GetMapping("/api/oppija")
    fun getOppijat(): Iterable<Oppija> = oppijaService.getAll()

    @PostMapping("/api/oppija")
    fun addOppija(
        @RequestBody name: String,
    ) = oppijaService.insert(name)
}
