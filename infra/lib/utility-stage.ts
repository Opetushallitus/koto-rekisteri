import { Stage, StageProps } from "aws-cdk-lib"
import { Construct } from "constructs"
import { GithubActionsStack } from "./github-actions-stack"
import { ImageBuildsStack } from "./image-builds-stack"

export class UtilityStage extends Stage {
  readonly imageBuildsStack: ImageBuildsStack

  constructor(scope: Construct, id: string, props: StageProps) {
    super(scope, id, props)

    new GithubActionsStack(this, "GithubActions", {
      env: props.env,
    })

    this.imageBuildsStack = new ImageBuildsStack(this, "ImageBuilds", props)
  }
}
