export interface EnvironmentConfig {
  name: string
  account: string
  region: string
  network: {
    cidr: string
    maxAzs: number
  }
  domainName: string
  databaseName: string
  productionQuality: boolean
  slackWorkspaceId: string
  slackAlarmsChannel: SlackChannel
  slackInfoChannel?: SlackChannel
  koski: {
    region: string
    account: string
  }
}

export type SlackChannel = {
  name: string
  id: string
}

// CIDR allocation strategy:
// Top: 10.15.0.0/16
// VPCs: 10.15.0.0/18, 10.15.64.0/18, 10.15.128.0/18, 10.15.192.0/18 (16382 addresses)
// Subnets: (let AWS calculate these for us)

export type EnvironmentName = "dev" | "test" | "prod"

export const deploymentAccounts: {
  [A in EnvironmentName]: EnvironmentConfig
} = {
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
    productionQuality: false,
    slackWorkspaceId: "T02C6SZL7KP",
    slackAlarmsChannel: {
      name: "kielitutkintorekisteri-alerts-dev-test",
      id: "C08E14CRZ3J",
    },
    koski: {
      region: "eu-west-1",
      account: "500150530292",
    },
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
    productionQuality: false,
    slackWorkspaceId: "T02C6SZL7KP",
    slackAlarmsChannel: {
      name: "kielitutkintorekisteri-alerts-dev-test",
      id: "C08E14CRZ3J",
    },
    slackInfoChannel: {
      name: "kielitutkintorekisteri-alerts-dev-test",
      id: "C08E14CRZ3J",
    },
    koski: {
      region: "eu-west-1",
      account: "692437769085",
    },
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
    productionQuality: true,
    slackWorkspaceId: "T02C6SZL7KP",
    slackAlarmsChannel: {
      name: "kielitutkintorekisteri-alerts",
      id: "C07QPSYBY7L",
    },
    koski: {
      region: "eu-west-1",
      account: "508832528142",
    },
  },
}

export const utilityAccount = {
  name: "util",
  account: "961341524988",
  region: "eu-west-1",
  slackChannel: {
    name: "kielitutkintorekisteri-alerts",
    id: "C07QPSYBY7L",
  },
  slackWorkspaceId: "T02C6SZL7KP",
}
