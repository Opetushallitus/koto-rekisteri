import { StackProps, Stage, StageProps } from "aws-cdk-lib"
import { ManagedPolicy, PolicyStatement } from "aws-cdk-lib/aws-iam"
import { Construct } from "constructs"
import { GithubActionsStack } from "./github-actions-stack"
import { ContainerRepositoryStack } from "./container-repository-stack"

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

    githubActionsStack.githubActionsRole.addToPolicy(
      new PolicyStatement({
        actions: ["ecr:GetAuthorizationToken"],
        resources: ["*"],
      }),
    )

    this.imageBuildsStack = new ContainerRepositoryStack(
      this,
      "ImageBuilds",
      props,
    )
  }
}
