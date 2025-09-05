import { CloudWatchLogsEvent } from "aws-lambda"
import { getAuditLogEntry, parse } from "./parser"
import { SendMessageCommand, SQSClient } from "@aws-sdk/client-sqs"
import { getQueueUrl } from "./queueUrl"
import { getAssumeRole } from "./assumeRole"

export const handler = async (event: CloudWatchLogsEvent) => {
  const data = parse(event)
  const auditLogEntry = getAuditLogEntry(data)

  // TODO: Fetch these from SSM
  const region = "eu-west-1"
  const kosskiAccountId = "500150530292"

  const { Credentials } = await getAssumeRole("eu-west-1", kosskiAccountId)
  if (!Credentials) {
    throw `AssumeRole did not find credentials for account '${kosskiAccountId}'.`
  }

  const sqs = new SQSClient([
    {
      region,
      credentials: {
        accessKeyId: Credentials.AccessKeyId,
        secretAccessKey: Credentials.SecretAccessKey,
        sessionToken: Credentials.SessionToken,
        expiration: Credentials.Expiration,
      },
    },
  ])

  const command = new SendMessageCommand({
    QueueUrl: await getQueueUrl(),

    // TODO: Send all, and maybe you should use batch send instead?
    MessageBody: auditLogEntry[0].toString(),
  })

  try {
    const data = await sqs.send(command)
    return {
      statusCode: 200,
      message: "ok",
      data,
    }
  } catch (error) {
    return {
      statusCode: 500,
      message: "unknown error",
      error,
    }
  }
}
