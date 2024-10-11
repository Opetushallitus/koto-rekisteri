package fi.oph.kitu.yki

import fi.oph.kitu.generated.model.YkiSuoritus
import org.springframework.stereotype.Repository

@Repository
class YkiRepository {
    fun insertSuoritukset(suoritukset: List<YkiSuoritus>) {
        for (suoritus in suoritukset) {
            insertSuoritus(suoritus)
        }
    }

    fun insertSuoritus(suoritus: YkiSuoritus): Unit = throw NotImplementedError()
}
