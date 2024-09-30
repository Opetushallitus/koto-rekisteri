package fi.oph.kitu.dev

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/dev/mock/yki")
class MockYkiControllerApi {
    @GetMapping
    fun mockYki() =
        YkiMockData(
            "Yrjö Ykittäjä",
            "010106A911C",
            "Yhdistynyt Kuningaskunta",
            "Muu",
            "Taitotalo, Helsinki",
            "suomi",
            "EVK B2/YKI 4",
            "2024-09-27 12:40",
            "Yrjönkatu 13 C, 00120 Helsinki",
        )
}

data class YkiMockData(
    val nimi: String,
    val henkilötunnus: String,
    val kansalaisuus: String,
    val sukupuoli: String,
    val tutkinnonSuorittamispaikka: String,
    val tutkintokieli: String,
    val saadutTaitotasoarviot: String,
    val tutkintokertojenAjankohta: String,
    val tarpeellisetYhteystiedot: String,
)
