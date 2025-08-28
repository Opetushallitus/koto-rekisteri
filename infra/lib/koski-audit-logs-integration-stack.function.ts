import { gunzipSync } from "node:zlib"
import { CloudWatchLogsEvent } from "aws-lambda"
import { SQSClient, SendMessageCommand } from "@aws-sdk/client-sqs"

export const handler = async (event: CloudWatchLogsEvent) => {
  const payload = Buffer.from(event.awslogs.data, "base64")
  const decompressed = gunzipSync(payload)
  const { logEvents } = JSON.parse(decompressed.toString())

  const client = new SQSClient({ region: "REGION" })
  const command = new SendMessageCommand({
    QueueUrl:
      // TODO: Get the queue url from Parameter store
      "https://sqs.eu-west-1.amazonaws.com/500150530292/oma-opintopolku-loki-audit-queue",
    MessageBody: logEvents,
  })

  try {
    const data = await client.send(command)
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
