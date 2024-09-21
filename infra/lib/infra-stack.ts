import * as cdk from "aws-cdk-lib"
import { aws_certificatemanager, aws_ecs, aws_route53 } from "aws-cdk-lib"
import { Construct } from "constructs"
import { IVpc } from "aws-cdk-lib/aws-ec2"
import * as ecs_patterns from "aws-cdk-lib/aws-ecs-patterns"
import * as ecs from "aws-cdk-lib/aws-ecs"
import { GithubActionsStack } from "./github-actions-stack"
import { Platform } from "aws-cdk-lib/aws-ecr-assets"
import { DatabaseCluster } from "aws-cdk-lib/aws-rds"
import {
  ApplicationProtocol,
  SslPolicy,
} from "aws-cdk-lib/aws-elasticloadbalancingv2"
import { CertificateValidation } from "aws-cdk-lib/aws-certificatemanager"

export interface InfraStackProps extends cdk.StackProps {
  name: string
  domainName: string
  vpc: IVpc
  database: DatabaseCluster
}

export class InfraStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: InfraStackProps) {
    super(scope, id, props)

    new GithubActionsStack(this, "GithubActionsStack", {
      env: props.env,
    })

    // Default Postgres DB user: https://docs.aws.amazon.com/cdk/api/v2/docs/aws-cdk-lib.aws_rds.DatabaseCluster.html#credentials
    const dbUser = "postgres"

    const zone = aws_route53.HostedZone.fromLookup(this, "Zone", {
      domainName: props.domainName,
    })

    const certificate = new aws_certificatemanager.Certificate(
      this,
      "Certificate",
      {
        domainName: props.domainName,
        validation: CertificateValidation.fromDns(zone),
      },
    )

    const service = new ecs_patterns.ApplicationLoadBalancedFargateService(
      this,
      "KotoService",
      {
        vpc: props.vpc,
        taskImageOptions: {
          image: ecs.ContainerImage.fromAsset("..", {
            file: "Dockerfile",
            platform: Platform.LINUX_AMD64,
          }),
          containerPort: 8080,
          environment: {
            SPRING_PROFILES_ACTIVE: props.name,
            DATABASE_URL: `jdbc:postgresql://${props.database.clusterEndpoint.socketAddress}/public`,
            DATABASE_USER: dbUser,
          },
          secrets: {
            DATABASE_PASSWORD: aws_ecs.Secret.fromSecretsManager(
              props.database.secret!,
            ),
          },
        },
        cpu: 1024,
        memoryLimitMiB: 2048,
        circuitBreaker: {
          enable: true,
          rollback: true,
        },
        domainName: props.domainName,
        domainZone: zone,
        protocol: ApplicationProtocol.HTTPS,
        redirectHTTP: true,
        certificate,
        sslPolicy: SslPolicy.RECOMMENDED_TLS,
      },
    )

    service.targetGroup.configureHealthCheck({
      ...service.targetGroup.healthCheck,
      path: "/actuator/health",
    })

    props.database.grantConnect(service.taskDefinition.taskRole, dbUser)
  }
}
