#!/usr/bin/env node
import "source-map-support/register";
import * as cdk from "aws-cdk-lib";
import { InfraStack } from "../lib/infra-stack";

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
    domainName: "kotorekisteri.hahtuvaopintopolku.fi",
  },
  test: {
    account: "961341546901",
    region: "eu-west-1",
    network: {
      cidr: "10.15.64.0/18",
      maxAzs: 3,
    },
    domainName: "kotorekisteri.testiopintopolku.fi",
  },
  prod: {
    account: "515966535475",
    region: "eu-west-1",
    network: {
      cidr: "10.15.128.0/18",
      maxAzs: 3,
    },
    domainName: "kotorekisteri.opintopolku.fi",
  },
};

type EnvName = keyof typeof environments;

const app = new cdk.App();

const envName = process.env.KITU_ENV;

if (envName === undefined) {
  throw new Error("KITU_ENV required");
}

if (environments[envName as EnvName] === undefined) {
  throw new Error(
    `KITU_ENV invalid value ${envName}, expected one of ${Object.keys(environments).join(", ")}`,
  );
}

const env = environments[envName as EnvName];

new InfraStack(app, "InfraStack", {
  env,
  cidrBlock: env.network.cidr,
  maxAzs: env.network.maxAzs,
  domainName: env.domainName,
});
