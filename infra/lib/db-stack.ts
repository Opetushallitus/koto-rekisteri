import * as cdk from "aws-cdk-lib"
import { Construct } from "constructs"
import { aws_ec2, aws_rds } from "aws-cdk-lib"

export interface DbStackProps extends cdk.StackProps {
  vpc: aws_ec2.IVpc
}

export class DbStack extends cdk.Stack {
  readonly cluster: aws_rds.DatabaseCluster

  constructor(scope: Construct, id: string, props: DbStackProps) {
    super(scope, id, props)

    this.cluster = new aws_rds.DatabaseCluster(this, "DbStack", {
      engine: aws_rds.DatabaseClusterEngine.auroraPostgres({
        version: aws_rds.AuroraPostgresEngineVersion.VER_16_3,
      }),
      vpc: props.vpc,
      writer: aws_rds.ClusterInstance.serverlessV2("writer"),
    })
  }
}
