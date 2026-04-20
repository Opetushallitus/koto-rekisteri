package fi.oph.kitu.csvparsing

import tools.jackson.databind.MapperFeature

annotation class Features(
    vararg val features: MapperFeature,
)
