package fi.oph.kitu.yki.arvioijat

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import fi.oph.kitu.auditlogs.AuditLogOperation
import fi.oph.kitu.auditlogs.AuditLogger
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.oppijanumero.OppijanumeroException
import fi.oph.kitu.oppijanumero.OppijanumeroHakuService
import fi.oph.kitu.oppijanumero.OppijanumeroService
import fi.oph.kitu.util.validation.Validation.ValidationError
import fi.oph.kitu.util.validation.ValidationService
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime

@Service
class YkiArvioijaService(
    private val repository: YkiArvioijaRepository,
    private val validationService: ValidationService,
    private val oppijanumeroHaku: OppijanumeroHakuService,
    private val oppijanumeroService: OppijanumeroService,
    private val auditLogger: AuditLogger,
) {
    @WithSpan
    fun haeSivullinen(params: YkiArvioijaParams): List<YkiArvioijaListRow> =
        repository.findForListView(params).also { rows ->
            auditLogger.logAllInternalOnly("Yki arvioija viewed", rows) {
                arrayOf("arvioija.oid" to it.arvioijaOid.toString())
            }
        }

    @WithSpan
    fun laske(params: YkiArvioijaParams): Int = repository.countForListView(params)

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
    fun haeHenkilotiedot(haku: OnrHaku): Either<YkiArvioijaError, ArvioijanEsitaytto> =
        haeOid(haku).flatMap { oid -> haeEsitaytto(oid) }

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
     * Lomake kantaa vain virkailijan syottamat kentat. Merkinnan elinkaaritiedot — passivointihetki,
     * yksilointitila ja kielikohtainen tila — kuuluvat muille kulkureiteille, joten ne poimitaan
     * olemassa olevalta rivilta eivatka nollaudu tallennuksessa.
     */
    private fun entiteetti(
        validoitu: TallennaArvioija,
        olemassaoleva: YkiArvioijaEntity?,
    ): YkiArvioijaEntity =
        validoitu
            .toEntity(
                ensimmainenRekisterointipaiva(olemassaoleva, validoitu),
                olemassaoleva?.arviointioikeudet?.associate { it.kieli to it.tila }.orEmpty(),
            ).copy(
                passivoitu = olemassaoleva?.passivoitu,
                yksilointiKesken = olemassaoleva?.yksilointiKesken == true,
            )

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

    private fun haeOid(haku: OnrHaku): Either<YkiArvioijaError, Oid> =
        when (haku.tapa) {
            ArvioijaHakutapa.OPPIJANUMERO -> haeOidOppijanumerolla(haku)
            ArvioijaHakutapa.HETU -> haeOidHetulla(haku)
        }

    private fun haeOidOppijanumerolla(haku: OnrHaku): Either<YkiArvioijaError, Oid> {
        val oppijanumero =
            haku.oppijanumero?.trim()?.takeIf { it.isNotEmpty() }
                ?: return virhe("oppijanumero", "Oppijanumero on pakollinen tieto").left()

        return Oid.parse(oppijanumero).mapLeft {
            virhe("oppijanumero", "Oppijanumero on virheellinen")
        }
    }

    private fun haeOidHetulla(haku: OnrHaku): Either<YkiArvioijaError, Oid> {
        puuttuvatHakukentat(haku)?.let { return YkiArvioijaError.Validointivirheet(it).left() }

        val oppija =
            oppijanumeroHaku.oppijaOf(
                hetu = haku.hetu!!,
                etunimet = haku.etunimet!!,
                sukunimi = haku.sukunimi!!,
                kutsumanimi = haku.kutsumanimi,
            )

        return oppijanumeroHaku.haeMasterOid(oppija).mapLeft { onrVirhe ->
            when (onrVirhe) {
                is OppijanumeroException.OppijaNotIdentifiedException -> YkiArvioijaError.OppijaaEiYksiloity(null)
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

    private fun puuttuvatHakukentat(haku: OnrHaku) =
        buildList {
            if (haku.hetu.isNullOrBlank()) add(ValidationError(listOf("hetu"), "Henkilötunnus on pakollinen tieto"))
            if (haku.etunimet.isNullOrBlank()) add(ValidationError(listOf("etunimet"), "Etunimet on pakollinen tieto"))
            if (haku.sukunimi.isNullOrBlank()) add(ValidationError(listOf("sukunimi"), "Sukunimi on pakollinen tieto"))
        }.toNonEmptyListOrNull()

    private fun virhe(
        kentta: String,
        viesti: String,
    ) = YkiArvioijaError.Validointivirheet(nonEmptyListOf(ValidationError(listOf(kentta), viesti)))
}
