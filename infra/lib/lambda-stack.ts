import { Stack, StackProps, aws_logs_destinations } from "aws-cdk-lib"
import { Construct } from "constructs"
import { Function, Runtime, Code } from "aws-cdk-lib/aws-lambda"
import * as path from "node:path"
import {
  FilterPattern,
  LogGroup,
  SubscriptionFilter,
} from "aws-cdk-lib/aws-logs"

export interface SendAuditLogsToKoskiStackProps extends StackProps {
  serviceAuditLogGroup: LogGroup
}

export class SendAuditLogsToKoskiStack extends Stack {
  constructor(
    scope: Construct,
    id: string,
    props?: SendAuditLogsToKoskiStackProps,
  ) {
    super(scope, id, props)
    if (!props) {
      throw "Missing Props"
    }

    const sendAuditLogsToKoskiLambda = new Function(
      this,
      "sendAuditLogsToKoski-cdktest",
      {
        runtime: Runtime.NODEJS_LATEST,
        handler: "index.handler",
        code: Code.fromAsset(path.join(__dirname, "./sendAuditLogsToKoski")),
      },
    )

    new SubscriptionFilter(this, "sendAuditLogsToKoski-sf-cdktest", {
      logGroup: props.serviceAuditLogGroup,
      filterName: "sendAuditLogsToKoskiTest-cdktest",
      filterPattern: FilterPattern.allEvents(),
      destination: new aws_logs_destinations.LambdaDestination(
        sendAuditLogsToKoskiLambda,
      ),
    })
  }
}
