package fi.oph.kitu.yki.suoritukset

object YkiSuoritusSql {
    fun buildSql(vararg parts: String?) = parts.filterNotNull().joinToString("\n").trimIndent()

    fun selectSuorituksetFull(
        viimeisin: Boolean,
        vararg conditions: String?,
    ) = buildSql(
        withCtes(
            "suoritus" to selectRootSuoritukset(viimeisin),
            "arvosana" to selectArvosanat(),
            "tarkistusarviointi_agg" to selectTarkistusarviointiAgg(),
        ),
        selectFullYkiSuoritusEntity(
            ykiSuoritusTable = "suoritus",
            arvosanaTable = "arvosana",
            tarkistusarvointiAggregationTable = "tarkistusarviointi_agg",
        ),
        *conditions,
    )

    /**
     * Hakee suorituksista vain päätason tiedot (ei esim. sisältyviä osakokeita)
     */
    fun selectSuorituksetRoot(
        viimeisin: Boolean,
        vararg conditions: String?,
    ) = buildSql(
        selectRootSuoritukset(viimeisin),
        *conditions,
    )

    fun selectRootSuoritukset(viimeisin: Boolean = true) =
        """
        ${selectQuery(viimeisin)}
        FROM yki_suoritus
        ORDER BY
            solki_id,
            last_modified DESC,
            yki_suoritus.id DESC
        """.trimIndent()

    fun selectArvosanat(ykiSuoritusTable: String = "yki_suoritus") =
        """
        SELECT
            yki_suoritus.id as suoritus_id,
            max(arviointipaiva) AS arviointipaiva,
            max(arvosana) FILTER (WHERE tyyppi = 'PU') AS puhuminen,
            max(arvosana) FILTER (WHERE tyyppi = 'KI') AS kirjoittaminen,
            max(arvosana) FILTER (WHERE tyyppi = 'TY') AS tekstin_ymmartaminen,
            max(arvosana) FILTER (WHERE tyyppi = 'PY') AS puheen_ymmartaminen,
            max(arvosana) FILTER (WHERE tyyppi = 'RS') AS rakenteet_ja_sanasto,
            max(arvosana) FILTER (WHERE tyyppi = 'YL') AS yleisarvosana
        FROM
            $ykiSuoritusTable AS yki_suoritus
            JOIN yki_osakoe ON yki_suoritus.id = yki_osakoe.suoritus_id
        GROUP BY
            yki_suoritus.id
        """.trimIndent()

    fun selectTarkistusarviointiAgg(ykiSuoritusTable: String = "yki_suoritus") =
        """
        SELECT
            yki_suoritus.id AS suoritus_id,
            yki_osakoe_tarkistusarviointi.tarkistusarviointi_id,
            array_agg(yki_osakoe.tyyppi) AS tarkistusarvioidut_osakokeet,
            array_agg(yki_osakoe.tyyppi) FILTER (WHERE arvosana_muuttui) AS arvosana_muuttui
        FROM
            $ykiSuoritusTable AS yki_suoritus
            LEFT JOIN yki_osakoe ON yki_osakoe.suoritus_id = yki_suoritus.id
            LEFT JOIN yki_osakoe_tarkistusarviointi ON yki_osakoe.id = yki_osakoe_tarkistusarviointi.osakoe_id
        WHERE
            tarkistusarviointi_id IS NOT NULL
        GROUP BY
            yki_suoritus.id,
            yki_osakoe_tarkistusarviointi.tarkistusarviointi_id
        """.trimIndent()

    fun selectFullYkiSuoritusEntity(
        ykiSuoritusTable: String,
        arvosanaTable: String,
        tarkistusarvointiAggregationTable: String,
    ) = """
        SELECT
                yki_suoritus.*,
                arvosana.*,
                yki_suoritus_lisatieto.arviointitila_lahetetty,
                yki_suoritus_lisatieto.arviointitilan_lahetysvirhe,
                tarkistusarviointi_agg.tarkistusarvioidut_osakokeet,
                tarkistusarviointi_agg.arvosana_muuttui,
                yki_tarkistusarviointi.asiatunnus as tarkistusarvioinnin_asiatunnus,
                yki_tarkistusarviointi.saapumispaiva as tarkistusarvioinnin_saapumis_pvm,
                yki_tarkistusarviointi.kasittelypaiva as tarkistusarvioinnin_kasittely_pvm,
                yki_suoritus_lisatieto.tarkistusarviointi_hyvaksytty_pvm as tarkistusarviointi_hyvaksytty_pvm,
                yki_tarkistusarviointi.perustelu
            FROM
                $ykiSuoritusTable AS yki_suoritus
                LEFT JOIN $arvosanaTable AS arvosana ON arvosana.suoritus_id = yki_suoritus.id
                LEFT JOIN $tarkistusarvointiAggregationTable AS tarkistusarviointi_agg ON tarkistusarviointi_agg.suoritus_id = yki_suoritus.id
                LEFT JOIN yki_tarkistusarviointi ON yki_tarkistusarviointi.id = tarkistusarviointi_agg.tarkistusarviointi_id
                LEFT JOIN yki_suoritus_lisatieto ON yki_suoritus.solki_id = yki_suoritus_lisatieto.solki_id
        """.trimIndent()

    fun selectQuery(
        distinct: Boolean,
        columns: String = "*",
    ): String = if (distinct) "SELECT DISTINCT ON (yki_suoritus.solki_id) $columns" else "SELECT $columns"

    fun pagingQuery(
        limit: Int?,
        offset: Int?,
    ): String = if (limit != null && offset != null) "LIMIT :limit OFFSET :offset" else ""

    fun withCtes(vararg ctes: Pair<String, String>) =
        """
        WITH ${ctes.joinToString(",\n") { "${it.first} AS (${it.second})" }}
        """.trimIndent()
}
