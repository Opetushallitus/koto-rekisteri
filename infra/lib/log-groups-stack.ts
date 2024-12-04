import { aws_sns, Stack, StackProps } from "aws-cdk-lib"
import { TreatMissingData } from "aws-cdk-lib/aws-cloudwatch"
import { SnsAction } from "aws-cdk-lib/aws-cloudwatch-actions"
import { FilterPattern, LogGroup } from "aws-cdk-lib/aws-logs"
import { Construct } from "constructs"

export interface LogGroupsStackProps extends StackProps {
  alarmsSnsTopic: aws_sns.ITopic
}

export class LogGroupsStack extends Stack {
  readonly serviceLogGroup: LogGroup
  readonly serviceAuditLogGroup: LogGroup

  constructor(scope: Construct, id: string, props: LogGroupsStackProps) {
    super(scope, id, props)

    this.serviceLogGroup = new LogGroup(this, "Service", {
      logGroupName: "KituService",
    })
    this.serviceAuditLogGroup = new LogGroup(this, "ServiceAudit", {
      logGroupName: "KituServiceAudit",
    })

    const alarm = this.serviceLogGroup
      .addMetricFilter("Errors", {
        metricName: "LogErrors",
        metricNamespace: "Kitu",
        filterPattern: FilterPattern.any(
          FilterPattern.booleanValue("$.success", false),
          FilterPattern.exists("$.stack_trace"),
          FilterPattern.exists("$.error.type"),
          FilterPattern.stringValue("$.level", "=", "WARN"),
          FilterPattern.stringValue("$.log.level", "=", "WARN"),
          FilterPattern.stringValue("$.level", "=", "ERROR"),
          FilterPattern.stringValue("$.log.level", "=", "ERROR"),
        ),
      })
      .metric()
      .createAlarm(this, "ErrorsAlarm", {
        threshold: 1,
        evaluationPeriods: 1,
        treatMissingData: TreatMissingData.NOT_BREACHING,
      })

    alarm.addAlarmAction(new SnsAction(props.alarmsSnsTopic))
    alarm.addOkAction(new SnsAction(props.alarmsSnsTopic))
  }
}
