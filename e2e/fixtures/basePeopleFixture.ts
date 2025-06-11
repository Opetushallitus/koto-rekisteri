export type Oid = `${number}.${number}.${number}.${number}.${number}.${number}`
export type Email = `${string}@${string}.${string}`
export type Sukupuoli = "N" | "M"

export type Adress = {
  katuosoite: string
  postinumero: string
  postitoimipaikka: string
}

export type Person = {
  hetu: string
  sukupuoli: Sukupuoli
  etunimet: string
  kutsumanimi: string
  sukunimi: string
  email: Email
  oppijanumero: Oid
  osoite: Adress
}

type CreatePersonArgs = Omit<Person, "kutsumanimi"> & { kutsumanimi?: string }

const createPerson = ({
  hetu,
  sukupuoli,
  etunimet,
  kutsumanimi,
  sukunimi,
  email,
  oppijanumero,
  osoite,
}: CreatePersonArgs): Person => ({
  hetu,
  sukupuoli,
  etunimet,
  kutsumanimi: kutsumanimi ?? etunimet.split(" ")[0],
  sukunimi,
  email,
  oppijanumero,
  osoite,
})

export type FixturePerson = keyof typeof peopleFixture
export const peopleFixture = {
  ranja: createPerson({
    hetu: "010180-9026",
    sukupuoli: "N",
    etunimet: "Ranja Testi",
    sukunimi: "Öhman-Testi",
    email: "testi@testi.fi",
    oppijanumero: "1.2.246.562.24.20281155246",
    osoite: {
      katuosoite: "Testikuja 5",
      postinumero: "40100",
      postitoimipaikka: "Testilä",
    },
  }),
  petro: createPerson({
    hetu: "010116A9518",
    sukupuoli: "M",
    etunimet: "Petro Testi",
    sukunimi: "Kivinen-Testi",
    email: "testi.petro@testi.fi",
    oppijanumero: "1.2.246.562.24.59267607404",
    osoite: {
      katuosoite: "Testikuja 10",
      postinumero: "40200",
      postitoimipaikka: "Testinsuu",
    },
  }),
  magdalena: createPerson({
    hetu: "010866-9260",
    sukupuoli: "N",
    etunimet: "Magdalena Testi",
    sukunimi: "Sallinen-Testi",
    email: "devnull-14@oph.fi",
    oppijanumero: "1.2.246.562.24.33342764709",
    osoite: {
      katuosoite: "Testikoto 10",
      postinumero: "40300",
      postitoimipaikka: "Koestamo",
    },
  }),
} as const
