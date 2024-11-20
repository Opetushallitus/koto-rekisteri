import * as cdk from "aws-cdk-lib"
import {
  aws_chatbot,
  aws_lambda,
  aws_lambda_nodejs,
  aws_sns,
  StackProps,
} from "aws-cdk-lib"
import { TreatMissingData } from "aws-cdk-lib/aws-cloudwatch"
import { OutputFormat } from "aws-cdk-lib/aws-lambda-nodejs"
import { Secret } from "aws-cdk-lib/aws-secretsmanager"
import { LambdaSubscription } from "aws-cdk-lib/aws-sns-subscriptions"
import { Construct } from "constructs"
import * as path from "node:path"

export interface AlarmsStackProps extends StackProps {
  slackWorkspaceId: string
  slackChannelId: string
  slackChannelName: string
}

export class AlarmsStack extends cdk.Stack {
  readonly alarmSnsTopic: aws_sns.Topic
  readonly channelConfiguration: aws_chatbot.SlackChannelConfiguration

  constructor(scope: Construct, id: string, props: AlarmsStackProps) {
    super(scope, id, props)

    this.alarmSnsTopic = new aws_sns.Topic(this, "AlarmSnsTopic")

    const slackWebhookUrlSecretName = `slack-webhook-url`

    const slackNotifierLambda = new aws_lambda_nodejs.NodejsFunction(
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

    this.alarmSnsTopic.addSubscription(
      new LambdaSubscription(slackNotifierLambda),
    )

    Secret.fromSecretNameV2(
      this,
      "SlackWebhookUrlSecret",
      slackWebhookUrlSecretName,
    ).grantRead(slackNotifierLambda)

    // Can't send alarms from this to the SNS topic, since that would create an infinite loop.
    slackNotifierLambda
      .metricErrors()
      .createAlarm(this, "SlackNotifierLambdaErrors", {
        threshold: 1,
        evaluationPeriods: 1,
        treatMissingData: TreatMissingData.NOT_BREACHING,
      })

    this.channelConfiguration = new aws_chatbot.SlackChannelConfiguration(
      this,
      "SlackBot",
      {
        slackChannelId: props.slackChannelId,
        slackWorkspaceId: props.slackWorkspaceId,
        slackChannelConfigurationName: props.slackChannelName,
        notificationTopics: [this.alarmSnsTopic],
      },
    )
  }
}
