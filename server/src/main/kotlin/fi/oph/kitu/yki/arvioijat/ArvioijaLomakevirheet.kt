package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.html.FormErrors
import fi.oph.kitu.i18n.UiText
import fi.oph.kitu.oppijanumero.OppijanumeroException
import fi.oph.kitu.util.validation.Validation.ValidationError

internal fun lomakevirheet(error: YkiArvioijaError): FormErrors =
    when (error) {
        is YkiArvioijaError.Validointivirheet -> {
            FormErrors.of(error.virheet)
        }

        is YkiArvioijaError.OppijaaEiYksiloity -> {
            kentta(
                "oppijanumero",
                UiText.Yki.Arvioija.eiYksiloity
                    .toString(),
            )
        }

        is YkiArvioijaError.OppijanumeroaEiSaatu -> {
            when (error.syy) {
                is OppijanumeroException.OppijaNotFoundException -> {
                    yleinen(
                        UiText.Yki.Arvioija.eiLoytynytOnrista
                            .toString(),
                    )
                }

                else -> {
                    yleinen(
                        UiText.Yki.Arvioija.onrEiVastannut
                            .toString(),
                    )
                }
            }
        }

        YkiArvioijaError.KausiEiOleAktiivinen -> {
            yleinen(
                UiText.Yki.Arvioija.Kausi.eiAktiivinen
                    .toString(),
            )
        }

        YkiArvioijaError.ViimeistaKauttaEiVoiPoistaa -> {
            yleinen(
                UiText.Yki.Arvioija.Kausi.viimeistaEiVoiPoistaa
                    .toString(),
            )
        }

        YkiArvioijaError.ArvioijaaEiLoydy, YkiArvioijaError.KauttaEiLoydy -> {
            yleinen(
                UiText.Yki.Arvioija.eiLoydy
                    .toString(),
            )
        }

        YkiArvioijaError.MuokattuSamanaikaisesti -> {
            yleinen(
                UiText.Yki.Arvioija.muokattuSamanaikaisesti
                    .toString(),
            )
        }
    }

internal fun yleinen(viesti: String): FormErrors = FormErrors.of(listOf(ValidationError(emptyList(), viesti)))

private fun kentta(
    nimi: String,
    viesti: String,
): FormErrors = FormErrors.of(listOf(ValidationError(listOf(nimi), viesti)))
