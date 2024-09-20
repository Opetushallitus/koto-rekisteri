export interface EnvironmentConfig {
  name: string
  network: {
    cidr: string
    maxAzs: number
  }
  domainName: string
  databaseName: string
}

export const deploymentAccounts = {
  dev: {
    name: "untuva",
    account: "682033502734",
    region: "eu-west-1",
    network: {
      cidr: "10.15.0.0/18",
      maxAzs: 2,
    },
    domainName: "kios.untuvaopintopolku.fi",
    databaseName: "kios",
  },
  test: {
    name: "qa",
    account: "961341546901",
    region: "eu-west-1",
    network: {
      cidr: "10.15.64.0/18",
      maxAzs: 3,
    },
    domainName: "kios.testiopintopolku.fi",
    databaseName: "kios",
  },
  prod: {
    name: "prod",
    account: "515966535475",
    region: "eu-west-1",
    network: {
      cidr: "10.15.128.0/18",
      maxAzs: 3,
    },
    domainName: "kios.opintopolku.fi",
    databaseName: "kios",
  },
}

export const utilityAccount = {
  name: "util",
  account: "961341524988",
  region: "eu-west-1",
}
