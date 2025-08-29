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

export interface KoskiAuditLogsIntegrationStackProps extends StackProps {
  serviceAuditLogGroup: LogGroup
  koski: {
    region: string
    account: string
  }
}

export class KoskiAuditLogsIntegrationStack extends Stack {
  constructor(
    scope: Construct,
    id: string,
    props: KoskiAuditLogsIntegrationStackProps,
  ) {
    super(scope, id, props)
    const { serviceAuditLogGroup, koski } = props

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

    const auditQueueParam = new StringParameter(
      this,
      "KoskiAuditLogsIntegrationParams",
      {
        parameterName:
          "/kitu/koski-integration/oma-opintopolku-loki-audit-queue",
        stringValue: `https://sqs.${koski.region}.amazonaws.com/${koski.account}/oma-opintopolku-loki-audit-queue`,
      },
    )

    auditQueueParam.grantRead(sendAuditLogsToKoskiLambda)
  }
}
