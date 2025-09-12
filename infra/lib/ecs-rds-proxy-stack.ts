import { CfnOutput, Stack, StackProps } from "aws-cdk-lib"
import { IVpc, SecurityGroup } from "aws-cdk-lib/aws-ec2"
import {
  ContainerImage,
  FargateTaskDefinition,
  LinuxParameters,
  LogDriver,
} from "aws-cdk-lib/aws-ecs"
import {
  ManagedPolicy,
  PolicyStatement,
  Role,
  ServicePrincipal,
} from "aws-cdk-lib/aws-iam"
import { LogGroup } from "aws-cdk-lib/aws-logs"
import { IDatabaseCluster } from "aws-cdk-lib/aws-rds"
import { Construct } from "constructs"

interface EcsRdsProxyStackProps extends StackProps {
  vpc: IVpc
  targetRdsCluster: IDatabaseCluster
}

export class EcsRdsProxyStack extends Stack {
  private readonly taskDefinition: FargateTaskDefinition
  private readonly taskExecutionRole: Role
  private readonly securityGroup: SecurityGroup
  private readonly logGroup: LogGroup

  constructor(scope: Construct, id: string, props: EcsRdsProxyStackProps) {
    super(scope, id, props)

    this.logGroup = new LogGroup(this, "ProxyLogs")

    this.taskDefinition = new FargateTaskDefinition(this, "TaskDefinition")
    // https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-sessions-start.html#sessions-remote-port-forwarding
    this.taskDefinition.addToTaskRolePolicy(
      new PolicyStatement({
        actions: [
          "ssmmessages:CreateControlChannel",
          "ssmmessages:CreateDataChannel",
          "ssmmessages:OpenControlChannel",
          "ssmmessages:OpenDataChannel",
        ],
        resources: ["*"],
      }),
    )
    this.taskDefinition.addContainer("proxy", {
      image: ContainerImage.fromRegistry(
        "public.ecr.aws/amazonlinux/amazonlinux:2023-minimal",
      ),
      logging: LogDriver.awsLogs({
        logGroup: this.logGroup,
        streamPrefix: "proxy",
      }),
      linuxParameters: new LinuxParameters(this, "ProxyLinuxParameters", {
        // install init process to reap background SSM agent
        // https://docs.aws.amazon.com/AmazonECS/latest/developerguide/ecs-exec.html#ecs-exec-enabling-and-using
        initProcessEnabled: true,
      }),
    })

    this.taskExecutionRole = new Role(this, "TaskExecutionRole", {
      assumedBy: new ServicePrincipal("ecs-tasks"),
      managedPolicies: [
        ManagedPolicy.fromAwsManagedPolicyName(
          "service-role/AmazonECSTaskExecutionRolePolicy",
        ),
      ],
    })

    this.securityGroup = new SecurityGroup(this, "ProxySecurityGroup", {
      vpc: props.vpc,
    })
    props.targetRdsCluster.connections.allowDefaultPortFrom(this.securityGroup)

    new CfnOutput(this, "TaskDefinitionArn", {
      value: this.taskDefinition.taskDefinitionArn,
    })
    new CfnOutput(this, "TaskExecutionRoleArn", {
      value: this.taskExecutionRole.roleArn,
    })
    new CfnOutput(this, "SecurityGroup", {
      value: this.securityGroup.securityGroupId,
    })
  }
}
