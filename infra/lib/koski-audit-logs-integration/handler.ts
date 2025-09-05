import { CloudWatchLogsEvent } from "aws-lambda"
import { getAuditLogEntry, parse } from "./parser"
import { SendMessageCommand, SQSClient } from "@aws-sdk/client-sqs"
import { getQueueUrl } from "./queueUrl"

const sqs = new SQSClient({ region: "eu-west-1" })
export const handler = async (event: CloudWatchLogsEvent) => {
  const data = parse(event)
  const auditLogEntry = getAuditLogEntry(data)

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
