package fi.oph.kitu.dev.mockdata

import fi.oph.kitu.yki.Tutkintotaso
import fi.oph.kitu.yki.suoritukset.YkiSuoritusPoikkeama
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.random.Random

private data class PoikkeamaTemplate(
    val kentta: String,
    val arvoSolkissa: String,
    val arvoKitussa: String,
)

private val poikkeamaTemplates =
    listOf(
        PoikkeamaTemplate("sukunimi", "Mäkinen", "Makinen"),
        PoikkeamaTemplate("sukunimi", "Virtanen-Korhonen", "Virtanen Korhonen"),
        PoikkeamaTemplate("etunimet", "Anna Maria", "Anna-Maria"),
        PoikkeamaTemplate("etunimet", "Jukka Tapio", "Jukka"),
        PoikkeamaTemplate("katuosoite", "Mannerheimintie 1 A 5", "Mannerheimintie 1A5"),
        PoikkeamaTemplate("katuosoite", "Kauppakatu 12", "Kauppakatu 12 B"),
        PoikkeamaTemplate("postinumero", "00100", "00120"),
        PoikkeamaTemplate("postitoimipaikka", "Helsinki", "HELSINKI"),
        PoikkeamaTemplate("postitoimipaikka", "Tampere", "Tampre"),
        PoikkeamaTemplate("email", "matti.meikalainen@example.fi", "matti.meikalainen@example.com"),
        PoikkeamaTemplate("jarjestajanNimi", "Helsingin yliopisto", "Helsingin Yliopisto"),
        PoikkeamaTemplate("tekstinYmmartaminen", "3", "4"),
        PoikkeamaTemplate("kirjoittaminen", "2", "3"),
        PoikkeamaTemplate("puheenYmmartaminen", "3", "2"),
        PoikkeamaTemplate("puhuminen", "5", "4"),
        PoikkeamaTemplate("perustelu", "Erinomainen", "Hyvä"),
        PoikkeamaTemplate("tarkistusarvioinninAsiatunnus", "OPH-1234-2024", "OPH-1234/2024"),
        PoikkeamaTemplate("tarkistusarvioinninSaapumisPvm", "2024-03-15", "2024-03-14"),
        PoikkeamaTemplate("tarkistusarvioinninKasittelyPvm", "2024-04-01", "2024-04-02"),
    )

fun generateRandomYkiSuoritusPoikkeama(
    solkiId: Int,
    random: Random = Random,
): YkiSuoritusPoikkeama {
    val template = poikkeamaTemplates.random(random)
    val havaittu = Instant.now().minus(random.nextLong(0, 30), ChronoUnit.DAYS)
    return YkiSuoritusPoikkeama(
        solkiId = solkiId,
        kentta = template.kentta,
        arvoKitussa = template.arvoKitussa,
        arvoSolkissa = template.arvoSolkissa,
        havaittu = havaittu,
    )
}

private val sukunimet = listOf("Mäkinen", "Virtanen", "Korhonen", "Nieminen", "Mäkelä", "Hämäläinen")
private val etunimet = listOf("Anna", "Matti", "Liisa", "Jukka", "Sofia", "Mikael", "Aino", "Eero")

fun generateRandomMissingYkiSuoritusPoikkeama(
    solkiId: Int,
    random: Random = Random,
): YkiSuoritusPoikkeama {
    val sukunimi = sukunimet.random(random)
    val etunimi = etunimet.random(random)
    val taso = Tutkintotaso.entries.random(random)
    val tutkintopaiva = LocalDate.now().minusDays(random.nextLong(0, 365))
    val havaittu = Instant.now().minus(random.nextLong(0, 30), ChronoUnit.DAYS)
    return YkiSuoritusPoikkeama(
        solkiId = solkiId,
        kentta = YkiSuoritusPoikkeama.SUORITUS_PUUTTUU_KITUSTA,
        arvoKitussa = "",
        arvoSolkissa = "$sukunimi $etunimi, $taso, $tutkintopaiva",
        havaittu = havaittu,
    )
}
