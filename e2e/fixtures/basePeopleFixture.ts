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
  fanni: createPerson({
    hetu: "010100A9846",
    sukupuoli: "N",
    etunimet: "Fanni Testi",
    sukunimi: "Vesala-Testi",
    email: "devnull-3@oph.fi",
    oppijanumero: "1.2.246.562.24.74064782358",
    osoite: {
      // FIXME:
      katuosoite: "",
      postinumero: "",
      postitoimipaikka: "",
    },
  }),
  eino: createPerson({
    hetu: "010100A999N",
    sukupuoli: "M",
    etunimet: "Eino Testi",
    sukunimi: "Välimaa-Testi",
    oppijanumero: "1.2.246.562.24.67409348034",
    email: "devnull-10@oph.fi",
    osoite: {
      // FIXME:
      katuosoite: "",
      postinumero: "",
      postitoimipaikka: "",
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
  pernilla: createPerson({
    hetu: "010100A9868",
    sukupuoli: "N",
    etunimet: "Pernilla Testi",
    sukunimi: "Pulkkinen-Testi",
    email: "devnull-4@oph.fi",
    oppijanumero: "1.2.246.562.24.83611868632",
    osoite: {
      // FIXME
      katuosoite: "",
      postitoimipaikka: "",
      postinumero: "",
    },
  }),
  kalervo: createPerson({
    hetu: "010100A995J",
    sukupuoli: "M",
    etunimet: "Kalervo Testi",
    sukunimi: "Paasonen-Testi",
    email: "devnull-5@oph.fi",
    oppijanumero: "1.2.246.562.24.61390710528",
    osoite: {
      // FIXME
      katuosoite: "",
      postinumero: "",
      postitoimipaikka: "",
    },
  }),
  toni: createPerson({
    hetu: "010100A9857",
    sukupuoli: "M",
    etunimet: "Toni Testi",
    sukunimi: "Laasonen-Testi",
    email: "devnull-6@oph.fi",
    oppijanumero: "1.2.246.562.24.16014275446",
    osoite: {
      // FIXME
      katuosoite: "",
      postinumero: "",
      postitoimipaikka: "",
    },
  }),
  amalia: createPerson({
    hetu: "010100A990C",
    sukupuoli: "M",
    etunimet: "Amalia Testi",
    sukunimi: "Andersson-Testi",
    email: "devnull-7@oph.fi",
    oppijanumero: "1.2.246.562.24.64655158835",
    osoite: {
      // FIXME
      katuosoite: "",
      postinumero: "",
      postitoimipaikka: "",
    },
  }),
  topi: createPerson({
    hetu: "010100A991D",
    sukupuoli: "M",
    etunimet: "Topi Testi",
    sukunimi: "Takkinen-Testi",
    email: "devnull-8@oph.fi",
    oppijanumero: "1.2.246.562.24.19650463538",
    osoite: {
      // FIXME
      katuosoite: "",
      postinumero: "",
      postitoimipaikka: "",
    },
  }),
  tobias: createPerson({
    hetu: "010100A993F",
    sukupuoli: "M",
    etunimet: "Tobias Testi",
    sukunimi: "Saarela-Testi",
    email: "devnull-9@oph.fi",
    oppijanumero: "1.2.246.562.24.67738436206",
    osoite: {
      // FIXME
      katuosoite: "",
      postinumero: "",
      postitoimipaikka: "",
    },
  }),
  silja: createPerson({
    hetu: "010180-918P",
    sukupuoli: "N",
    etunimet: "Silja Testi",
    sukunimi: "Haverinen-Testi",
    email: "devnull-11@oph.fi",
    oppijanumero: "1.2.246.562.24.27639310186",
    osoite: {
      // FIXME
      katuosoite: "",
      postinumero: "",
      postitoimipaikka: "",
    },
  }),
  anniina: createPerson({
    hetu: "010180-922U",
    sukupuoli: "M",
    etunimet: "Anniina Testi",
    sukunimi: "Torvinen-Testi",
    email: "devnull-12@oph.fi",
    oppijanumero: "1.2.246.562.24.24941612410",
    osoite: {
      // FIXME
      katuosoite: "",
      postinumero: "",
      postitoimipaikka: "",
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
