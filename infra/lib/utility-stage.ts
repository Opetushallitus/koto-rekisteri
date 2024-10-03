import { Stage, StageProps } from "aws-cdk-lib"
import { PolicyStatement } from "aws-cdk-lib/aws-iam"
import { Construct } from "constructs"
import { ContainerRepositoryStack } from "./container-repository-stack"
import { GithubActionsStack } from "./github-actions-stack"

interface UtilityStageProps extends StageProps {
  allowPullsFromAccounts: string[]
}

export class UtilityStage extends Stage {
  readonly imageBuildsStack: ContainerRepositoryStack

  constructor(scope: Construct, id: string, props: UtilityStageProps) {
    super(scope, id, props)

    const githubActionsStack = new GithubActionsStack(this, "GithubActions", {
      env: props.env,
    })

    this.imageBuildsStack = new ContainerRepositoryStack(
      this,
      "ImageBuilds",
      props,
    )

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
