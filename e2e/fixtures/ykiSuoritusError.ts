import SQL from "sql-template-strings"
import { TestDB } from "./baseFixture"
import { OidQuoted } from "./kotoSuoritus"

export interface YkiSuoritusError {
  suorittajanOid: OidQuoted | undefined
  hetu: string | undefined
  nimi: string | undefined
  lastModified: string | undefined
  virheellinenKentta: string | undefined
  virheellinenArvo: string | undefined
  virheellinenRivi: string
  virheenRivinumero: number
  virheenLuontiaika: string
}

export const fixtureData = {
  missingOid: {
    suorittajanOid: undefined,
    hetu: '"010180-9026"',
    nimi: '"Öhman-Testi" "Ranja Testi"',
    lastModified: "2024-10-30 13:53:56",
    virheellinenKentta: undefined,
    virheellinenArvo: undefined,
    virheellinenRivi:
      ',"010180-9026","N","Öhman-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",183424,2024-10-30T13:53:56Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,5,5,,5,5,,,,0,0,,',
    virheenRivinumero: 2,
    virheenLuontiaika: "2025-03-27 07:29:53",
  } as YkiSuoritusError,
  invalidGender: {
    suorittajanOid: '"1.2.246.562.24.59267607404"',
    hetu: '"010116A9518"',
    nimi: '"Kivinen-Testi" "Petro Testi"',
    lastModified: "2024-10-30 13:55:09",
    virheellinenKentta: "sukupuoli",
    virheellinenArvo: "CORRUPTED",
    virheellinenRivi:
      '"1.2.246.562.24.59267607404","010116A9518","CORRUPTED","Kivinen-Testi","Petro Testi","","Testikuja 10","40100","Testinsuu","testi.petro@testi.fi",183425,2024-10-30T13:55:09Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-10-30,6,6,,6,6,,,,0,0,,',
    virheenRivinumero: 3,
    virheenLuontiaika: "2025-03-27 07:29:53",
  } as YkiSuoritusError,
}

const insertQuery = (error: YkiSuoritusError) => SQL`
  INSERT INTO yki_suoritus_error(
    suorittajan_oid,
    hetu,
    nimi,
    last_modified,
    virheellinen_kentta,
    virheellinen_arvo,
    virheellinen_rivi,
    virheen_rivinumero,
    virheen_luontiaika
  ) VALUES (${error.suorittajanOid},
            ${error.hetu},
            ${error.nimi},
            ${error.lastModified},
            ${error.virheellinenKentta},
            ${error.virheellinenArvo},
            ${error.virheellinenRivi},
            ${error.virheenRivinumero},
            ${error.virheenLuontiaika})
`

export type YkiSuorittajaErrorName = keyof typeof fixtureData

export const insert = async (db: TestDB, error: YkiSuorittajaErrorName) =>
  await db.dbClient.query(insertQuery(fixtureData[error]))
