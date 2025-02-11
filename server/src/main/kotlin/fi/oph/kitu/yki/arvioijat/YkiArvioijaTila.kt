package fi.oph.kitu.yki.arvioijat

import java.sql.ResultSet

enum class YkiArvioijaTila {
    AKTIIVINEN,
    PASSIVOITU,
}

fun ResultSet.getYkiArvioijaTila(columnLabel: String): YkiArvioijaTila = YkiArvioijaTila.valueOf(getString(columnLabel))
