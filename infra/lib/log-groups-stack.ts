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

  constructor(scope: Construct, id: string, props: LogGroupsStackProps) {
    super(scope, id, props)

    this.serviceLogGroup = new LogGroup(this, "Service", {
      logGroupName: "KituService",
    })

    const alarm = this.serviceLogGroup
      .addMetricFilter("Errors", {
        metricName: "LogErrors",
        metricNamespace: "Kitu",
        filterPattern: FilterPattern.anyTerm("ERROR"),
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
