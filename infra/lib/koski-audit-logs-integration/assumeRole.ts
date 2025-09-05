import {
  STSClient,
  AssumeRoleCommand,
  AssumeRoleCommandOutput,
} from "@aws-sdk/client-sts"
export const getAssumeRole = async (
  region: string,
  koskiAccountId: string,
): Promise<AssumeRoleCommandOutput> => {
  const RoleArn = `arn:aws:iam::${koskiAccountId}:role/kitu-sqs-sender`
  const RoleSessionName = "OmaOpintopolkuLokiLambdaSession"
  const sts = new STSClient({ region })
  return await sts.send(new AssumeRoleCommand({ RoleArn, RoleSessionName }))
}
