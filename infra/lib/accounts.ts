export interface EnvironmentConfig {
  name: string
  network: {
    cidr: string
    maxAzs: number
  }
  domainName: string
  databaseName: string
  productionQuality: boolean
  slackWorkspaceId: string
  slackChannelName: string
  slackChannelId: string
}

// CIDR allocation strategy:
// Top: 10.15.0.0/16
// VPCs: 10.15.0.0/18, 10.15.64.0/18, 10.15.128.0/18, 10.15.192.0/18 (16382 addresses)
// Subnets: (let AWS calculate these for us)

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
    productionQuality: false,
    slackWorkspaceId: "T02C6SZL7KP",
    slackChannelName: "koto-rekisteri-alerts-dev-test",
    alertsSlackChanneId: "C08E14CRZ3J",
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
    slackChannelName: "koto-rekisteri-alerts-dev-test",
    alertsSlackChanneId: "C08E14CRZ3J",
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
    slackChannelName: "koto-rekisteri-alerts",
    alertsSlackChanneId: "C07QPSYBY7L",
  },
}

export const utilityAccount = {
  name: "util",
  account: "961341524988",
  region: "eu-west-1",
}
