import * as cdk from "aws-cdk-lib";
import { Construct } from "constructs";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import * as ecsPatterns from "aws-cdk-lib/aws-ecs-patterns";
import * as ecs from "aws-cdk-lib/aws-ecs";
import { GithubActionsStack } from "./github-actions-stack";
import {
  aws_certificatemanager,
  aws_cloudfront,
  aws_cloudfront_origins,
} from "aws-cdk-lib";

export interface InfraStackProps extends cdk.StackProps {
  domainName: string;
  cidrBlock: string;
  maxAzs: number;
}

export class InfraStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: InfraStackProps) {
    super(scope, id, props);

    new GithubActionsStack(this, "GithubActionsStack", {
      env: props.env,
    });

    const vpc = new ec2.Vpc(this, "Vpc", {
      ipAddresses: ec2.IpAddresses.cidr(props.cidrBlock),
      maxAzs: props.maxAzs,
    });

    const service = new ecsPatterns.ApplicationLoadBalancedFargateService(
      this,
      "KotoService",
      {
        vpc,
        taskImageOptions: {
          image: ecs.ContainerImage.fromAsset("..", { file: "Dockerfile" }),
          containerPort: 8080,
        },
      },
    );

    new aws_cloudfront.Distribution(this, "KotoCfn", {
      defaultBehavior: {
        origin: new aws_cloudfront_origins.LoadBalancerV2Origin(
          service.loadBalancer,
          {},
        ),
      },
      // FIXME: uncomment when we have a certificate
      // domainNames: [props.domainName],
    });
  }
}
