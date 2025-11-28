package fi.oph.kitu.oppijanumero

import org.springframework.stereotype.Service

@Service
class OppijanumeroTroubleshootingService(
    val oppijanumeroService: OppijanumeroService,
) {
    fun tryEachEtunimiAsKutsumanimi(oppija: Oppija): Oppija? =
        oppija.etunimet
            .split(" ")
            .map { etunimi -> oppija.copy(kutsumanimi = etunimi) }
            .firstOrNull { oppija ->
                oppijanumeroService.getOppijanumero(oppija).getOrNull() != null
            }
}
