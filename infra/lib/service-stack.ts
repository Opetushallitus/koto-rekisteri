import { aws_ecs, aws_secretsmanager, Stack, StackProps } from "aws-cdk-lib"
import {
  Certificate,
  CertificateValidation,
} from "aws-cdk-lib/aws-certificatemanager"
import { TreatMissingData } from "aws-cdk-lib/aws-cloudwatch"
import { SnsAction } from "aws-cdk-lib/aws-cloudwatch-actions"
import { IVpc, SecurityGroup } from "aws-cdk-lib/aws-ec2"
import { ContainerImage, LogDriver, Secret } from "aws-cdk-lib/aws-ecs"
import { ApplicationLoadBalancedFargateService } from "aws-cdk-lib/aws-ecs-patterns"
import {
  ApplicationLoadBalancer,
  ApplicationProtocol,
  HttpCodeTarget,
  SslPolicy,
} from "aws-cdk-lib/aws-elasticloadbalancingv2"
import { ILogGroup } from "aws-cdk-lib/aws-logs"
import { DatabaseCluster } from "aws-cdk-lib/aws-rds"
import { HostedZone } from "aws-cdk-lib/aws-route53"
import { ITopic } from "aws-cdk-lib/aws-sns"
import { Construct } from "constructs"
import * as s3 from "aws-cdk-lib/aws-s3"
import * as cdk from "aws-cdk-lib"

export interface ServiceStackProps extends StackProps {
  auditLogGroup: ILogGroup
  logGroup: ILogGroup
  image: ContainerImage
  name: string
  domainName: string
  serviceSecurityGroup: SecurityGroup
  loadBalancerSecurityGroup: SecurityGroup
  vpc: IVpc
  database: DatabaseCluster
  databaseName: string
  alarmSnsTopic: ITopic
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

    const loadBalancer = new ApplicationLoadBalancer(this, "LoadBalancer", {
      vpc: props.vpc,
      securityGroup: props.loadBalancerSecurityGroup,
      internetFacing: true,
    })

    // Create an S3 bucket
    new s3.Bucket(this, "Kitu-Bucket", {
      bucketName: `kitu-bucket-${props.name}`,
      publicReadAccess: false,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
    })

    const snsAction = new SnsAction(props.alarmSnsTopic)
    const alarm5xx = loadBalancer.metrics
      .httpCodeTarget(HttpCodeTarget.TARGET_5XX_COUNT)
      .createAlarm(this, "LoadBalancer5xxAlarm", {
        evaluationPeriods: 1,
        threshold: 1,
        treatMissingData: TreatMissingData.NOT_BREACHING,
      })
    alarm5xx.addAlarmAction(snsAction)
    alarm5xx.addOkAction(snsAction)

    this.service = new ApplicationLoadBalancedFargateService(this, "Kitu", {
      vpc: props.vpc,
      securityGroups: [props.serviceSecurityGroup],
      loadBalancer: loadBalancer,
      taskImageOptions: {
        image: props.image,
        containerPort: 8080,
        logDriver: LogDriver.awsLogs({
          logGroup: props.logGroup,
          streamPrefix: "kitu",
        }),
        environment: {
          SPRING_PROFILES_ACTIVE: props.name,
          DATABASE_URL: `jdbc:postgresql://${props.database.clusterEndpoint.socketAddress}/${props.databaseName}`,
          DATABASE_USER: dbUser,
          AUDIT_LOG_LOG_GROUP_NAME: props.auditLogGroup.logGroupName,
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
          YKI_API_PASSWORD: aws_ecs.Secret.fromSecretsManager(
            aws_secretsmanager.Secret.fromSecretNameV2(
              this,
              "YkiApiPassword",
              "yki-api-password",
            ),
          ),
          YKI_API_USER: aws_ecs.Secret.fromSecretsManager(
            aws_secretsmanager.Secret.fromSecretNameV2(
              this,
              "YkiApiUser",
              "yki-api-user",
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

    props.auditLogGroup.grantWrite(this.service.service.taskDefinition.taskRole)

    this.service.service
      .metricCpuUtilization()
      .createAlarm(this, "CpuUtilization", {
        threshold: 50,
        evaluationPeriods: 1,
      })
      .addAlarmAction(snsAction)

    this.service.service
      .metricMemoryUtilization()
      .createAlarm(this, "MemoryUtilization", {
        threshold: 50,
        evaluationPeriods: 1,
      })
      .addAlarmAction(snsAction)
  }
}
