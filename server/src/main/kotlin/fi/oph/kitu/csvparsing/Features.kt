package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.databind.MapperFeature

annotation class Features(
    vararg val features: MapperFeature,
)
