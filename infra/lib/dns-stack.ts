import * as cdk from "aws-cdk-lib"
import { aws_route53 } from "aws-cdk-lib"
import { Construct } from "constructs"

export interface DnsStackProps extends cdk.StackProps {
  name: string
}

export class DnsStack extends cdk.Stack {
  readonly hostedZone: aws_route53.IHostedZone

  constructor(scope: Construct, id: string, props: DnsStackProps) {
    super(scope, id, props)

    // note: DNS zone created separately to make it harder to accidentally destroy
    this.hostedZone = aws_route53.HostedZone.fromLookup(this, "HostedZone", {
      domainName: props.name,
    })
  }
}
