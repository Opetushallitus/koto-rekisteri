package fi.oph.kitu.yki.arvioijat

import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class YkiArvioijaMappingService {
    @WithSpan
    fun convertToEntityIterable(iterable: Iterable<SolkiArvioijaResponse>) = iterable.map { convertToEntity(it) }

    fun convertToEntity(
        response: SolkiArvioijaResponse,
        id: Number? = null,
        rekisteriintuontiaika: OffsetDateTime? = null,
    ) = YkiArvioijaEntity(
        id,
        arvioijanOppijanumero = response.arvioijanOppijanumero,
        henkilotunnus = response.henkilotunnus,
        sukunimi = response.sukunimi,
        etunimet = response.etunimet,
        sahkopostiosoite = response.sahkopostiosoite,
        katuosoite = response.katuosoite,
        postinumero = response.postinumero,
        postitoimipaikka = response.postitoimipaikka,
        arviointioikeudet =
            listOf(
                YkiArviointioikeusEntity(
                    id = null,
                    arvioijaId = id,
                    tasot = response.tasot.toSet(),
                    kieli = response.kieli,
                    tila = if (response.tila == 0) YkiArvioijaTila.AKTIIVINEN else YkiArvioijaTila.PASSIVOITU,
                    kaudenAlkupaiva = response.kaudenAlkupaiva,
                    kaudenPaattymispaiva = response.kaudenPaattymispaiva,
                    jatkorekisterointi = response.jatkorekisterointi,
                    ensimmainenRekisterointipaiva = response.ensimmainenRekisterointipaiva,
                    rekisteriintuontiaika = rekisteriintuontiaika,
                ),
            ),
    )

    fun convertToResponses(entity: YkiArvioijaEntity) =
        entity.arviointioikeudet.map {
            SolkiArvioijaResponse(
                arvioijanOppijanumero = entity.arvioijanOppijanumero,
                henkilotunnus = entity.henkilotunnus,
                sukunimi = entity.sukunimi,
                etunimet = entity.etunimet,
                sahkopostiosoite = entity.sahkopostiosoite,
                katuosoite = entity.katuosoite,
                postinumero = entity.postinumero,
                postitoimipaikka = entity.postitoimipaikka,
                ensimmainenRekisterointipaiva = it.ensimmainenRekisterointipaiva,
                kaudenAlkupaiva = it.kaudenAlkupaiva,
                kaudenPaattymispaiva = it.kaudenPaattymispaiva,
                jatkorekisterointi = it.jatkorekisterointi,
                tila = it.tila.ordinal,
                kieli = it.kieli,
                tasot = it.tasot,
            )
        }
}
