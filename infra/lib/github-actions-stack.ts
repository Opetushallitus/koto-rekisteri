import * as cdk from "aws-cdk-lib";
import { Construct } from "constructs";
import * as iam from "aws-cdk-lib/aws-iam";

export const GITHUB_ACTIONS_OIDC_THUMBPRINT_LIST = [
  "6938fd4d98bab03faadb97b34396831e3780aea1",
  "1c58a3a8518e8759bf075b76b750d4f2df264fcd",
];

export class GithubActionsStack extends cdk.Stack {
  public githubActionsRole: iam.Role;

  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const githubActionsOidcProvider = new iam.CfnOIDCProvider(
      this,
      "GithubActionsIdentityProvider",
      {
        url: "https://token.actions.githubusercontent.com",
        clientIdList: ["sts.amazonaws.com"],
        thumbprintList: GITHUB_ACTIONS_OIDC_THUMBPRINT_LIST,
      },
    );

    this.githubActionsRole = new iam.Role(this, "GithubActionsRole", {
      roleName: `kitu-github-actions-role`,
      assumedBy: new iam.FederatedPrincipal(
        githubActionsOidcProvider.attrArn,
        {
          StringEquals: {
            "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
          },
          StringLike: {
            "token.actions.githubusercontent.com:sub":
              "repo:Opetushallitus/koto-rekisteri:*",
          },
        },
        "sts:AssumeRoleWithWebIdentity",
      ),
    });

    const cdkPolicyStatement = new iam.PolicyStatement({
      actions: ["sts:AssumeRole", "iam:PassRole"],
      resources: [
        "arn:aws:iam::*:role/cdk-readOnlyRole",
        "arn:aws:iam::*:role/cdk-hnb659fds-deploy-role-*",
        "arn:aws:iam::*:role/cdk-hnb659fds-file-publishing-*",
        "arn:aws:iam::682033502734:role/cdk-hnb659fds-lookup-*",
      ],
    });
    this.githubActionsRole.addToPolicy(cdkPolicyStatement);
  }
}
