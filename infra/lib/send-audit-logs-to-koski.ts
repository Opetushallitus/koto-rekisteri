import { Construct } from "constructs"
import { NodejsFunction } from "aws-cdk-lib/aws-lambda-nodejs"
import { LambdaRestApi } from "aws-cdk-lib/aws-apigateway"
import { Runtime } from "aws-cdk-lib/aws-lambda"

export class SendAuditLogsToKoski extends Construct {
  function: NodejsFunction

  constructor(scope: Construct, id: string) {
    super(scope, id)
    this.function = new NodejsFunction(this, "function", {
      runtime: Runtime.NODEJS_LATEST,
    })
  }
}
