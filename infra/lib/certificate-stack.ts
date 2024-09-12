import * as cdk from "aws-cdk-lib";
import { aws_certificatemanager, aws_route53 } from "aws-cdk-lib";
import { Construct } from "constructs";

export interface CertificateStackProps extends cdk.StackProps {
  hostedZone: aws_route53.IHostedZone;
  domainName: string;
}

export class CertificateStack extends cdk.Stack {
  public readonly certificate: aws_certificatemanager.Certificate;

  constructor(scope: Construct, id: string, props: CertificateStackProps) {
    super(scope, id, props);

    this.certificate = new aws_certificatemanager.Certificate(
      this,
      "Certificate",
      {
        domainName: props.domainName,
        validation: aws_certificatemanager.CertificateValidation.fromDns(
          props.hostedZone,
        ),
      },
    );
  }
}
