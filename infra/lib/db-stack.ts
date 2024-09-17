import * as cdk from "aws-cdk-lib"
import { Construct } from "constructs"

export interface DbStackProps extends cdk.StackProps {}

export class DbStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: DbStackProps) {
    super(scope, id, props)
  }
}
