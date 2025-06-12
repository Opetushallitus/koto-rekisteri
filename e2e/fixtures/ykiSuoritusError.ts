import SQL from "sql-template-strings"
import { TestDB } from "./baseFixture"
import { FixturePerson, OidQuoted, peopleFixture } from "./basePeopleFixture"

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

type CreateErrorArgs = Partial<
  Omit<YkiSuoritusError, "suorittajanOid" | "hetu" | "nimi">
>

const createError = (
  person: FixturePerson,
  {
    lastModified,
    virheellinenKentta,
    virheellinenArvo,
    virheellinenRivi,
    virheenRivinumero,
    virheenLuontiaika,
  }: CreateErrorArgs,
): YkiSuoritusError => {
  const p = peopleFixture[person]
  return {
    suorittajanOid: `"${p.oppijanumero}"`,
    hetu: p.hetu,
    nimi: `"${p.sukunimi}" "${p.etunimet}"`,
    lastModified,
    virheellinenKentta,
    virheellinenArvo,
    virheellinenRivi,
    virheenRivinumero,
    virheenLuontiaika,
  }
}

export const fixtureData = {
  missingOid: {
    suorittajanOid: undefined,
    hetu: `"${peopleFixture.ranja.hetu}"`,
    nimi: `"${peopleFixture.ranja.sukunimi}" "${peopleFixture.ranja.etunimet}"`,
    lastModified: "2024-10-30 13:53:56",
    virheellinenKentta: undefined,
    virheellinenArvo: undefined,
    virheellinenRivi:
      ',"010180-9026","N","Öhman-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",183424,2024-10-30T13:53:56Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,5,5,,5,5,,,,0,0,,',
    virheenRivinumero: 2,
    virheenLuontiaika: "2025-03-27 07:29:53",
  },
  invalidGender: createError("petro", {
    lastModified: "2024-10-30 13:55:09",
    virheellinenKentta: "sukupuoli",
    virheellinenArvo: "CORRUPTED",
    virheellinenRivi:
      '"1.2.246.562.24.59267607404","010116A9518","CORRUPTED","Kivinen-Testi","Petro Testi","","Testikuja 10","40100","Testinsuu","testi.petro@testi.fi",183425,2024-10-30T13:55:09Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-10-30,6,6,,6,6,,,,0,0,,',
    virheenRivinumero: 3,
    virheenLuontiaika: "2025-03-27 07:29:53",
  }),
  weirdError: createError("magdalena", {
    lastModified: "2042-12-22T22:42:42Z",
    virheellinenKentta: "yksi niistä",
    virheellinenArvo: "en kerro, arvaa!",
    virheellinenRivi:
      '"1.2.246.562.24.33342764709","010866-9260","N","Sallinen-Testi","Magdalena-Testi","FIN","Testaamo 10","Testikylä","devnull-14@oph.fi",721388,2023-11-30T12:34:56Z,2024-10-01,"fin","PT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-10-30,6,6,,6,6,,,,0,0,,',
    virheenRivinumero: 5,
    virheenLuontiaika: "2023-11-30T12:34:56Z",
  }),
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
