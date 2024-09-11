import * as cdk from "aws-cdk-lib";
import { Construct } from "constructs";
import * as ec2 from "aws-cdk-lib/aws-ec2";

export interface InfraStackProps extends cdk.StackProps {
  cidrBlock: string;
  maxAzs: number;
}

export class InfraStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: InfraStackProps) {
    super(scope, id, props);

    const vpc = new ec2.Vpc(this, "Vpc", {
      ipAddresses: ec2.IpAddresses.cidr(props.cidrBlock),
      maxAzs: props.maxAzs,
    });
  }
}
