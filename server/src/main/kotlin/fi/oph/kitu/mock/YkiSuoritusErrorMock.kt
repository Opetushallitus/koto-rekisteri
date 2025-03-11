package fi.oph.kitu.mock

import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorEntity
import java.time.Instant

fun generateRandomYkiSuoritusErrorEntity(): YkiSuoritusErrorEntity =
    YkiSuoritusErrorEntity(
        id = null,
        message = "An error was occurred!",
        context = "1,2,3,4,5,6,7,8,9,10",
        stackTrace = "\tat somewhere\n\tat somewhere",
        created = Instant.now(),
        exceptionMessage = "An error was occurred!",
    )
