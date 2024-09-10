#!/usr/bin/env node
import "source-map-support/register";
import * as cdk from "aws-cdk-lib";
import { InfraStack } from "../lib/infra-stack";

const accounts = {
  dev: { account: "682033502734", region: "eu-west-1" },
};

const app = new cdk.App();

new InfraStack(app, "InfraStack", {
  env: accounts.dev,
});
