import { SendMessageCommand, SQSClient } from "@aws-sdk/client-sqs"
import { fromTemporaryCredentials } from "@aws-sdk/credential-providers"
import { CloudWatchLogsEvent } from "aws-lambda"
import { getAuditLogEntry, parse } from "./parser"

export const handler = async (event: CloudWatchLogsEvent) => {
  const QueueUrl = process.env.KOSKI_SQS_QUEUE_URL
  if (!QueueUrl) {
    throw "Cannot proceed, because 'KOSKI_SQS_QUEUE_URL' is missing."
  }

  const roleArn = process.env.KOSKI_ROLE_ARN
  if (!roleArn) {
    throw "Cannot proceed, because 'KOSKI_ROLE_ARN' is missing."
  }

  const data = parse(event)
  const auditLogEntry = getAuditLogEntry(data)

  const sqs = new SQSClient({
    credentials: fromTemporaryCredentials({
      params: { RoleArn: roleArn },
    }),
  })

  const command = new SendMessageCommand({
    QueueUrl,

    // TODO: Send all, and maybe you should use batch send instead?
    MessageBody: auditLogEntry[0].toString(),
  })

  try {
    const data = await sqs.send(command)
    console.log("data sent to sqs", data)
    return {
      statusCode: 200,
      message: "ok",
      data,
    }
  } catch (error) {
    console.log("failed to send to sqs", error)
    return {
      statusCode: 500,
      message: "unknown error",
      error,
    }
  }
}
