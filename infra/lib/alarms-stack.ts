import * as cdk from "aws-cdk-lib"
import { aws_sns, PhysicalName, StackProps } from "aws-cdk-lib"
import { Construct } from "constructs"
import {
  Effect,
  ManagedPolicy,
  PolicyDocument,
  PolicyStatement,
  Role,
  ServicePrincipal,
} from "aws-cdk-lib/aws-iam"
import { CfnInvestigationGroup } from "aws-cdk-lib/aws-aiops"
import {
  AlarmActionConfig,
  IAlarm,
  IAlarmAction,
} from "aws-cdk-lib/aws-cloudwatch"

export interface AlarmsStackProps extends StackProps {}

export class AlarmsStack extends cdk.Stack {
  readonly alarmSnsTopic: aws_sns.Topic
  readonly infoSnsTopic: aws_sns.Topic
  readonly investigationGroup: CfnInvestigationGroup
  readonly investigationAction: IAlarmAction

  constructor(scope: Construct, id: string, props: AlarmsStackProps) {
    super(scope, id, props)

    this.alarmSnsTopic = this.createSnsTopic("AlarmSnsTopic")
    this.infoSnsTopic = this.createSnsTopic("InfoSnsTopic")

    this.investigationGroup = this.createInvestigationGroup()
    this.investigationAction = new InvestigationGroupAlarmAction(
      this.investigationGroup.attrArn,
    )
  }

  private createSnsTopic(id: string) {
    const topic = new aws_sns.Topic(this, id, {
      topicName: PhysicalName.GENERATE_IF_NEEDED,
    })

    topic.grantPublish(new ServicePrincipal("cloudwatch.amazonaws.com"))

    return topic
  }

  private createInvestigationGroup() {
    const role = new Role(this, "InvestigationGroupRole", {
      assumedBy: new ServicePrincipal("aiops.amazonaws.com"),
      managedPolicies: [
        ManagedPolicy.fromAwsManagedPolicyName("AIOpsAssistantPolicy"),
      ],
    })

    // CloudWatch alarms can't start investigations unless the group's resource
    // policy grants aiops.alarms.cloudwatch.amazonaws.com explicit access.
    const alarmsPolicy = new PolicyDocument({
      statements: [
        new PolicyStatement({
          effect: Effect.ALLOW,
          principals: [
            new ServicePrincipal("aiops.alarms.cloudwatch.amazonaws.com"),
          ],
          actions: [
            "aiops:CreateInvestigation",
            "aiops:CreateInvestigationEvent",
          ],
          resources: ["*"],
          conditions: {
            StringEquals: { "aws:SourceAccount": this.account },
            ArnLike: {
              "aws:SourceArn": `arn:${this.partition}:cloudwatch:${this.region}:${this.account}:alarm:*`,
            },
          },
        }),
      ],
    })

    return new CfnInvestigationGroup(this, "InvestigationGroup", {
      name: `kitu-${this.stackName}`,
      roleArn: role.roleArn,
      retentionInDays: 90,
      investigationGroupPolicy: JSON.stringify(alarmsPolicy.toJSON()),
    })
  }
}

// Adding an investigation group's ARN as an alarm action tells CloudWatch to
// auto-start an Amazon Q investigation when the alarm transitions to ALARM.
// There is no built-in IAlarmAction for this in aws-cdk-lib yet.
class InvestigationGroupAlarmAction implements IAlarmAction {
  constructor(private readonly investigationGroupArn: string) {}

  bind(_scope: Construct, _alarm: IAlarm): AlarmActionConfig {
    return { alarmActionArn: this.investigationGroupArn }
  }
}
