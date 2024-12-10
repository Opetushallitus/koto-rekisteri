package fi.oph.kitu.yki.arvioijat

import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class YkiArvioijaMappingService {
    fun convertToEntityIterable(iterable: Iterable<SolkiArvioijaResponse>) = iterable.map { convertToEntity(it) }

    fun convertToEntity(
        response: SolkiArvioijaResponse,
        id: Number? = null,
        rekisteriintuontiaika: OffsetDateTime? = null,
    ) = YkiArvioijaEntity(
        id,
        rekisteriintuontiaika,
        response.arvioijanOppijanumero,
        response.henkilotunnus,
        response.sukunimi,
        response.etunimet,
        response.sahkopostiosoite,
        response.katuosoite,
        response.postinumero,
        response.postitoimipaikka,
        response.ensimmainenRekisterointipaiva,
        response.kaudenAlkupaiva,
        response.kaudenPaattymispaiva,
        response.jatkorekisterointi,
        response.tila,
        response.kieli,
        tasot = response.tasot.toSet(),
    )
}
