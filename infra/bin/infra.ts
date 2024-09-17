#!/usr/bin/env node
import "source-map-support/register"
import * as cdk from "aws-cdk-lib"
import { InfraStack } from "../lib/infra-stack"
import { DnsStack } from "../lib/dns-stack"
import { CertificateStack } from "../lib/certificate-stack"
import { DbStack } from "../lib/db-stack"
import { NetworkStack } from "../lib/network-stack"

// CIDR allocation strategy:
// Top: 10.15.0.0/16
// VPCs: 10.15.0.0/18, 10.15.64.0/18, 10.15.128.0/18, 10.15.192.0/18 (16382 addresses)
// Subnets: (let AWS calculate these for us)

const environments = {
  dev: {
    name: "untuva",
    account: "682033502734",
    region: "eu-west-1",
    network: {
      cidr: "10.15.0.0/18",
      maxAzs: 2,
    },
    domainName: "kios.untuvaopintopolku.fi",
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
  },
}

type EnvironmentName = keyof typeof environments
type Environment = (typeof environments)[EnvironmentName]
const validEnvironmentNames = Object.keys(environments)
const isValidEnvironmentName = (name: string): name is EnvironmentName =>
  validEnvironmentNames.includes(name)

const app = new cdk.App()

const envName = process.env.KITU_ENV

if (envName === undefined) {
  throw new Error("KITU_ENV required")
}

if (!isValidEnvironmentName(envName)) {
  throw new Error(
    `KITU_ENV invalid value ${envName}, expected one of ${validEnvironmentNames.join(", ")}`,
  )
}

const env: Environment = environments[envName]

const dnsStack = new DnsStack(app, "DnsStack", {
  crossRegionReferences: true,
  env,
  name: env.domainName,
  terminationProtection: true,
})

const certificateStack = new CertificateStack(app, "CertificateStack", {
  crossRegionReferences: true,
  env: { ...env, region: "us-east-1" },
  hostedZone: dnsStack.hostedZone,
  domainName: env.domainName,
})

const networkStack = new NetworkStack(app, "NetworkStack", {
  env,
  cidrBlock: env.network.cidr,
  maxAzs: env.network.maxAzs,
})

const dbStack = new DbStack(app, "DbStack", { env, vpc: networkStack.vpc })

new InfraStack(app, "InfraStack", {
  crossRegionReferences: true,
  env,
  name: env.name,
  domainName: env.domainName,
  certificate: certificateStack.certificate,
  vpc: networkStack.vpc,
  database: dbStack.cluster,
})
