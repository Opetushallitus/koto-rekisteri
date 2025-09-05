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
  console.log(
    `trying to get role with RoleArn: ${RoleArn} and RoleSessionName: ${RoleSessionName}`,
  )

  const sts = new STSClient({ region })
  console.log("sts client created")

  const output = await sts.send(
    new AssumeRoleCommand({ RoleArn, RoleSessionName }),
  )
  console.log("sts received:", {
    AssumeRoleUser: {
      Arn: output.AssumedRoleUser?.Arn,
      AssumedRoleId: output.AssumedRoleUser?.AssumedRoleId,
    },
    Credentials: {
      $metadata: output.$metadata,
      "AccessKeyId-first3": output.Credentials?.AccessKeyId?.substring(0, 3),
      "SecretAccessKey-first3": output.Credentials?.SecretAccessKey?.substring(
        0,
        3,
      ),
      Expiration: output.Credentials?.Expiration,
      "SessionToken-first3": output.Credentials?.SessionToken?.substring(0, 3),
    },
    SourceIdentity: output.SourceIdentity,
    PackedPolicySize: output.PackedPolicySize,
  })

  return output
}
