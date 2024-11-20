package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import fi.oph.kitu.yki.YkiSuoritusEntity
import java.io.StringWriter

fun Iterable<YkiSuoritusEntity>.toCsvString(): String {
    val mapper = CsvMapper()
    val schema =
        CsvSchema
            .builder()
            .addColumn("id")
            .addColumn("suorittajanOppijanumero")
            .addColumn("hetu")
            .addColumn("sukupuoli")
            .addColumn("sukunimi")
            .addColumn("etunimet")
            .addColumn("kansalaisuus")
            .addColumn("katuosoite")
            .addColumn("postinumero")
            .addColumn("postitoimipaikka")
            .addColumn("email")
            .addColumn("tutkintopaiva")
            .addColumn("tutkintokieli")
            .addColumn("tutkintotaso")
            .addColumn("jarjestajanTunnusOid")
            .addColumn("jarjestajanNimi")
            .addColumn("arviointipaiva")
            .addColumn("tekstinYmmartaminen")
            .addColumn("kirjoittaminen")
            .addColumn("rakenteetJaSanasto")
            .addColumn("puheenYmmartaminen")
            .addColumn("puhuminen")
            .addColumn("yleisarvosana")
            .build()
            .withHeader()

    val writer = StringWriter()
    mapper
        .writerFor(Iterable::class.java)
        .with(schema)
        .writeValue(writer, this)

    val str = writer.toString()
    return str
}
