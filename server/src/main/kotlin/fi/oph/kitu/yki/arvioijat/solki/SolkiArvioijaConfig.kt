package fi.oph.kitu.yki.arvioijat.solki

import fi.oph.kitu.oppijanumero.OppijanumeroService
import fi.oph.kitu.util.TimeService
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Toteutus valitaan kytkimesta, ei beanien olemassaolosta. Aiemmin valinta nojasi
 * `@ConditionalOnBean`/`@ConditionalOnMissingBean`-pariin komponenttiskannatuilla luokilla, jolloin
 * tulos riippui skannausjarjestyksesta: pahimmillaan mock olisi voittanut ymparistossa jossa
 * lahetyksen kuuluu olla paalla, tai kumpaakaan beania ei olisi syntynyt eika arvioijarekisteri
 * olisi kaynnistynyt lainkaan ([fi.oph.kitu.yki.arvioijat.YkiArvioijaService] riippuu tasta.)
 *
 * Molemmat beanit ovat samassa konfiguraatiossa, joten ne kasitellaan esittelyjarjestyksessa ja
 * varatoteutus saadaan aina jos kytkin on pois.
 */
@Configuration
class SolkiArvioijaConfig {
    @Bean
    @ConditionalOnProperty("kitu.yki.arvioijarekisteri.integraatio.enabled", havingValue = "true")
    fun solkiArvioijaLahetys(
        repository: YkiArvioijaRepository,
        client: SolkiArvioijaClient,
        timeService: TimeService,
        oppijanumeroService: OppijanumeroService,
    ): SolkiArvioijaService = SolkiArvioijaServiceImpl(repository, client, timeService, oppijanumeroService)

    @Bean
    @ConditionalOnMissingBean(SolkiArvioijaService::class)
    fun solkiArvioijaLahetysPoisKaytosta(): SolkiArvioijaService = SolkiArvioijaServiceMock()
}
