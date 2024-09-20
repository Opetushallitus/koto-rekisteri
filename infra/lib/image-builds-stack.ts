import * as cdk from "aws-cdk-lib"
import { aws_ecr_assets, StackProps } from "aws-cdk-lib"
import { DockerImageAsset, Platform } from "aws-cdk-lib/aws-ecr-assets"
import { Construct } from "constructs"
import { GithubActionsStack } from "./github-actions-stack"

export class ImageBuildsStack extends cdk.Stack {
  readonly serviceImage: DockerImageAsset

  constructor(scope: Construct, id: string, props: StackProps) {
    super(scope, id, props)

    this.serviceImage = new aws_ecr_assets.DockerImageAsset(
      this,
      "ServiceImage",
      {
        directory: "..",
        platform: Platform.LINUX_AMD64,
      },
    )
  }
}
