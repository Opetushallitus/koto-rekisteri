package fi.oph.kitu.csvparsing

import java.io.OutputStream

private val UTF_8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

// Excel on European locales (mm. fi-FI) defaults the field delimiter to ';' and otherwise
// collapses every row into a single cell when opening a comma-separated CSV. The `sep=`
// directive overrides the locale default for Excel regardless of OS settings, and the
// UTF-8 BOM makes Excel parse the bytes as UTF-8 so Finnish characters render correctly.
// Numbers, LibreOffice, and other modern spreadsheet tools recognize and skip the prelude.
fun OutputStream.writeExcelCsvPrelude(separator: Char = ',') {
    write(UTF_8_BOM)
    write("sep=$separator\r\n".toByteArray())
}
