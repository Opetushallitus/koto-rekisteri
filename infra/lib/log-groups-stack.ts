import { aws_sns, Stack, StackProps } from "aws-cdk-lib"
import { Stats, TreatMissingData } from "aws-cdk-lib/aws-cloudwatch"
import { SnsAction } from "aws-cdk-lib/aws-cloudwatch-actions"
import { ServicePrincipal } from "aws-cdk-lib/aws-iam"
import {
  CfnResourcePolicy,
  CustomDataIdentifier,
  DataProtectionPolicy,
  FilterPattern,
  LogGroup,
} from "aws-cdk-lib/aws-logs"
import { CfnTransactionSearchConfig } from "aws-cdk-lib/aws-xray"
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

    this.enableTransactionSearch()

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

  /** Enable Transaction Search.
   *
   * This enables querying traces older than 30 days.
   *
   * @see https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-Transaction-Search.html
   * @see https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-Transaction-Search-Cloudformation.html
   */
  private enableTransactionSearch() {
    // This enables Transaction Search. CDK doesn't have a L2 construct for TS yet.
    const transactionSearchConfig = new CfnTransactionSearchConfig(
      this,
      "TransactionSearch",
      {
        indexingPercentage: 100,
      },
    )

    // The rest is granting XRay write rights to the Transaction Search log groups that store the trace data.
    const xray = new ServicePrincipal("xray", {
      conditions: {
        ArnLike: {
          "aws:SourceArn": `arn:${this.partition}:xray:${this.region}:${this.account}:*`,
        },
        StringEquals: {
          "aws:SourceAccount": this.account,
        },
      },
    })

    const transactionSearchSpans = new LogGroup(
      this,
      "TransactionSearchSpans",
      {
        logGroupName: "aws/spans",
        retention: 30,
      },
    )

    const applicationSignalsData = new LogGroup(
      this,
      "ApplicationSignalsData",
      {
        logGroupName: "/aws/application-signals/data",
        retention: 30,
      },
    )

    transactionSearchSpans.grantWrite(xray)
    applicationSignalsData.grantWrite(xray)

    // Transaction Search cannot be enabled until it has write access to the log groups specified above.
    //
    // This means that we need to tell CloudFormation that the transactionSearchConfig resource has to be created *after* the ResourcePolicy resources of these log groups, but CDK does not expose the resource policy resources of L2 log group constructs.
    //
    // Instead, we use the .node property to reach into the L1 resource level and find the appropriate ResourcePolicy resource. AWS has various documentation pages on this:
    //
    // CDK guide: https://docs.aws.amazon.com/cdk/v2/guide/cfn-layer.html
    // Knowledge Center: https://repost.aws/knowledge-center/cdk-retrieve-construct-objects
    //
    // The end result is that the TransactionSearch resource in cdk.out/assembly-${env}/${env}LogGroups.template.json contains this property:
    //
    //   "DependsOn": [
    //     "ApplicationSignalsDataPolicyResourcePolicy921BB654",
    //     "TransactionSearchSpansPolicyResourcePolicy81B12951"
    //    ],

    // Use the following for loop to print the IDs of the children of the L2 LogGroup construct.
    // I guessed that the node with ID 'ResourcePolicy' is the one we want.
    // for (const child of transactionSearchSpans.node.findAll()) {
    //   console.info(child.node.id)
    // }

    transactionSearchConfig.addDependency(
      // Note that findChild() only searches *direct* children, so we need to traverse the tree until we reach the resource policy.
      transactionSearchSpans.node
        .findChild("Policy")
        .node.findChild("ResourcePolicy") as CfnResourcePolicy,
    )
    transactionSearchConfig.addDependency(
      // Note that findChild() only searches *direct* children, so we need to traverse the tree until we reach the resource policy.
      applicationSignalsData.node
        .findChild("Policy")
        .node.findChild("ResourcePolicy") as CfnResourcePolicy,
    )
  }
}
