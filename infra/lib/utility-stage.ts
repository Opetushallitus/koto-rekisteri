import { Stage, StageProps } from "aws-cdk-lib"
import { PolicyStatement } from "aws-cdk-lib/aws-iam"
import { Construct } from "constructs"
import { ContainerRepositoryStack } from "./container-repository-stack"
import { GithubActionsStack } from "./github-actions-stack"
import { AlarmsStack } from "./alarms-stack"
import { SlackBotStack } from "./slack-bot-stack"

interface UtilityStageProps extends StageProps {
  allowPullsFromAccounts: string[]
  slackChannelName: string
  slackChannelId: string
  slackWorkspaceId: string
}

export class UtilityStage extends Stage {
  readonly imageBuildsStack: ContainerRepositoryStack

  constructor(scope: Construct, id: string, props: UtilityStageProps) {
    super(scope, id, props)

    const githubActionsStack = new GithubActionsStack(this, "GithubActions", {
      env: props.env,
    })

    const alarmsStack = new AlarmsStack(this, "Alarms", {
      env: props.env,
    })

    new SlackBotStack(this, "SlackBot", {
      env: props.env,
      slackChannelName: props.slackChannelName,
      slackChannelId: props.slackChannelId,
      slackWorkspaceId: props.slackWorkspaceId,
      alarmTopics: [alarmsStack.alarmSnsTopic],
    })

    this.imageBuildsStack = new ContainerRepositoryStack(this, "ImageBuilds", {
      env: props.env,
      allowPullsFromAccounts: props.allowPullsFromAccounts,
      vulnerabilitiesSnsTopic: alarmsStack.alarmSnsTopic,
    })

    githubActionsStack.githubActionsRole.addToPolicy(
      new PolicyStatement({
        actions: ["ecr:GetAuthorizationToken"],
        resources: ["*"],
      }),
    )

    this.imageBuildsStack.repository.grantPullPush(
      githubActionsStack.githubActionsRole,
    )
  }
}
