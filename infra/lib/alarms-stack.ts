import * as cdk from "aws-cdk-lib"
import { aws_sns, PhysicalName, StackProps } from "aws-cdk-lib"
import { Construct } from "constructs"

export interface AlarmsStackProps extends StackProps {}

export class AlarmsStack extends cdk.Stack {
  readonly alarmSnsTopic: aws_sns.Topic

  constructor(scope: Construct, id: string, props: AlarmsStackProps) {
    super(scope, id, props)

    this.alarmSnsTopic = new aws_sns.Topic(this, "AlarmSnsTopic", {
      topicName: PhysicalName.GENERATE_IF_NEEDED,
    })
  }
}
