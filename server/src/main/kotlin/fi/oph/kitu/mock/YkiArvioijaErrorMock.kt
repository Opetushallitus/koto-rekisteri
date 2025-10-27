package fi.oph.kitu.mock

import fi.oph.kitu.yki.arvioijat.SolkiArvioijaResponse
import fi.oph.kitu.yki.arvioijat.YkiArvioijaMappingService
import fi.oph.kitu.yki.arvioijat.error.YkiArvioijaErrorEntity
import java.time.Instant
import kotlin.reflect.full.memberProperties

fun generateRandomYkiArvioijaErrorEntity(): YkiArvioijaErrorEntity {
    val lastModified = getRandomInstant(Instant.parse("2004-01-01T00:00:00Z"))
    val virheenLuontiaika = getRandomInstant(lastModified)
    val virheellinenKentta = SolkiArvioijaResponse::class.memberProperties.random().name

    val arvioijaEntity = generateRandomYkiArvioijaEntity()

    val csv =
        YkiArvioijaMappingService()
            .convertToResponses(arvioijaEntity)
            .first()
            .toCsvString()

    return YkiArvioijaErrorEntity(
        id = null,
        arvioijanOid = arvioijaEntity.arvioijanOppijanumero.toString(),
        hetu = arvioijaEntity.henkilotunnus,
        nimi = "${arvioijaEntity.sukunimi} ${arvioijaEntity.etunimet}",
        virheellinenKentta = virheellinenKentta,
        virheellinenArvo = "virheellinen_arvo",
        virheellinenRivi = csv,
        virheenRivinumero = (0..1000).random(),
        virheenLuontiaika = virheenLuontiaika,
    )
}

fun SolkiArvioijaResponse.toCsvString(): String =
    listOf(
        arvioijanOppijanumero,
        henkilotunnus,
        sukunimi,
        etunimet,
        sahkopostiosoite,
        katuosoite,
        postinumero,
        postitoimipaikka,
        ensimmainenRekisterointipaiva,
        kaudenAlkupaiva,
        kaudenPaattymispaiva,
        jatkorekisterointi,
        tila,
        kieli,
        tasot,
    ).joinToString(",") { it.toString() }
