package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.oid.Oid
import fi.oph.kitu.yki.Arviointitila
import fi.oph.kitu.yki.Sukupuoli
import fi.oph.kitu.yki.TutkinnonOsa.Companion.toTutkinnonOsaSet
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class YkiSuoritusMappingService(
    val ykiSuoritusRepository: YkiSuoritusRepository?,
) {
    @WithSpan
    fun convertToEntityIterable(iterable: Iterable<YkiSuoritusCsv>) = iterable.map { convertToEntity(it) }

    private fun convertToEntity(
        csv: YkiSuoritusCsv,
        id: Int? = null,
        koskiOpiskeluoikeus: String? = null,
    ) = YkiSuoritusEntity(
        id,
        csv.suorittajanOID,
        csv.hetu,
        csv.sukupuoli ?: Sukupuoli.E,
        csv.sukunimi,
        csv.etunimet,
        csv.kansalaisuus,
        csv.katuosoite,
        csv.postinumero,
        csv.postitoimipaikka,
        maa = null,
        csv.email,
        csv.suoritusID,
        csv.lastModified,
        csv.tutkintopaiva,
        csv.tutkintokieli,
        csv.tutkintotaso,
        todistuskieli = null,
        csv.jarjestajanOID,
        csv.jarjestajanNimi,
        csv.arviointipaiva,
        csv.tekstinYmmartaminen,
        csv.kirjoittaminen,
        csv.rakenteetJaSanasto,
        csv.puheenYmmartaminen,
        csv.puhuminen,
        csv.yleisarvosana,
        csv.tarkistusarvioinninSaapumisPvm,
        csv.tarkistusarvioinninAsiatunnus,
        csv.tarkistusarvioidutOsakokeet?.toTutkinnonOsaSet(),
        csv.arvosanaMuuttui?.toTutkinnonOsaSet(),
        csv.perustelu,
        csv.tarkistusarvioinninKasittelyPvm,
        tarkistusarviointiHyvaksyttyPvm = null,
        Oid.parse(koskiOpiskeluoikeus).getOrNull(),
        false,
        arviointitila =
            if (csv.arviointipaiva == null) {
                Arviointitila.ARVIOITAVA
            } else if (csv.tarkistusarvioinninAsiatunnus == null || csv.tarkistusarvioinninAsiatunnus.isEmpty()) {
                Arviointitila.ARVIOITU
            } else if (csv.tarkistusarvioinninKasittelyPvm == null) {
                Arviointitila.TARKISTUSARVIOITAVA
            } else {
                if (csv.tarkistusarvioinninKasittelyPvm < LocalDate.of(2025, 11, 14) ||
                    ykiSuoritusRepository?.tarkistusarvointiHyvaksytty(csv.suoritusID) == true
                ) {
                    // Vanhat tarkistusarvioinnit on hyväksytty prosessilla, joka oli käytössä ennen kuin
                    // Kielitutkintorekisteriin rakennettiin sitä varten toiminnallisuus, joten ne hyväksytään
                    // rekisteriin automaattisesti.
                    Arviointitila.TARKISTUSARVIOINTI_HYVAKSYTTY
                } else {
                    Arviointitila.TARKISTUSARVIOITU
                }
            },
        arviointitilaLahetetty = null,
        arviointitilanLahetysvirhe = null,
    )

    @WithSpan
    fun convertToResponseIterable(iterable: Iterable<YkiSuoritusEntity>) = iterable.map { convertToResponse(it) }

    fun convertToResponse(entity: YkiSuoritusEntity): YkiSuoritusCsvResponse =
        YkiSuoritusCsvResponse(
            suorittajanOID = entity.suorittajanOID,
            hetu = entity.hetu,
            sukupuoli = entity.sukupuoli,
            sukunimi = entity.sukunimi,
            etunimet = entity.etunimet,
            kansalaisuus = entity.kansalaisuus,
            katuosoite = entity.katuosoite,
            postinumero = entity.postinumero,
            postitoimipaikka = entity.postitoimipaikka,
            maa = entity.maa,
            email = entity.email,
            solkiTunniste = entity.solkiId,
            lastModified = entity.lastModified,
            tutkintopaiva = entity.tutkintopaiva,
            tutkintokieli = entity.tutkintokieli,
            tutkintotaso = entity.tutkintotaso,
            todistuskieli = entity.todistuskieli,
            jarjestajanOID = entity.jarjestajanTunnusOid,
            jarjestajanNimi = entity.jarjestajanNimi,
            arviointitila = entity.arviointitila,
            tilaLahetetty = entity.arviointitilaLahetetty?.toInstant(),
            arviointipaiva = entity.arviointipaiva,
            tekstinYmmartaminen = entity.tekstinYmmartaminen,
            kirjoittaminen = entity.kirjoittaminen,
            rakenteetJaSanasto = entity.rakenteetJaSanasto,
            puheenYmmartaminen = entity.puheenYmmartaminen,
            puhuminen = entity.puhuminen,
            yleisarvosana = entity.yleisarvosana,
            tarkistusarvioinninSaapumisPvm = entity.tarkistusarvioinninSaapumisPvm,
            tarkistusarvioinninAsiatunnus = entity.tarkistusarvioinninAsiatunnus,
            tarkistusarvioidutOsakokeet = entity.tarkistusarvioidutOsakokeet?.joinToString(" "),
            arvosanaMuuttui = entity.arvosanaMuuttui?.joinToString(" "),
            perustelu = entity.perustelu,
            tarkistusarvioinninKasittelyPvm = entity.tarkistusarvioinninKasittelyPvm,
        )
}
