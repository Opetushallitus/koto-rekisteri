import { Stack, StackProps } from "aws-cdk-lib"
import { LogGroup } from "aws-cdk-lib/aws-logs"
import { Construct } from "constructs"

export interface LogGroupsStackProps extends StackProps {}

export class LogGroupsStack extends Stack {
  readonly serviceLogGroup: LogGroup

  constructor(scope: Construct, id: string, props: LogGroupsStackProps) {
    super(scope, id, props)

    this.serviceLogGroup = new LogGroup(this, "Service", {
      logGroupName: "KituService",
    })
  }
}
