#!/usr/bin/env node
import "source-map-support/register";
import * as cdk from "aws-cdk-lib";
import { InfraStack } from "../lib/infra-stack";

// CIDR allocation strategy:
// Top: 10.15.0.0/16
// VPCs: 10.15.0.0/18, 10.15.64.0/18, 10.15.128.0/18, 10.15.192.0/18 (16382 addresses)
// Subnets: ...

const accounts = {
  dev: {
    account: "682033502734",
    region: "eu-west-1",
  },
  network: {
    cidrs: {
      dev: "10.15.0.0/18",
    },
    maxAzs: {
      dev: 2,
    },
  },
};

const app = new cdk.App();

const devStack = new InfraStack(app, "InfraStack", {
  env: accounts.dev,
  cidrBlock: accounts.network.cidrs.dev,
  maxAzs: accounts.network.maxAzs.dev,
});
