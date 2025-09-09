import { CloudWatchLogsEvent } from "aws-lambda"
import { getAuditLogEntry, parse } from "./parser"
import { SendMessageCommand, SQSClient } from "@aws-sdk/client-sqs"
import { getQueueUrl } from "./queueUrl"
import { getAssumeRole } from "./assumeRole"

export const handler = async (event: CloudWatchLogsEvent) => {
  const data = parse(event)
  const auditLogEntry = getAuditLogEntry(data)

export const handler = async (event: CloudWatchLogsEvent) => {
  const QueueUrl = process.env.KOSKI_SQS_QUEUE_URL
  if (!QueueUrl) {
    throw "Cannot proceed, because 'KOSKI_SQS_QUEUE_URL' is missing."
  }

  const roleArn = process.env.KOSKI_ROLE_ARN
  if (!roleArn) {
    throw "Cannot proceed, because 'KOSKI_ROLE_ARN' is missing."
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
