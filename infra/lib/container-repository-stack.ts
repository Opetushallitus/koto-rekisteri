import * as cdk from "aws-cdk-lib"
import { StackProps } from "aws-cdk-lib"
import { Repository, TagMutability } from "aws-cdk-lib/aws-ecr"
import { AccountPrincipal } from "aws-cdk-lib/aws-iam"
import { Construct } from "constructs"

interface ImageBuildStackProps extends StackProps {
  allowPullsFromAccounts: string[]
}

export class ContainerRepositoryStack extends cdk.Stack {
  readonly repository: Repository

  constructor(scope: Construct, id: string, props: ImageBuildStackProps) {
    super(scope, id, props)

    this.repository = new Repository(this, "Service", {
      repositoryName: "kitu",
      imageTagMutability: TagMutability.IMMUTABLE,
    })

    for (const accountId of props.allowPullsFromAccounts) {
      this.repository.grantPull(new AccountPrincipal(accountId))
    }
  }
}
