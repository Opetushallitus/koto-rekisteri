package fi.oph.kitu.kielitesti

import fi.oph.kitu.generated.api.UserControllerApi
import fi.oph.kitu.oppija.Oppija
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController : UserControllerApi {
    @Autowired
    private lateinit var moodleService: MoodleService

    override fun getUsers(): ResponseEntity<List<Oppija>> = ResponseEntity(moodleService.getUsers(), HttpStatus.OK)
}
