package fi.oph.kitu.oppijanumero

import org.springframework.stereotype.Service

@Service
class OppijanumeroTroubleshootingService(
    private val oppijanumeroService: OppijanumeroService,
) {
    fun troubleshootOppijaNameCombinations(oppija: Oppija): Oppija? =
        tryEachEtunimiAsKutsumanimi(oppija) ?: switchEtunimetAndSukunimi(oppija)

    fun tryEachEtunimiAsKutsumanimi(oppija: Oppija): Oppija? =
        oppija.etunimet
            .split(" ")
            .map { etunimi -> oppija.copy(kutsumanimi = etunimi) }
            .firstOrNull { oppija -> oppijanumeroService.getOppijanumero(oppija).isSuccess }

    fun switchEtunimetAndSukunimi(oppija: Oppija): Oppija? =
        tryEachEtunimiAsKutsumanimi(oppija.copy(etunimet = oppija.sukunimi, sukunimi = oppija.etunimet))
}
