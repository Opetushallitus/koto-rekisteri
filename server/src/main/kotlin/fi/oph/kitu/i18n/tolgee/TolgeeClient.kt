package fi.oph.kitu.i18n.tolgee

/**
 * Kirjoittava rajapinta Tolgee Cloudiin. Käännösten *luku* tapahtuu OPH:n lokalisointipalvelun
 * proxyn kautta ([fi.oph.kitu.i18n.LokalisointiClient]); tämä rajapinta vain pitää Tolgeen
 * avainluettelon samana kuin koodin [fi.oph.kitu.i18n.UiTextRegistry].
 */
interface TolgeeClient {
    fun fetchKeys(): Map<String, Long>

    fun createKeys(entries: Map<String, String>)

    fun deleteKeys(ids: List<Long>)
}
