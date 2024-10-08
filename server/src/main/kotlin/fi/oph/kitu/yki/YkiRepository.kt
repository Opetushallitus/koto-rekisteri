package fi.oph.kitu.yki

import fi.oph.kitu.generated.model.YkiSuoritus
import org.springframework.stereotype.Repository

@Repository
class YkiRepository {
    fun insertSuoritus(suoritus: YkiSuoritus): Unit = throw NotImplementedError()
}
