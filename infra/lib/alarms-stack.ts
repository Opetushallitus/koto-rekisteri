import * as cdk from "aws-cdk-lib"
import { aws_lambda, aws_lambda_nodejs, StackProps } from "aws-cdk-lib"
import { TreatMissingData } from "aws-cdk-lib/aws-cloudwatch"
import { NodejsFunction, OutputFormat } from "aws-cdk-lib/aws-lambda-nodejs"
import { Secret } from "aws-cdk-lib/aws-secretsmanager"
import { Construct } from "constructs"
import * as path from "node:path"

export class AlarmsStack extends cdk.Stack {
  readonly slackNotifierLambda: NodejsFunction

  constructor(scope: Construct, id: string, props: StackProps) {
    super(scope, id, props)

    const slackWebhookUrlSecretName = `slack-webhook-url`

    this.slackNotifierLambda = new aws_lambda_nodejs.NodejsFunction(
      this,
      "SlackNotifierLambda",
      {
        runtime: aws_lambda.Runtime.NODEJS_20_X,
        entry: path.join(__dirname, "lambdas/slackNotifierLambda/index.mts"),
        handler: "handler",
        timeout: cdk.Duration.seconds(60),
        environment: {
          SLACK_WEBHOOK_URL_SECRET_NAME: slackWebhookUrlSecretName,
        },
        bundling: {
          format: OutputFormat.ESM,
        },
      },
    )

    Secret.fromSecretNameV2(
      this,
      "SlackWebhookUrlSecret",
      slackWebhookUrlSecretName,
    ).grantRead(this.slackNotifierLambda)

    // Can't send alarms from this metric this Lambda, since that would create an infinite loop.
    this.slackNotifierLambda
      .metricErrors()
      .createAlarm(this, "SlackNotifierLambdaErrors", {
        threshold: 1,
        evaluationPeriods: 1,
        treatMissingData: TreatMissingData.NOT_BREACHING,
      })
  }
}
