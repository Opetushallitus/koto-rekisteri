import { aws_ecs, aws_secretsmanager, Stack, StackProps } from "aws-cdk-lib"
import {
  Certificate,
  CertificateValidation,
} from "aws-cdk-lib/aws-certificatemanager"
import { IVpc, SecurityGroup } from "aws-cdk-lib/aws-ec2"
import { ContainerImage, Secret } from "aws-cdk-lib/aws-ecs"
import { ApplicationLoadBalancedFargateService } from "aws-cdk-lib/aws-ecs-patterns"
import {
  ApplicationLoadBalancer,
  ApplicationProtocol,
  SslPolicy,
} from "aws-cdk-lib/aws-elasticloadbalancingv2"
import { DatabaseCluster } from "aws-cdk-lib/aws-rds"
import { HostedZone } from "aws-cdk-lib/aws-route53"
import { Construct } from "constructs"

export interface ServiceStackProps extends StackProps {
  image: ContainerImage
  name: string
  domainName: string
  serviceSecurityGroup: SecurityGroup
  loadBalancerSecurityGroup: SecurityGroup
  vpc: IVpc
  database: DatabaseCluster
  databaseName: string
}

export class ServiceStack extends Stack {
  readonly service: ApplicationLoadBalancedFargateService

  constructor(scope: Construct, id: string, props: ServiceStackProps) {
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

    this.service = new ApplicationLoadBalancedFargateService(this, "Kitu", {
      vpc: props.vpc,
      securityGroups: [props.serviceSecurityGroup],
      loadBalancer: new ApplicationLoadBalancer(this, "LoadBalancer", {
        vpc: props.vpc,
        securityGroup: props.loadBalancerSecurityGroup,
        internetFacing: true,
      }),
      taskImageOptions: {
        image: props.image,
        containerPort: 8080,
        environment: {
          SPRING_PROFILES_ACTIVE: props.name,
          DATABASE_URL: `jdbc:postgresql://${props.database.clusterEndpoint.socketAddress}/${props.databaseName}`,
          DATABASE_USER: dbUser,
        },
        secrets: {
          DATABASE_PASSWORD: Secret.fromSecretsManager(
            props.database.secret!,
            "password",
          ),
          KIELITESTI_TOKEN: aws_ecs.Secret.fromSecretsManager(
            aws_secretsmanager.Secret.fromSecretNameV2(
              this,
              "KielitestiToken",
              "kielitesti-token",
            ),
          ),
          OPPIJANUMERO_PASSWORD: aws_ecs.Secret.fromSecretsManager(
            aws_secretsmanager.Secret.fromSecretNameV2(
              this,
              "OppijanumeroPassword",
              "oppijanumero-password",
            ),
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
    })

    this.service.targetGroup.configureHealthCheck({
      ...this.service.targetGroup.healthCheck,
      path: "/actuator/health",
    })
  }
}
