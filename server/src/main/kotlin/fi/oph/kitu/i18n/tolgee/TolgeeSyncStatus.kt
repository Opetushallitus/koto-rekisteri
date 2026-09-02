package fi.oph.kitu.i18n.tolgee

sealed interface TolgeeSyncResult {
    data class Ok(
        val lisatty: Int,
        val poistettu: Int,
    ) : TolgeeSyncResult

    data class PoistorajaYlittyi(
        val poistettavia: Int,
        val raja: Int,
    ) : TolgeeSyncResult

    data object RekisteriTyhja : TolgeeSyncResult

    data class Virhe(
        val syy: String,
    ) : TolgeeSyncResult
}

/**
 * Viimeisimmän käynnistyksenaikaisen synkronoinnin tulos etusivun varoitusta varten.
 * Ei kantaa eikä tilaa yli uudelleenkäynnistysten — synkronointi ajetaan joka bootissa.
 */
object TolgeeSyncStatus {
    @Volatile
    var last: TolgeeSyncResult? = null
}
