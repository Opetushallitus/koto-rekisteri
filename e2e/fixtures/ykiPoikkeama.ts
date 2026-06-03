import SQL from "sql-template-strings"
import { TestDB } from "./baseFixture"

export interface YkiPoikkeama {
  solkiId: number
  kentta: string
  arvoKitussa: string
  arvoSolkissa: string
  havaittu: string
  tutkintopaiva: string | null
  tutkintokieli: "FIN" | "SWE" | "ENG" | null
  tutkintotaso: "YT" | "PT" | "KT" | null
}

const make = (overrides: Partial<YkiPoikkeama> = {}): YkiPoikkeama => ({
  solkiId: 183424,
  kentta: "sukunimi",
  arvoKitussa: "Mäkitie",
  arvoSolkissa: "Mäkinen",
  havaittu: "2026-06-03T08:00:00Z",
  tutkintopaiva: "2024-09-01",
  tutkintokieli: "FIN",
  tutkintotaso: "YT",
  ...overrides,
})

export const fixtureData = {
  ranjaSukunimi: make({
    solkiId: 183424,
    kentta: "sukunimi",
    arvoKitussa: "Mäkitie",
    arvoSolkissa: "Öhman-Testi",
  }),
  ranjaEtunimet: make({
    solkiId: 183424,
    kentta: "etunimet",
    arvoKitussa: "Ranja",
    arvoSolkissa: "Ranja Testi",
  }),
  petroPostinumero: make({
    solkiId: 123123,
    kentta: "postinumero",
    arvoKitussa: "00100",
    arvoSolkissa: "00120",
    tutkintopaiva: "2024-08-25",
    tutkintokieli: "SWE",
    tutkintotaso: "YT",
  }),
  magdalenaTaso: make({
    solkiId: 172836,
    kentta: "tutkintotaso",
    arvoKitussa: "YT",
    arvoSolkissa: "PT",
    tutkintopaiva: "2025-01-12",
    tutkintokieli: "FIN",
    tutkintotaso: "PT",
  }),
  puuttuvaSuoritus: make({
    solkiId: 999999,
    kentta: "(suoritus puuttuu Kitusta)",
    arvoKitussa: "",
    arvoSolkissa: "Puuttuva Henkilö, YT, 2025-03-10",
    tutkintopaiva: "2025-03-10",
    tutkintokieli: "FIN",
    tutkintotaso: "YT",
  }),
} as const

export type YkiPoikkeamaName = keyof typeof fixtureData

export const insert = async (db: TestDB, name: YkiPoikkeamaName) => {
  const p = fixtureData[name]
  await db.dbClient.query(SQL`
    INSERT INTO yki_suoritus_poikkeama
      (solki_id, kentta, arvo_kitussa, arvo_solkissa, havaittu,
       tutkintopaiva, tutkintokieli, tutkintotaso)
    VALUES (
      ${p.solkiId},
      ${p.kentta},
      ${p.arvoKitussa},
      ${p.arvoSolkissa},
      ${p.havaittu},
      ${p.tutkintopaiva},
      ${p.tutkintokieli},
      ${p.tutkintotaso}
    )
  `)
}
