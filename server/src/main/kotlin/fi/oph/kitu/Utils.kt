package fi.oph.kitu

import org.springframework.web.client.RestClient

fun RestClient.ResponseSpec.csvBody(columnSeparator: String = ","): List<List<String>>? = TODO("todo") // csv
