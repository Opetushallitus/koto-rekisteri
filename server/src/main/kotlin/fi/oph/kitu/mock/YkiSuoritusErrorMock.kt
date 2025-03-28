package fi.oph.kitu.mock

import fi.oph.kitu.yki.suoritukset.YkiSuoritusCsv
import fi.oph.kitu.yki.suoritukset.YkiSuoritusMappingService
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorEntity
import java.time.Instant
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

fun generateRandomYkiSuoritusErrorEntity(): YkiSuoritusErrorEntity {
    val lastModified = getRandomInstant(Instant.parse("2004-01-01T00:00:00Z"))
    val virheenLuontiaika = getRandomInstant(lastModified)
    val virheellinenKentta = YkiSuoritusCsv::class.memberProperties.random().name

    val suoritusEntity = generateRandomYkiSuoritusEntity()

    val csv = YkiSuoritusMappingService().convertToResponse(suoritusEntity).toCsvString()

    return YkiSuoritusErrorEntity(
        id = null,
        suorittajanOid = suoritusEntity.suorittajanOID,
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
    this::class
        .memberProperties
        .onEach { it.isAccessible = true }
        .joinToString(separator = ",") {
            @Suppress("UNCHECKED_CAST")
            (it as KProperty1<YkiSuoritusCsv, *>).get(this).toString()
        }
