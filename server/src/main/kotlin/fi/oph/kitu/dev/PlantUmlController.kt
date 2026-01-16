package fi.oph.kitu.dev

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/uml")
class PlantUmlController(
    val plantUmlService: PlantUmlService,
) {
    @GetMapping("/{pkg}", produces = ["text/plain"])
    fun getPlantUml(
        @PathVariable pkg: String,
    ) = plantUmlService.generatePlantUml(pkg)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
}
