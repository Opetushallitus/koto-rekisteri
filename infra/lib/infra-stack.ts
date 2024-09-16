import * as cdk from "aws-cdk-lib"
import { Construct } from "constructs"
import * as ec2 from "aws-cdk-lib/aws-ec2"
import * as ecsPatterns from "aws-cdk-lib/aws-ecs-patterns"
import * as ecs from "aws-cdk-lib/aws-ecs"
import { GithubActionsStack } from "./github-actions-stack"
import {
  aws_certificatemanager,
  aws_cloudfront,
  aws_cloudfront_origins,
  aws_route53,
} from "aws-cdk-lib"
import { Platform } from "aws-cdk-lib/aws-ecr-assets"
import { ViewerProtocolPolicy } from "aws-cdk-lib/aws-cloudfront"
import { RecordTarget } from "aws-cdk-lib/aws-route53"
import { CloudFrontTarget } from "aws-cdk-lib/aws-route53-targets"

export interface InfraStackProps extends cdk.StackProps {
  certificate: aws_certificatemanager.ICertificate
  name: string
  domainName: string
  cidrBlock: string
  maxAzs: number
}

export class InfraStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: InfraStackProps) {
    super(scope, id, props)

    new GithubActionsStack(this, "GithubActionsStack", {
      env: props.env,
    })

    const vpc = new ec2.Vpc(this, "Vpc", {
      ipAddresses: ec2.IpAddresses.cidr(props.cidrBlock),
      maxAzs: props.maxAzs,
    })

    const service = new ecsPatterns.ApplicationLoadBalancedFargateService(
      this,
      "KotoService",
      {
        vpc,
        taskImageOptions: {
          image: ecs.ContainerImage.fromAsset("..", {
            file: "Dockerfile",
            platform: Platform.LINUX_AMD64,
          }),
          containerPort: 8080,
          environment: {
            SPRING_PROFILES_ACTIVE: props.name,
          },
        },
        cpu: 1024,
        memoryLimitMiB: 2048,
        circuitBreaker: {
          enable: true,
          rollback: true,
        },
      },
    )
    // ApplicationLoadBalancedFargateService doesn't let us configure this in the constructor.
    service.targetGroup.configureHealthCheck({
      ...service.targetGroup.healthCheck,
      path: "/actuator/health",
    })

    const distribution = new aws_cloudfront.Distribution(this, "KotoCfn", {
      defaultBehavior: {
        origin: new aws_cloudfront_origins.LoadBalancerV2Origin(
          service.loadBalancer,
          {},
        ),
        viewerProtocolPolicy: ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
      },
      domainNames: [props.domainName],
      certificate: props.certificate,
    })

    const zone = aws_route53.HostedZone.fromLookup(this, "Zone", {
      domainName: props.domainName,
    })

    const recordTarget = RecordTarget.fromAlias(
      new CloudFrontTarget(distribution),
    )

    new aws_route53.ARecord(this, "RecordIpV4", {
      recordName: props.domainName,
      target: recordTarget,
      zone,
    })
    new aws_route53.AaaaRecord(this, "RecordIpV6", {
      recordName: props.domainName,
      target: recordTarget,
      zone,
    })
  }
}
