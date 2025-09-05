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

export interface KoskiAuditLogsIntegrationStackProps extends StackProps {
  serviceAuditLogGroup: LogGroup
}

export class KoskiAuditLogsIntegrationStack extends Stack {
  constructor(
    scope: Construct,
    id: string,
    props: KoskiAuditLogsIntegrationStackProps,
  ) {
    super(scope, id, props)
    const sendAuditLogsToKoskiLambda = new NodejsFunction(this, "function", {
      runtime: Runtime.NODEJS_LATEST,
      entry: path.join(__dirname, "koski-audit-logs-integration/handler.ts"),
    })

    new SubscriptionFilter(this, "sendAuditLogsToKoskiSubscriptionFilter", {
      logGroup: props.serviceAuditLogGroup,
      filterName: "sendAuditLogsToKoski",
      filterPattern: FilterPattern.allEvents(),
      destination: new aws_logs_destinations.LambdaDestination(
        sendAuditLogsToKoskiLambda,
      ),
    })
  }
}
