import { Stack, StackProps } from "aws-cdk-lib"
import {
  Certificate,
  CertificateValidation,
} from "aws-cdk-lib/aws-certificatemanager"
import { IVpc } from "aws-cdk-lib/aws-ec2"
import { DockerImageAsset } from "aws-cdk-lib/aws-ecr-assets"
import { ContainerImage, Secret } from "aws-cdk-lib/aws-ecs"
import { ApplicationLoadBalancedFargateService } from "aws-cdk-lib/aws-ecs-patterns"
import {
  ApplicationProtocol,
  SslPolicy,
} from "aws-cdk-lib/aws-elasticloadbalancingv2"
import { DatabaseCluster } from "aws-cdk-lib/aws-rds"
import { HostedZone } from "aws-cdk-lib/aws-route53"
import { Construct } from "constructs"

export interface InfraStackProps extends StackProps {
  image: DockerImageAsset
  name: string
  domainName: string
  vpc: IVpc
  database: DatabaseCluster
  databaseName: string
}

export class ServiceStack extends Stack {
  constructor(scope: Construct, id: string, props: InfraStackProps) {
    super(scope, id, props)

    // Default Postgres DB user: https://docs.aws.amazon.com/cdk/api/v2/docs/aws-cdk-lib.aws_rds.DatabaseCluster.html#credentials
    const dbUser = "postgres"

    const zone = HostedZone.fromLookup(this, "Zone", {
      domainName: props.domainName,
    })

    const certificate = new Certificate(this, "Certificate", {
      domainName: props.domainName,
      validation: CertificateValidation.fromDns(zone),
    })

    const service = new ApplicationLoadBalancedFargateService(this, "Kitu", {
      vpc: props.vpc,
      taskImageOptions: {
        image: ContainerImage.fromDockerImageAsset(props.image),
        containerPort: 8080,
        environment: {
          SPRING_PROFILES_ACTIVE: props.name,
          DATABASE_URL: `jdbc:postgresql://${props.database.clusterEndpoint.socketAddress}/${props.databaseName}`,
          DATABASE_USER: dbUser,
        },
        secrets: {
          DATABASE_PASSWORD: Secret.fromSecretsManager(props.database.secret!),
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
    })

    service.targetGroup.configureHealthCheck({
      ...service.targetGroup.healthCheck,
      path: "/actuator/health",
    })

    props.database.grantConnect(service.taskDefinition.taskRole, dbUser)
  }
}
