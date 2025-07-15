package fi.oph.kitu.mock

import fi.oph.kitu.yki.suoritukset.YkiSuoritusCsv
import fi.oph.kitu.yki.suoritukset.YkiSuoritusMappingService
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorEntity
import java.time.Instant
import kotlin.reflect.full.memberProperties

fun generateRandomYkiSuoritusErrorEntity(): YkiSuoritusErrorEntity {
    val lastModified = getRandomInstant(Instant.parse("2004-01-01T00:00:00Z"))
    val virheenLuontiaika = getRandomInstant(lastModified)
    val virheellinenKentta = YkiSuoritusCsv::class.memberProperties.random().name

    val suoritusEntity = generateRandomYkiSuoritusEntity()

    val csv = YkiSuoritusMappingService().convertToResponse(suoritusEntity).toCsvString()

    return YkiSuoritusErrorEntity(
        id = null,
        suorittajanOid = suoritusEntity.suorittajanOID.toString(),
        hetu = suoritusEntity.hetu,
        nimi = "${suoritusEntity.sukunimi} ${suoritusEntity.etunimet}",
        lastModified = lastModified,
        virheellinenKentta = virheellinenKentta,
        virheellinenArvo = "virheellinen_arvo",
        virheellinenRivi = csv,
        virheenRivinumero = (0..1000).random(),
        virheenLuontiaika = virheenLuontiaika,
    )
}

fun YkiSuoritusCsv.toCsvString(): String =
    this.suorittajanOID.toString() +
        "," +
        this.hetu +
        "," +
        this.sukupuoli +
        "," +
        this.sukunimi +
        "," +
        this.etunimet +
        "," +
        this.kansalaisuus +
        "," +
        this.katuosoite +
        "," +
        this.postinumero +
        "," +
        this.postitoimipaikka +
        "," +
        this.email +
        "," +
        this.suoritusID +
        "," +
        this.lastModified +
        "," +
        this.tutkintopaiva +
        "," +
        this.tutkintokieli +
        "," +
        this.tutkintotaso +
        "," +
        this.jarjestajanOID +
        "," +
        this.jarjestajanNimi +
        "," +
        this.arviointipaiva +
        "," +
        this.tekstinYmmartaminen +
        "," +
        this.kirjoittaminen +
        "," +
        this.rakenteetJaSanasto +
        "," +
        this.puheenYmmartaminen +
        "," +
        this.puhuminen +
        "," +
        this.yleisarvosana +
        "," +
        this.tarkistusarvioinninSaapumisPvm +
        "," +
        this.tarkistusarvioinninAsiatunnus +
        "," +
        this.tarkistusarvioidutOsakokeet +
        "," +
        this.arvosanaMuuttui +
        "," +
        this.perustelu +
        "," +
        this.tarkistusarvioinninKasittelyPvm
