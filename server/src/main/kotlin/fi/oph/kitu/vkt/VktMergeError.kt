package fi.oph.kitu.vkt

import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.oid.Oid

sealed interface VktMergeError {
    val message: String

    data class UseaOppija(
        val oppijanumerot: List<Oid>,
    ) : VktMergeError {
        override val message: String
            get() = "Vain yhden oppijan suorituksia voi yhdistää (annettiin: ${oppijanumerot.joinToString(", ")}"
    }

    data class UseaTutkintokieli(
        val kielet: List<Koodisto.Tutkintokieli>,
    ) : VktMergeError {
        override val message: String
            get() = "Vain yhden tutkintokielen suorituksia voi yhdistää (annettiin: ${kielet.joinToString(", ")}"
    }

    data class UseaTaitotaso(
        val taitotasot: List<Koodisto.VktTaitotaso>,
    ) : VktMergeError {
        override val message: String
            get() = "Vain yhden taitotason suorituksia voi yhdistää (annettiin: ${taitotasot.joinToString(", ")}"
    }
}
