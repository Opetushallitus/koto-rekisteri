package fi.oph.kitu.health

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {
    @GetMapping("api/health")
    fun getHealth() = "OK"
}
