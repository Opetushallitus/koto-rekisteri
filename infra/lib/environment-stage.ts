import { aws_ec2, Stack, StackProps, Stage, StageProps } from "aws-cdk-lib"
import { ISecurityGroup, Port, SecurityGroup, Vpc } from "aws-cdk-lib/aws-ec2"
import { ContainerImage } from "aws-cdk-lib/aws-ecs"
import { ApplicationLoadBalancedFargateService } from "aws-cdk-lib/aws-ecs-patterns"
import { DatabaseCluster } from "aws-cdk-lib/aws-rds"
import { Construct } from "constructs"
import { EnvironmentConfig } from "./accounts"
import { DbStack } from "./db-stack"
import { DnsStack } from "./dns-stack"
import { GithubActionsStack } from "./github-actions-stack"
import { NetworkStack } from "./network-stack"
import { ServiceStack } from "./service-stack"

interface EnvironmentStageProps extends StageProps {
  environmentConfig: EnvironmentConfig
  serviceImage: ContainerImage
}

interface ConnectionsStackProps extends StackProps {
  vpc: Vpc
}

class ConnectionsStack extends Stack {
  readonly serviceSG: SecurityGroup
  databaseSG: ISecurityGroup
  readonly loadBalancerSG: SecurityGroup

  constructor(scope: Construct, id: string, props: ConnectionsStackProps) {
    super(scope, id, props)

    this.loadBalancerSG = new aws_ec2.SecurityGroup(this, "LoadBalancerSG", {
      vpc: props.vpc,
    })
    this.serviceSG = new aws_ec2.SecurityGroup(this, "ServiceSG", {
      vpc: props.vpc,
    })
  }

  createRules() {
    this.databaseSG.addIngressRule(this.serviceSG, Port.tcp(5432))
  }
}

export class EnvironmentStage extends Stage {
  constructor(scope: Construct, id: string, props: EnvironmentStageProps) {
    super(scope, id, props)

    const { env, environmentConfig } = props

    new GithubActionsStack(this, "GithubActions", {
      env,
    })

    new DnsStack(this, "Dns", {
      env,
      name: environmentConfig.domainName,
    })

    const networkStack = new NetworkStack(this, "Network", {
      env,
      cidrBlock: environmentConfig.network.cidr,
      maxAzs: environmentConfig.network.maxAzs,
    })

    const connectionsStack = new ConnectionsStack(this, "ConnectionsStack", {
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
      vpc: networkStack.vpc,
      database: dbStack.cluster,
      databaseName: environmentConfig.databaseName,
      image: props.serviceImage,
    })

    connectionsStack.createRules()
  }
}
