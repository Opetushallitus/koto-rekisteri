import { CloudWatchLogsEvent } from "aws-lambda"
import { parse } from "./parser"
import { SendMessageCommand, SQSClient } from "@aws-sdk/client-sqs"

export const handler = async (event: CloudWatchLogsEvent) => {
  const { logEvents } = parse(event)

  const sqs = new SQSClient({ region: "eu-west-1" })
  const command = new SendMessageCommand({
    QueueUrl:
      // TODO: Get the queue url from Parameter store
      "https://sqs.eu-west-1.amazonaws.com/500150530292/oma-opintopolku-loki-audit-queue",
    MessageBody: logEvents,
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
