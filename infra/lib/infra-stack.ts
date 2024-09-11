import * as cdk from "aws-cdk-lib";
import { Construct } from "constructs";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import { GithubActionsStack } from "./github-actions-stack";

export interface InfraStackProps extends cdk.StackProps {
  cidrBlock: string;
  maxAzs: number;
}

export class InfraStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: InfraStackProps) {
    super(scope, id, props);

    const githubActionsStack = new GithubActionsStack(
      this,
      "GithubActionsStack",
      {
        env: props.env,
      },
    );

    const vpc = new ec2.Vpc(this, "Vpc", {
      ipAddresses: ec2.IpAddresses.cidr(props.cidrBlock),
      maxAzs: props.maxAzs,
    });
  }
}
