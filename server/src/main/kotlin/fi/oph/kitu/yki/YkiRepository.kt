package fi.oph.kitu.yki

import org.springframework.stereotype.Repository

@Repository
class YkiRepository {
    fun insertSuoritukset(suoritukset: List<YkiSuoritus>): Void = throw NotImplementedError()

    fun insertSuoritus(suoritus: YkiSuoritus): Void = throw NotImplementedError()
}
