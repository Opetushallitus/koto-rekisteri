package fi.oph.kitu

import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet

fun ResultSet.getNullableDouble(columnLabel: String): Double? =
    if (this.getObject(columnLabel) != null) this.getDouble(columnLabel) else null

fun ResultSet.getNullableInt(columnLabel: String): Int? =
    if (this.getObject(columnLabel) != null) this.getInt(columnLabel) else null

fun ResultSet.getNullableBoolean(columnLabel: String): Boolean? =
    if (this.getObject(columnLabel) != null) this.getBoolean(columnLabel) else null

fun PreparedStatement.setNullableDate(
    parameterIndex: Int,
    date: java.util.Date?,
) {
    if (date != null) {
        this.setDate(parameterIndex, Date(date.time))
    } else {
        this.setObject(parameterIndex, null)
    }
}
