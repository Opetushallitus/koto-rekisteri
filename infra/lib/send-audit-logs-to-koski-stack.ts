import { Stack, StackProps, aws_logs_destinations } from "aws-cdk-lib"
import { Construct } from "constructs"
import {
  FilterPattern,
  LogGroup,
  SubscriptionFilter,
} from "aws-cdk-lib/aws-logs"
import { SendAuditLogsToKoski } from "./send-audit-logs-to-koski"

export interface SendAuditLogsToKoskiStackProps extends StackProps {
  serviceAuditLogGroup: LogGroup
}

export class SendAuditLogsToKoskiStack extends Stack {
  constructor(
    scope: Construct,
    id: string,
    props: SendAuditLogsToKoskiStackProps,
  ) {
    super(scope, id, props)
    const sendAuditLogsToKoskiLambda = new SendAuditLogsToKoski(
      this,
      "sendAuditLogsToKoski",
    )

    new SubscriptionFilter(this, "sendAuditLogsToKoski", {
      logGroup: props.serviceAuditLogGroup,
      filterName: "sendAuditLogsToKoski",
      filterPattern: FilterPattern.allEvents(),
      destination: new aws_logs_destinations.LambdaDestination(
        sendAuditLogsToKoskiLambda.function,
      ),
    })
  }
}
