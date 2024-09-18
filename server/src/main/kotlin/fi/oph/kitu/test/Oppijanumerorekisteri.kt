package fi.oph.kitu.test

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class Oppijanumerorekisteri {
    // @CrossOrigin(origins = [("http://localhost:3000")])
    @GetMapping("api/test/onr")
    fun testOnr(): String = "OK (ONR)"
}
