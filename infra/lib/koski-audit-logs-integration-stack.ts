import { Stack, StackProps, aws_logs_destinations } from "aws-cdk-lib"
import { Construct } from "constructs"
import {
  FilterPattern,
  LogGroup,
  SubscriptionFilter,
} from "aws-cdk-lib/aws-logs"
import { NodejsFunction } from "aws-cdk-lib/aws-lambda-nodejs"
import { Runtime } from "aws-cdk-lib/aws-lambda"
import path = require("node:path")
import { StringParameter } from "aws-cdk-lib/aws-ssm"
import { Effect, PolicyStatement } from "aws-cdk-lib/aws-iam"

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
      entry: path.join(__dirname, "koski-audit-logs-integration/handler.ts"),
    })

    sendAuditLogsToKoskiLambda.addToRolePolicy(
      new PolicyStatement({
        sid: "AllowSendKoskiSqsQueue",
        effect: Effect.ALLOW,
        actions: ["sqs:SendMessage"],
        resources: [
          `arn:aws:sqs:${koski.region}:${koski.account}:oma-opintopolku-loki-audit-queue`,
        ],
      }),
    )

    new SubscriptionFilter(this, "sendAuditLogsToKoskiSubscriptionFilter", {
      logGroup: serviceAuditLogGroup,
      filterName: "sendAuditLogsToKoski",
      // We only send audit logs to koski, that are in OPH standardized format.
      // The format is checking by this filter - if the event contains word "operation",
      // it will be passed to lambda
      filterPattern: FilterPattern.anyTerm("operation"),
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
