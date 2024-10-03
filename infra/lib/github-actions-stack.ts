import { Stack, StackProps } from "aws-cdk-lib"
import {
  OpenIdConnectPrincipal,
  OpenIdConnectProvider,
  PolicyStatement,
  Role,
} from "aws-cdk-lib/aws-iam"
import { Construct } from "constructs"

export class GithubActionsStack extends Stack {
  public githubActionsRole: Role

  constructor(scope: Construct, id: string, props?: StackProps) {
    super(scope, id, props)

    const githubActionsOidcProvider = new OpenIdConnectProvider(
      this,
      "GithubActionsIdentityProvider",
      {
        url: "https://token.actions.githubusercontent.com",
        clientIds: ["sts.amazonaws.com"],
      },
    )

    this.githubActionsRole = new Role(this, "GithubActionsRole", {
      roleName: `kitu-github-actions-role`,
      assumedBy: new OpenIdConnectPrincipal(githubActionsOidcProvider, {
        StringEquals: {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
        },
        StringLike: {
          "token.actions.githubusercontent.com:sub":
            "repo:Opetushallitus/koto-rekisteri:*",
        },
      }),
    })

    this.githubActionsRole.addToPolicy(
      new PolicyStatement({
        sid: "AllowAssumingCDKRoles",
        actions: ["sts:AssumeRole", "iam:PassRole"],
        resources: [
          "arn:aws:iam::*:role/cdk-readOnlyRole",
          "arn:aws:iam::*:role/cdk-hnb659fds-deploy-role-*",
          "arn:aws:iam::*:role/cdk-hnb659fds-file-publishing-*",
          "arn:aws:iam::*:role/cdk-hnb659fds-image-publishing-*",
          "arn:aws:iam::*:role/cdk-hnb659fds-lookup-*",
        ],
      }),
    )
    this.githubActionsRole.addToPolicy(
      new PolicyStatement({
        sid: "AllowLiveTailing",
        actions: ["logs:StartLiveTail"],
        resources: ["*"],
      }),
    )
  }
}
