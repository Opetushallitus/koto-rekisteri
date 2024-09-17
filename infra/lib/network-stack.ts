import * as cdk from "aws-cdk-lib"
import { Construct } from "constructs"
import * as ec2 from "aws-cdk-lib/aws-ec2"
import { Vpc } from "aws-cdk-lib/aws-ec2"

interface NetworkStackProps extends cdk.StackProps {
  maxAzs: number
  cidrBlock: string
}

export class NetworkStack extends cdk.Stack {
  readonly vpc: Vpc

  constructor(scope: Construct, id: string, props: NetworkStackProps) {
    super(scope, id, props)

    this.vpc = new ec2.Vpc(this, "Vpc", {
      ipAddresses: ec2.IpAddresses.cidr(props.cidrBlock),
      maxAzs: props.maxAzs,
    })
  }
}
