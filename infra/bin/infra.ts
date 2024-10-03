#!/usr/bin/env node
import "source-map-support/register"
import { App } from "aws-cdk-lib"
import { EcrImage } from "aws-cdk-lib/aws-ecs"
import { deploymentAccounts, utilityAccount } from "../lib/accounts"
import { getEnv } from "../lib/env"
import { EnvironmentStage } from "../lib/environment-stage"
import { UtilityStage } from "../lib/utility-stage"

// CIDR allocation strategy:
// Top: 10.15.0.0/16
// VPCs: 10.15.0.0/18, 10.15.64.0/18, 10.15.128.0/18, 10.15.192.0/18 (16382 addresses)
// Subnets: (let AWS calculate these for us)

const app = new App()

const utilityStage = new UtilityStage(app, "Util", {
  env: utilityAccount,
  allowPullsFromAccounts: [
    deploymentAccounts.dev.account,
    deploymentAccounts.test.account,
    deploymentAccounts.prod.account,
  ],
})

const serviceImage = EcrImage.fromEcrRepository(
  utilityStage.imageBuildsStack.repository,
  getEnv("TAG"),
)

new EnvironmentStage(app, "Dev", {
  env: deploymentAccounts.dev,
  environmentConfig: deploymentAccounts.dev,
  serviceImage,
})
new EnvironmentStage(app, "Test", {
  env: deploymentAccounts.test,
  environmentConfig: deploymentAccounts.test,
  serviceImage,
})
new EnvironmentStage(app, "Prod", {
  env: deploymentAccounts.prod,
  environmentConfig: deploymentAccounts.prod,
  serviceImage,
})
