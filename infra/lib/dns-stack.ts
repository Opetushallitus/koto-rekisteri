import * as cdk from "aws-cdk-lib";
import { aws_route53 } from "aws-cdk-lib";
import { Construct } from "constructs";

export interface DnsStackProps extends cdk.StackProps {
  name: string;
}

export class DnsStack extends cdk.Stack {
  public readonly hostedZone: aws_route53.HostedZone;

  constructor(scope: Construct, id: string, props: DnsStackProps) {
    super(scope, id, props);

    this.hostedZone = new aws_route53.HostedZone(this, "HostedZone", {
      zoneName: props.name,
    });
  }
}
