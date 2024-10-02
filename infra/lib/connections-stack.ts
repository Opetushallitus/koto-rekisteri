import { aws_ec2, Stack, StackProps } from "aws-cdk-lib"
import { ISecurityGroup, Port, SecurityGroup, Vpc } from "aws-cdk-lib/aws-ec2"
import { Construct } from "constructs"

interface ConnectionsStackProps extends StackProps {
  vpc: Vpc
}

export class ConnectionsStack extends Stack {
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
