package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.Oid
import fi.oph.kitu.yki.Arviointitila
import fi.oph.kitu.yki.Sukupuoli
import fi.oph.kitu.yki.TutkinnonOsa.Companion.toTutkinnonOsaSet
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.stereotype.Service

@Service
class YkiSuoritusMappingService {
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
