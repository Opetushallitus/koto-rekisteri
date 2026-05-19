package fi.oph.kitu.vkt

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.tiedontuontischema.Henkilosuoritus
import fi.oph.kitu.tiedontuontischema.VktHenkilosuoritus

fun mergeVktHenkilosuoritukset(
    henkilosuoritukset: List<VktHenkilosuoritus>,
    suorituksenVastaanottajat: Map<Oid, String>,
): Either<VktMergeError, VktHenkilosuoritus> =
    either {
        val suoritukset = henkilosuoritukset.map { it.suoritus }

        val oppijanumerot = henkilosuoritukset.map { it.henkilo.oid }.distinct()
        ensure(oppijanumerot.size <= 1) { VktMergeError.UseaOppija(oppijanumerot) }

        val kielet = suoritukset.map { it.kieli }.distinct()
        ensure(kielet.size <= 1) { VktMergeError.UseaTutkintokieli(kielet) }

        val taitotasot = suoritukset.map { it.taitotaso }.distinct()
        ensure(taitotasot.size <= 1) { VktMergeError.UseaTaitotaso(taitotasot) }

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

        Henkilosuoritus(
            henkilo = viimeisin.henkilo,
            suoritus =
                viimeisin.suoritus.copy(
                    osat = kaikkiOsakokeet,
                    koskiSiirtoKasitelty = suoritukset.all { it.koskiSiirtoKasitelty },
                    koskiOpiskeluoikeusOid = suoritukset.firstNotNullOfOrNull { it.koskiOpiskeluoikeusOid },
                ),
        )
    }
