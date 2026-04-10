package fi.oph.kitu.kotoutumiskoulutus.suoritukset

import fi.oph.kitu.SortDirection

data class KielitestiSuorituksetParams(
    var limit: Int = 100,
    var page: Int = 1,
    var sortColumn: KielitestiSuoritusColumn = KielitestiSuoritusColumn.Suoritusaika,
    var sortDirection: SortDirection = SortDirection.DESC,
    var search: String? = null,
)
