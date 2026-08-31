package fi.oph.kitu.yki.arvioijat

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import fi.oph.kitu.auditlogs.AuditLogOperation
import fi.oph.kitu.auditlogs.AuditLogger
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.oppijanumero.OppijanumeroException
import fi.oph.kitu.oppijanumero.OppijanumeroService
import fi.oph.kitu.util.TimeService
import fi.oph.kitu.util.validation.Validation.ValidationError
import fi.oph.kitu.util.validation.ValidationService
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class YkiArvioijaService(
    private val repository: YkiArvioijaRepository,
    private val validationService: ValidationService,
    private val oppijanumeroService: OppijanumeroService,
    private val auditLogger: AuditLogger,
    private val timeService: TimeService,
) {
    @WithSpan
    fun haeSivullinen(params: YkiArvioijaParams): List<YkiArvioijaListRow> =
        repository.findForListView(params, timeService.today()).also { rows ->
            auditLogger.logAllInternalOnly("Yki arvioija viewed", rows) {
                arrayOf("arvioija.oid" to it.arvioijaOid.toString())
            }
        }

    @WithSpan
    fun laske(params: YkiArvioijaParams): Int = repository.countForListView(params, timeService.today())

    /** CSV-vientiin: koko suodatettu joukko ilman sivutusta. */
    @WithSpan
    fun haeKaikki(params: YkiArvioijaParams): List<YkiArvioijaListRow> =
        haeSivullinen(params.copy(page = 1, limit = Int.MAX_VALUE))

    @WithSpan
    fun haeArvioija(id: Int): YkiArvioijaEntity? =
        repository.findArvioijaById(id)?.also {
            auditLogger.log(AuditLogOperation.YkiArvioijaViewed, it.arvioijaOid)
        }

    /** ONR-kyselyn epaonnistuminen on eri asia kuin "ei turvakieltoa", ks. [Turvakieltotieto]. */
    @WithSpan
    fun haeTurvakielto(oid: Oid): Turvakieltotieto =
        oppijanumeroService.getHenkiloByMasterOid(oid).fold(
            ifLeft = { Turvakieltotieto.EI_TIEDOSSA },
            ifRight = { if (it.turvakielto == true) Turvakieltotieto.ON else Turvakieltotieto.EI },
        )

    @WithSpan
    fun haeHenkilotiedot(oppijanumero: String?): Either<YkiArvioijaError, ArvioijanEsitaytto> =
        haeOid(oppijanumero).flatMap { oid -> haeEsitaytto(oid) }

    @WithSpan
    fun luoArvioija(
        komento: TallennaArvioija,
        tekija: Oid?,
        odotettuMuokkaushetki: OffsetDateTime? = null,
    ): Either<YkiArvioijaError, YkiArvioijaEntity> =
        validationService
            .validateAndEnrich(komento)
            .mapLeft { YkiArvioijaError.Validointivirheet(it) }
            .flatMap { validoitu ->
                // Lisayslomakkeelle voi paatya myos jo rekisterissa oleva arvioija (jatkokausi),
                // jolloin tallennus paivittaa merkintaa eika aloita sita alusta.
                val olemassaoleva = repository.findByArvioijaOid(validoitu.arvioijaOid)
                tallennaTaiKonflikti(
                    entiteetti(validoitu, olemassaoleva),
                    tekija,
                    odotettuMuokkaushetki,
                ).flatMap { id ->
                    auditLogger.log(
                        if (olemassaoleva == null) {
                            AuditLogOperation.YkiArvioijaCreated
                        } else {
                            AuditLogOperation.YkiArvioijaUpdated
                        },
                        validoitu.arvioijaOid,
                    )
                    repository.findArvioijaById(id)?.right() ?: YkiArvioijaError.ArvioijaaEiLoydy.left()
                }
            }

    @WithSpan
    fun haeKausihistoria(id: Int): List<YkiArvioijaKausiEntity> = repository.findKausihistoria(id)

    /**
     * Passivointi ei kulje lomakkeen kautta, joten se rakentaa entiteetin suoraan olemassa olevasta
     * rivista. Tila lasketaan kaudesta, joten passivointi paattaa kauden tahan paivaan — mutta vain
     * kiristaen: nappi tarjotaan myos luonnollisesti paattyneelle merkinnalle, ja ilman kiristysta
     * klikkaus pidentaisi vanhentuneen arviointioikeuden tahan paivaan asti ja kirjaisi
     * kausihistoriaan hallintopaatoksen jota ei tehty. Sailytysajan laskenta alkaa
     * passivointihetkesta, joten aiemmin passivoidun leimaa ei siirreta eteenpain.
     */
    @WithSpan
    fun passivoiArvioija(
        id: Int,
        tekija: Oid?,
    ): Either<YkiArvioijaError, YkiArvioijaEntity> {
        val olemassaoleva = repository.findArvioijaById(id) ?: return YkiArvioijaError.ArvioijaaEiLoydy.left()
        val tanaan = timeService.today()

        val passivoitu =
            olemassaoleva.copy(
                passivoitu = olemassaoleva.passivoitu ?: timeService.now().atOffset(ZoneOffset.UTC),
                arviointioikeudet =
                    olemassaoleva.arviointioikeudet.map { oikeus ->
                        oikeus.copy(
                            tila = null,
                            kaudenAlkupaiva = oikeus.kaudenAlkupaiva?.let { minOf(it, tanaan) },
                            kaudenPaattymispaiva = minOf(oikeus.kaudenPaattymispaiva ?: tanaan, tanaan),
                        )
                    },
            )

        repository.tallenna(passivoitu, tekija)
        auditLogger.log(AuditLogOperation.YkiArvioijaPassivated, olemassaoleva.arvioijaOid)

        return repository.findArvioijaById(id)?.right() ?: YkiArvioijaError.ArvioijaaEiLoydy.left()
    }

    @WithSpan
    fun onOlemassa(id: Int): Boolean = repository.findArvioijaById(id) != null

    @WithSpan
    fun paivitaArvioija(
        id: Int,
        komento: TallennaArvioija,
        tekija: Oid?,
        odotettuMuokkaushetki: OffsetDateTime? = null,
    ): Either<YkiArvioijaError, YkiArvioijaEntity> {
        val olemassaoleva = repository.findArvioijaById(id) ?: return YkiArvioijaError.ArvioijaaEiLoydy.left()

        // Polun id ratkaisee kenen tietoja muokataan, ei lomakkeen piilokentta.
        val kohdistettu = komento.copy(arvioijaOid = olemassaoleva.arvioijaOid)

        return validationService
            .validateAndEnrich(kohdistettu)
            .mapLeft { YkiArvioijaError.Validointivirheet(it) }
            .flatMap { validoitu ->
                tallennaTaiKonflikti(
                    entiteetti(validoitu, olemassaoleva),
                    tekija,
                    odotettuMuokkaushetki,
                ).flatMap {
                    auditLogger.log(AuditLogOperation.YkiArvioijaUpdated, validoitu.arvioijaOid)
                    repository.findArvioijaById(id)?.right() ?: YkiArvioijaError.ArvioijaaEiLoydy.left()
                }
            }
    }

    /**
     * Lomake kantaa vain virkailijan syottamat kentat; tila lasketaan kaudesta. Sailytysajan
     * alkuhetki paatellaan tallennettavista kausista eika lomakkeen kaudesta, koska
     * [paataMuuttumatonPassivoitu] voi viela kiristaa kautta: muuten passiiviseksi jaava merkinta
     * menettaisi alkuhetkensa.
     */
    private fun entiteetti(
        validoitu: TallennaArvioija,
        olemassaoleva: YkiArvioijaEntity?,
    ): YkiArvioijaEntity {
        val tanaan = timeService.today()
        val tallennettava = validoitu.toEntity(ensimmainenRekisterointipaiva(olemassaoleva, validoitu))
        val arviointioikeudet =
            tallennettava.arviointioikeudet.map { oikeus ->
                paataMuuttumatonPassivoitu(oikeus, olemassaoleva, tanaan)
            }
        val merkintaJaaPassiiviseksi =
            arviointioikeudet.all { Rekisterointitila.laske(it, tanaan) == Rekisterointitila.PASSIVOITU }

        return tallennettava.copy(
            passivoitu = olemassaoleva?.passivoitu?.takeIf { merkintaJaaPassiiviseksi },
            arviointioikeudet = arviointioikeudet,
        )
    }

    /**
     * Tallennettu PASSIVOITU on ainoa merkitseva tallennettu tila, ja tallennus nollaa sen. Jos
     * kausi sailyy ennallaan — esimerkiksi yhteystietoa korjatessa — kannanotto kaannetaan uuteen
     * esitystapaan paattamalla kausi, jottei korjaus elvyta passivoitua merkintaa. Uusi kausi sen
     * sijaan on uusi rekisterointi ja saa aktivoida merkinnan.
     */
    private fun paataMuuttumatonPassivoitu(
        oikeus: YkiArviointioikeusEntity,
        olemassaoleva: YkiArvioijaEntity?,
        tanaan: LocalDate,
    ): YkiArviointioikeusEntity {
        val aiempi =
            olemassaoleva
                ?.arviointioikeudet
                ?.firstOrNull { it.kieli == oikeus.kieli }
                ?.takeIf { it.tila == YkiArvioijaTila.PASSIVOITU }
                ?: return oikeus

        val kausiEnnallaan =
            aiempi.kaudenAlkupaiva == oikeus.kaudenAlkupaiva &&
                aiempi.kaudenPaattymispaiva == oikeus.kaudenPaattymispaiva

        return if (kausiEnnallaan) {
            oikeus.copy(kaudenPaattymispaiva = minOf(oikeus.kaudenPaattymispaiva ?: tanaan, tanaan.minusDays(1)))
        } else {
            oikeus
        }
    }

    private fun tallennaTaiKonflikti(
        arvioija: YkiArvioijaEntity,
        tekija: Oid?,
        odotettuMuokkaushetki: OffsetDateTime?,
    ): Either<YkiArvioijaError, Int> =
        try {
            repository.tallenna(arvioija, tekija, odotettuMuokkaushetki = odotettuMuokkaushetki).right()
        } catch (_: OptimisticLockingFailureException) {
            YkiArvioijaError.MuokattuSamanaikaisesti.left()
        }

    private fun ensimmainenRekisterointipaiva(
        olemassaoleva: YkiArvioijaEntity?,
        komento: TallennaArvioija,
    ): LocalDate =
        olemassaoleva
            ?.arviointioikeudet
            ?.minOfOrNull { it.ensimmainenRekisterointipaiva }
            ?: komento.kaudenAlkupaiva

    private fun haeOid(oppijanumero: String?): Either<YkiArvioijaError, Oid> {
        val syote =
            oppijanumero?.trim()?.takeIf { it.isNotEmpty() }
                ?: return virhe("oppijanumero", "Oppijanumero on pakollinen tieto").left()

        val syotetty =
            Oid.parse(syote).getOrNull()
                ?: return virhe("oppijanumero", "Oppijanumero on virheellinen").left()

        // Virkailija voi syottaa duplikaatin OIDin. Rekisteri avaimennetaan master-OIDilla, joten
        // ilman ratkaisua sama henkilo saisi toisen merkinnan eika olemassa olevaa loydettaisi.
        // getOppijanumero ei putoa takaisin henkilo-OIDiin, joten yksiloimaton henkilo hylataan.
        return oppijanumeroService.getOppijanumero(syotetty).mapLeft { onrVirhe ->
            when (onrVirhe) {
                is OppijanumeroException.OppijaNotIdentifiedException -> YkiArvioijaError.OppijaaEiYksiloity(syotetty)
                else -> YkiArvioijaError.OppijanumeroaEiSaatu(onrVirhe)
            }
        }
    }

    private fun haeEsitaytto(oid: Oid): Either<YkiArvioijaError, ArvioijanEsitaytto> =
        oppijanumeroService
            .getHenkiloByMasterOid(oid)
            .mapLeft { YkiArvioijaError.OppijanumeroaEiSaatu(it) }
            .map { henkilo ->
                val yhteystiedot =
                    henkilo.yhteystiedotRyhma
                        .orEmpty()
                        .flatMap { it.yhteystieto.orEmpty() }

                fun arvo(tyyppi: String): String? =
                    yhteystiedot
                        .firstOrNull { it.yhteystietoTyyppi == tyyppi && !it.yhteystietoArvo.isNullOrBlank() }
                        ?.yhteystietoArvo

                ArvioijanEsitaytto(
                    arvioijaOid = oid,
                    sukunimi = henkilo.sukunimi.orEmpty(),
                    etunimet = henkilo.etunimet.orEmpty(),
                    sahkopostiosoite = arvo("YHTEYSTIETO_SAHKOPOSTI"),
                    katuosoite = arvo("YHTEYSTIETO_KATUOSOITE"),
                    postinumero = arvo("YHTEYSTIETO_POSTINUMERO"),
                    postitoimipaikka = arvo("YHTEYSTIETO_KAUPUNKI"),
                    turvakielto = if (henkilo.turvakielto == true) Turvakieltotieto.ON else Turvakieltotieto.EI,
                    olemassaolevaMerkinta = repository.findByArvioijaOid(oid),
                )
            }

    private fun virhe(
        kentta: String,
        viesti: String,
    ) = YkiArvioijaError.Validointivirheet(nonEmptyListOf(ValidationError(listOf(kentta), viesti)))
}
