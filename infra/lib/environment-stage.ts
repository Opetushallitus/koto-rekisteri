import { Stage, StageProps } from "aws-cdk-lib"
import { DockerImageAsset } from "aws-cdk-lib/aws-ecr-assets"
import { Construct } from "constructs"
import { EnvironmentConfig } from "./accounts"
import { DbStack } from "./db-stack"
import { DnsStack } from "./dns-stack"
import { GithubActionsStack } from "./github-actions-stack"
import { NetworkStack } from "./network-stack"
import { ServiceStack } from "./service-stack"

interface EnvironmentStageProps extends StageProps {
  environmentConfig: EnvironmentConfig
  serviceImage: DockerImageAsset
}

export class EnvironmentStage extends Stage {
  constructor(scope: Construct, id: string, props: EnvironmentStageProps) {
    super(scope, id, props)

    const { env, environmentConfig } = props

    new GithubActionsStack(this, "GithubActions", {
      env,
    })

    new DnsStack(this, "Dns", {
      terminationProtection: true,
      env,
      name: environmentConfig.domainName,
    })

    const networkStack = new NetworkStack(this, "Network", {
      env,
      cidrBlock: environmentConfig.network.cidr,
      maxAzs: environmentConfig.network.maxAzs,
    })

    const dbStack = new DbStack(this, "Database", {
      env,
      vpc: networkStack.vpc,
      databaseName: environmentConfig.databaseName,
    })

    new ServiceStack(this, "Service", {
      env,
      name: environmentConfig.name,
      domainName: environmentConfig.domainName,
      vpc: networkStack.vpc,
      database: dbStack.cluster,
      databaseName: environmentConfig.databaseName,
      image: props.serviceImage,
    })
  }
}
