import * as cdk from "aws-cdk-lib"
import { aws_chatbot, StackProps } from "aws-cdk-lib"
import { Construct } from "constructs"
import { ITopic } from "aws-cdk-lib/aws-sns"
import { LoggingLevel } from "aws-cdk-lib/aws-chatbot"
import { SlackChannel } from "./accounts"

export interface SlackBotProps extends StackProps {
  alarmTopics: ITopic[]
  slackWorkspaceId: string
  slackChannel: SlackChannel
}

export class SlackBotStack extends cdk.Stack {
  readonly channelConfiguration: aws_chatbot.SlackChannelConfiguration

  constructor(scope: Construct, id: string, props: SlackBotProps) {
    super(scope, id, props)

    this.channelConfiguration = new aws_chatbot.SlackChannelConfiguration(
      this,
      "SlackBot",
      {
        slackChannelId: props.slackChannel.id,
        slackWorkspaceId: props.slackWorkspaceId,
        slackChannelConfigurationName: props.slackChannel.name,
        notificationTopics: props.alarmTopics,
        loggingLevel: LoggingLevel.INFO,
      },
    )
  }
}
