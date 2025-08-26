import { Stack, StackProps, aws_logs_destinations } from "aws-cdk-lib"
import { Construct } from "constructs"
import {
  FilterPattern,
  LogGroup,
  SubscriptionFilter,
} from "aws-cdk-lib/aws-logs"
import { NodejsFunction } from "aws-cdk-lib/aws-lambda-nodejs"
import { Runtime } from "aws-cdk-lib/aws-lambda"
import { StringParameter } from "aws-cdk-lib/aws-ssm"
import { EnvironmentConfig } from "./accounts"

export interface KoskiAuditLogsIntegrationStackProps extends StackProps {
  serviceAuditLogGroup: LogGroup
  environmentConfig: EnvironmentConfig
}

export class KoskiAuditLogsIntegrationStack extends Stack {
  constructor(
    scope: Construct,
    id: string,
    props: KoskiAuditLogsIntegrationStackProps,
  ) {
    super(scope, id, props)
    const { serviceAuditLogGroup, environmentConfig } = props

    const sendAuditLogsToKoskiLambda = new NodejsFunction(this, "function", {
      runtime: Runtime.NODEJS_LATEST,
    })

    new SubscriptionFilter(this, "sendAuditLogsToKoskiSubscriptionFilter", {
      logGroup: serviceAuditLogGroup,
      filterName: "sendAuditLogsToKoski",
      filterPattern: FilterPattern.allEvents(),
      destination: new aws_logs_destinations.LambdaDestination(
        sendAuditLogsToKoskiLambda,
      ),
    })

    new StringParameter(this, "KoskiIntegration", {
      parameterName: `/kitu/koski-integration/sqs-audit-queue-url`,
      stringValue: `https://${environmentConfig.koski.region}.amazonaws.com/${environmentConfig.koski.accountId}/oma-opintopolku-loki-audit-queue`,
    })
  }
}
