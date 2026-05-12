package fi.oph.kitu.vkt

import fi.oph.kitu.oid.Oid
import fi.oph.kitu.tiedonsiirtoschema.Henkilosuoritus
import fi.oph.kitu.tiedonsiirtoschema.VktHenkilosuoritus

fun mergeVktHenkilosuoritukset(
    henkilosuoritukset: List<VktHenkilosuoritus>,
    suorituksenVastaanottajat: Map<Oid, String>,
): VktHenkilosuoritus {
    val suoritukset = henkilosuoritukset.map { it.suoritus }

    val oppijanumerot = henkilosuoritukset.map { it.henkilo.oid }.distinct()
    if (oppijanumerot.size > 1) {
        throw IllegalArgumentException(
            "Vain yhden oppijan suorituksia voi yhdistää (annettiin: ${oppijanumerot.joinToString(", ")}",
        )
    }

    val kielet = suoritukset.map { it.kieli }.distinct()
    if (kielet.size > 1) {
        throw IllegalArgumentException(
            "Vain yhden tutkintokielen suorituksia voi yhdistää (annettiin: ${kielet.joinToString(", ")}",
        )
    }

    val taitotasot = suoritukset.map { it.taitotaso }.distinct()
    if (taitotasot.size > 1) {
        throw IllegalArgumentException(
            "Vain yhden taitotason suorituksia voi yhdistää (annettiin: ${taitotasot.joinToString(", ")}",
        )
    }

    val viimeisin = henkilosuoritukset.sortedBy { it.lisatty }.last()
    val kaikkiOsakokeet =
        suoritukset.flatMap { suoritus ->
            suoritus.osat.map { osa ->
                val vastaanottaja =
                    suorituksenVastaanottajat[suoritus.suorituksenVastaanottaja]
                when (osa) {
                    is VktKirjoittamisenKoe -> {
                        osa.copy(
                            suorituksenVastaanottaja = vastaanottaja,
                            suorituspaikkakunta = suoritus.suorituspaikkakunta,
                        )
                    }

                    is VktTekstinYmmartamisenKoe -> {
                        osa.copy(
                            suorituksenVastaanottaja = vastaanottaja,
                            suorituspaikkakunta = suoritus.suorituspaikkakunta,
                        )
                    }

                    is VktPuhumisenKoe -> {
                        osa.copy(
                            suorituksenVastaanottaja = vastaanottaja,
                            suorituspaikkakunta = suoritus.suorituspaikkakunta,
                        )
                    }

                    is VktPuheenYmmartamisenKoe -> {
                        osa.copy(
                            suorituksenVastaanottaja = vastaanottaja,
                            suorituspaikkakunta = suoritus.suorituspaikkakunta,
                        )
                    }

                    else -> {
                        osa
                    }
                }
            }
        }

    return Henkilosuoritus(
        henkilo = viimeisin.henkilo,
        suoritus =
            viimeisin.suoritus.copy(
                osat = kaikkiOsakokeet,
                koskiSiirtoKasitelty = suoritukset.all { it.koskiSiirtoKasitelty },
                koskiOpiskeluoikeusOid = suoritukset.firstNotNullOfOrNull { it.koskiOpiskeluoikeusOid },
            ),
    )
}
