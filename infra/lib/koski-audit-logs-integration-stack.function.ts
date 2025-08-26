import { gunzipSync } from "node:zlib"
import { CloudWatchLogsEvent } from "aws-lambda"
import {
  SQSClient,
  ListQueuesCommand,
  ListQueuesCommandInput,
  SQSServiceException,
  SendMessageCommand,
  SendMessageCommandInput,
} from "@aws-sdk/client-sqs"

export const handler = async (event: CloudWatchLogsEvent) => {
  const payload = Buffer.from(event.awslogs.data, "base64")
  const decompressed = gunzipSync(payload)
  const { logEvents } = JSON.parse(decompressed.toString())

  // TODO: Send to SQS
  const client = new SQSClient({ region: "REGION" })

  // "oma-opintopolku-loki-audit-queue"
  //
  // Untuva:  https://sqs.eu-west-1.amazonaws.com/500150530292/oma-opintopolku-loki-audit-queue
  // QA:      https://sqs.eu-west-1.amazonaws.com/692437769085/oma-opintopolku-loki-audit-queue
  // https://github.com/Opetushallitus/varda/blob/main/applications/backend/webapps/varda/audit_log/audit_log.py

  const koskiSqsQueArn = "TODO" // Tallenna se CDK:lla Parameter storeen

  const command = new SendMessageCommand({
    QueueUrl: "TODO",
    MessageBody: logEvents,
  })

  // async/await.
  try {
    // process data.
    const data = await client.send(command)
    return {
      statusCode: 200,
      message: "ok",
      data,
    }
  } catch (error) {
    console.log("An error occurred during lambda sending to SQS:")
    console.log(JSON.stringify(error))
    if (error instanceof SQSServiceException) {
      return {
        statusCode: 500,
        message: "SQSServiceException",
        error: error.name,
      }
    } else {
      return {
        statusCode: 500,
        message: "unknownerror",
        error,
      }
    }
  }
}
