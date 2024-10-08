import { Stage, StageProps } from "aws-cdk-lib"
import { ContainerImage } from "aws-cdk-lib/aws-ecs"
import { Construct } from "constructs"
import { EnvironmentConfig } from "./accounts"
import { AlarmsStack } from "./alarms-stack"
import { ConnectionsStack } from "./connections-stack"
import { DbStack } from "./db-stack"
import { DnsStack } from "./dns-stack"
import { GithubActionsStack } from "./github-actions-stack"
import { LogGroupsStack } from "./log-groups-stack"
import { NetworkStack } from "./network-stack"
import { ServiceStack } from "./service-stack"

interface EnvironmentStageProps extends StageProps {
  environmentConfig: EnvironmentConfig
  serviceImage: ContainerImage
}

export class EnvironmentStage extends Stage {
  constructor(scope: Construct, id: string, props: EnvironmentStageProps) {
    super(scope, id, props)

    const { env, environmentConfig } = props

    new GithubActionsStack(this, "GithubActions", {
      env,
    })

    const alarmsStack = new AlarmsStack(this, "Alarms", { env })

    new DnsStack(this, "Dns", {
      env,
      name: environmentConfig.domainName,
    })

    const logGroupsStack = new LogGroupsStack(this, "LogGroups", {
      env,
      alarmsSnsTopic: alarmsStack.alarmSnsTopic,
    })

    const networkStack = new NetworkStack(this, "Network", {
      env,
      cidrBlock: environmentConfig.network.cidr,
      maxAzs: environmentConfig.network.maxAzs,
    })

    const connectionsStack = new ConnectionsStack(this, "Connections", {
      env,
      vpc: networkStack.vpc,
    })

    const dbStack = new DbStack(this, "Database", {
      env,
      vpc: networkStack.vpc,
      databaseName: environmentConfig.databaseName,
    })

    connectionsStack.databaseSG = dbStack.cluster.connections.securityGroups[0]

    new ServiceStack(this, "Service", {
      env,
      name: environmentConfig.name,
      domainName: environmentConfig.domainName,
      serviceSecurityGroup: connectionsStack.serviceSG,
      loadBalancerSecurityGroup: connectionsStack.loadBalancerSG,
      logGroup: logGroupsStack.serviceLogGroup,
      vpc: networkStack.vpc,
      database: dbStack.cluster,
      databaseName: environmentConfig.databaseName,
      image: props.serviceImage,
      alarmSnsTopic: alarmsStack.alarmSnsTopic,
    })

    connectionsStack.createRules()
  }
}
