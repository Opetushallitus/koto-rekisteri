import * as cdk from "aws-cdk-lib"
import { aws_lambda, aws_lambda_nodejs, aws_sns, StackProps } from "aws-cdk-lib"
import { Code } from "aws-cdk-lib/aws-lambda"
import { Secret } from "aws-cdk-lib/aws-secretsmanager"
import {
  LambdaSubscription,
  UrlSubscription,
} from "aws-cdk-lib/aws-sns-subscriptions"
import { Construct } from "constructs"

export class AlarmsStack extends cdk.Stack {
  readonly alarmSnsTopic

  constructor(scope: Construct, id: string, props: StackProps) {
    super(scope, id, props)

    this.alarmSnsTopic = new aws_sns.Topic(this, "AlarmSnsTopic")

    const slackWebhookUrlSecretName = `SlackWebhookUrl`

    const slackNotifierLambda = new aws_lambda_nodejs.NodejsFunction(
      this,
      "SlackNotifierLambda",
      {
        runtime: aws_lambda.Runtime.NODEJS_20_X,
        handler: "index.handler",
        timeout: cdk.Duration.seconds(60),
        code: Code.fromAsset("./lambdas/slackNotifierLambda"),
        environment: {
          SLACK_WEBHOOK_URL_SECRET_NAME: slackWebhookUrlSecretName,
        },
      },
    )

    this.alarmSnsTopic.addSubscription(
      new LambdaSubscription(slackNotifierLambda),
    )
    this.alarmSnsTopic.addSubscription(
      new UrlSubscription(process.env["PAGERDUTY_ENDPOINT"]!),
    )

    Secret.fromSecretNameV2(
      this,
      "SlackWebhookUrlSecret",
      slackWebhookUrlSecretName,
    ).grantRead(slackNotifierLambda)
  }
}
