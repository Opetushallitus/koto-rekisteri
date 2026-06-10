package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

/**
 * Heitetään kun Koealustan lataus- tai listausvastaus ei ole odotettua XML:ää
 * (esim. Moodle palauttaa HTTP 200:lla JSON-virheen). Estää virheellisen sisällön
 * tallentumisen S3:een.
 */
class TehtavapankkiDownloadException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Heitetään ajon lopuksi kun yksikin kurssi epäonnistui latauksessa tai ingestissä,
 * jotta ajastettu tehtävä menee FAILED-tilaan vaikka terveet kurssit käsiteltiin.
 */
class TehtavapankkiImportException(
    message: String,
) : RuntimeException(message)
