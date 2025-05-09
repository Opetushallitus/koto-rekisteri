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

    val arvioijaEntity = generateRandomYkiArviointiEntity()

    val csv = YkiArvioijaMappingService().convertToResponse(arvioijaEntity).toCsvString()

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
    this.arvioijanOppijanumero + "," +
        this.henkilotunnus + "," +
        this.sukunimi + "," +
        this.etunimet + "," +
        this.sahkopostiosoite + "," +
        this.katuosoite + "," +
        this.postinumero + "," +
        this.postitoimipaikka + "," +
        this.ensimmainenRekisterointipaiva + "," +
        this.kaudenAlkupaiva + "," +
        this.kaudenPaattymispaiva + "," +
        this.jatkorekisterointi + "," +
        this.tila + "," +
        this.kieli + "," +
        this.tasot
