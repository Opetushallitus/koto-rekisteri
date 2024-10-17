package fi.oph.kitu.yki

import org.springframework.stereotype.Repository

@Repository
class YkiRepository {
    fun insertSuoritukset(suoritukset: List<YkiSuoritus>): List<YkiSuoritus> = throw NotImplementedError()

    fun insertSuoritus(suoritus: YkiSuoritus): YkiSuoritus = throw NotImplementedError()
}
