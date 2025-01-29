import { aws_sns, Stack, StackProps } from "aws-cdk-lib"
import { Stats, TreatMissingData } from "aws-cdk-lib/aws-cloudwatch"
import { SnsAction } from "aws-cdk-lib/aws-cloudwatch-actions"
import {
  CustomDataIdentifier,
  DataProtectionPolicy,
  FilterPattern,
  LogGroup,
} from "aws-cdk-lib/aws-logs"
import { Construct } from "constructs"

export interface LogGroupsStackProps extends StackProps {
  alarmsSnsTopic: aws_sns.ITopic
}

export class LogGroupsStack extends Stack {
  readonly serviceLogGroup: LogGroup
  readonly serviceAuditLogGroup: LogGroup

  constructor(scope: Construct, id: string, props: LogGroupsStackProps) {
    super(scope, id, props)

    const dataProtectionAuditLogGroup = new LogGroup(
      this,
      "DataProtectionAudit",
      {
        logGroupName: "KituServiceDataProtectionAudit",
      },
    )

    const FINNISH_SSN_REGEX =
      "((0[1-9]|[12][0-9]|3[01]))((0[1-9]|1[0-2]))(\\d{2})([-AaBbCcDdEeFfXxYyWwVvUu+])(\\d{3})([0-9A-Ya-y])"
    const dataProtectionPolicy = new DataProtectionPolicy({
      name: "KituDataProtectionPolicy",
      description: "Kitu data protection policy",
      identifiers: [new CustomDataIdentifier("Finnish_SSN", FINNISH_SSN_REGEX)],
      logGroupAuditDestination: dataProtectionAuditLogGroup,
    })

    this.serviceLogGroup = new LogGroup(this, "Service", {
      logGroupName: "KituService",
      dataProtectionPolicy,
    })
    this.serviceAuditLogGroup = new LogGroup(this, "ServiceAudit", {
      logGroupName: "KituServiceAudit",
    })

    const errorsAlarm = this.serviceLogGroup
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
      .metric({ statistic: Stats.SAMPLE_COUNT })
      .createAlarm(this, "ErrorsAlarm", {
        threshold: 1,
        evaluationPeriods: 1,
        treatMissingData: TreatMissingData.NOT_BREACHING,
      })

    errorsAlarm.addAlarmAction(new SnsAction(props.alarmsSnsTopic))
    errorsAlarm.addOkAction(new SnsAction(props.alarmsSnsTopic))
  }
}
