package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.Oid
import fi.oph.kitu.yki.Sukupuoli
import org.springframework.stereotype.Service

@Service
class YkiSuoritusMappingService {
    fun convertToEntityIterable(iterable: Iterable<YkiSuoritusCsv>) = iterable.map { convertToEntity(it) }

    fun convertToEntity(
        csv: YkiSuoritusCsv,
        id: Int? = null,
        koskiOpiskeluoikeus: String? = null,
    ) = YkiSuoritusEntity(
        id,
        csv.suorittajanOID.toString(),
        csv.hetu,
        csv.sukupuoli ?: Sukupuoli.E,
        csv.sukunimi,
        csv.etunimet,
        csv.kansalaisuus,
        csv.katuosoite,
        csv.postinumero,
        csv.postitoimipaikka,
        csv.email,
        csv.suoritusID,
        csv.lastModified,
        csv.tutkintopaiva,
        csv.tutkintokieli,
        csv.tutkintotaso,
        csv.jarjestajanOID.toString(),
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
        csv.tarkistusarvioidutOsakokeet,
        csv.arvosanaMuuttui,
        csv.perustelu,
        csv.tarkistusarvioinninKasittelyPvm,
        Oid.parse(koskiOpiskeluoikeus).getOrNull(),
    )

    fun convertToResponseIterable(iterable: Iterable<YkiSuoritusEntity>) = iterable.map { convertToResponse(it) }

    fun convertToResponse(entity: YkiSuoritusEntity): YkiSuoritusCsv =
        YkiSuoritusCsv(
            suorittajanOID = Oid.parse(entity.suorittajanOID).getOrThrow(),
            hetu = entity.hetu,
            sukupuoli = entity.sukupuoli,
            sukunimi = entity.sukunimi,
            etunimet = entity.etunimet,
            kansalaisuus = entity.kansalaisuus,
            katuosoite = entity.katuosoite,
            postinumero = entity.postinumero,
            postitoimipaikka = entity.postitoimipaikka,
            email = entity.email,
            suoritusID = entity.suoritusId,
            lastModified = entity.lastModified,
            tutkintopaiva = entity.tutkintopaiva,
            tutkintokieli = entity.tutkintokieli,
            tutkintotaso = entity.tutkintotaso,
            jarjestajanOID = Oid.parse(entity.jarjestajanTunnusOid).getOrThrow(),
            jarjestajanNimi = entity.jarjestajanNimi,
            arviointipaiva = entity.arviointipaiva,
            tekstinYmmartaminen = entity.tekstinYmmartaminen,
            kirjoittaminen = entity.kirjoittaminen,
            rakenteetJaSanasto = entity.rakenteetJaSanasto,
            puheenYmmartaminen = entity.puheenYmmartaminen,
            puhuminen = entity.puhuminen,
            yleisarvosana = entity.yleisarvosana,
            tarkistusarvioinninSaapumisPvm = entity.tarkistusarvioinninSaapumisPvm,
            tarkistusarvioinninAsiatunnus = entity.tarkistusarvioinninAsiatunnus,
            tarkistusarvioidutOsakokeet = entity.tarkistusarvioidutOsakokeet,
            arvosanaMuuttui = entity.arvosanaMuuttui,
            perustelu = entity.perustelu,
            tarkistusarvioinninKasittelyPvm = entity.tarkistusarvioinninKasittelyPvm,
        )
}
