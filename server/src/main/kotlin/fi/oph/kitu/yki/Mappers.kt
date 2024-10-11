package fi.oph.kitu.yki

import fi.oph.kitu.generated.model.YkiSuoritus

class Mappers {
    companion object {
        fun toTutkintokieli(value: String): YkiSuoritus.Tutkintokieli {
            when (value.uppercase()) {
                "" -> throw NotImplementedError()
                else -> throw NotImplementedError()
            }
        }

        fun toYkiSuoritus(column: List<String>): YkiSuoritus =
            YkiSuoritus(
                suorittajanOid = column[0],
                sukunimi = column[1],
                etunimet = column[2],
                tutkintopaiva = column[3],
                tutkintokieli = toTutkintokieli(column[4]),
                jarjestajanOid = column[5],
                jarjestajanNimi = column[6],
                tekstinYmmartamisenArvosana = column[7].toBigDecimal(),
                kirjoittamisenArvosana = column[8].toBigDecimal(),
                rakenteidenJaSanastonArvosana = column[9].toBigDecimal(),
                puheenYmmartamisenArvosana = column[10].toBigDecimal(),
                puhumisenArvosana = column[11].toBigDecimal(),
                yleisarvosana = column[12].toBigDecimal(),
            )
    }
}
