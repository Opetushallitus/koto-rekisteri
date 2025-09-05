import { CloudWatchLogsEvent } from "aws-lambda"
import { getAuditLogEntry, parse } from "./parser"
import { SendMessageCommand, SQSClient } from "@aws-sdk/client-sqs"
import { getQueueUrl } from "./queueUrl"
import { getAssumeRole } from "./assumeRole"

export const handler = async (event: CloudWatchLogsEvent) => {
  console.log("received event:", event)

  const data = parse(event)
  console.log("parsed CloudWatchData:", data)

  const auditLogEntry = getAuditLogEntry(data)
  console.log("parsed auditLogEntry:", auditLogEntry)

  const QueueUrl = await getQueueUrl()
  console.log("Sending sqs:sendMessage to", QueueUrl)

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
    QueueUrl,

    // TODO: Send all, and maybe you should use batch send instead?
    MessageBody: auditLogEntry[0].toString(),
  })

  try {
    const data = await sqs.send(command)
    console.log("we got the response from sqs:", data)
    return {
      statusCode: 200,
      message: "ok",
      data,
    }
  } catch (error) {
    console.log("unable to send to sqs. Error:", error)
    return {
      statusCode: 500,
      message: "unknown error",
      error,
    }
  }
}
