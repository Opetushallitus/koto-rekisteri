package fi.oph.kitu.csvparsing

import tools.jackson.databind.MapperFeature

annotation class MapperFeatures(
    vararg val features: MapperFeature,
)
