package fi.oph.kitu

import org.springframework.web.client.RestClient
import org.springframework.web.client.body

fun RestClient.ResponseSpec.csvBody(columnSeparator: String = ","): List<List<String>>? {
    val response = this.body<String>()
    if (response.isNullOrBlank()) {
        return null
    }

    val lines = response.lines()
    val csv = mutableListOf<List<String>>()

    for (line in lines) {
        val column = line.split(columnSeparator)
        csv.add(column)
    }

    return csv
}
