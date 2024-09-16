#!/usr/bin/env node
import "source-map-support/register"
import * as cdk from "aws-cdk-lib"
import { InfraStack } from "../lib/infra-stack"
import { DnsStack } from "../lib/dns-stack"
import { CertificateStack } from "../lib/certificate-stack"

// CIDR allocation strategy:
// Top: 10.15.0.0/16
// VPCs: 10.15.0.0/18, 10.15.64.0/18, 10.15.128.0/18, 10.15.192.0/18 (16382 addresses)
// Subnets: (let AWS calculate these for us)

const environments = {
  dev: {
    account: "682033502734",
    region: "eu-west-1",
    network: {
      cidr: "10.15.0.0/18",
      maxAzs: 2,
    },
    domainName: "kios.untuvaopintopolku.fi",
  },
  test: {
    account: "961341546901",
    region: "eu-west-1",
    network: {
      cidr: "10.15.64.0/18",
      maxAzs: 3,
    },
    domainName: "kios.testiopintopolku.fi",
  },
  prod: {
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

new CertificateStack(app, "CertificateStack", {
  crossRegionReferences: true,
  env: { ...env, region: "us-east-1" },
  hostedZone: dnsStack.hostedZone,
  domainName: env.domainName,
})

new InfraStack(app, "InfraStack", {
  env,
  cidrBlock: env.network.cidr,
  maxAzs: env.network.maxAzs,
  domainName: env.domainName,
})
