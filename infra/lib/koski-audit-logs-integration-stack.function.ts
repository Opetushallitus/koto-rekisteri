import { gunzipSync } from "node:zlib"
import { CloudWatchLogsEvent } from "aws-lambda"
import { SQSClient, SendMessageCommand } from "@aws-sdk/client-sqs"
import { SSMClient, GetParameterCommand } from "@aws-sdk/client-ssm"

const sqs = new SQSClient({ region: "REGION" })
const ssm = new SSMClient()
const ssmParamName = "/kitu/koski-integration/oma-opintopolku-loki-audit-queue"
const ssmCommand = new GetParameterCommand({ Name: ssmParamName })
let queueUrl: string

export const handler = async (event: CloudWatchLogsEvent) => {
  if (!queueUrl) {
    queueUrl =
      (await ssm.send(ssmCommand)).Parameter?.Value ??
      ((): string => {
        throw `Could not find parameter with for  ${ssmParamName}.`
      })()
  }

  const payload = Buffer.from(event.awslogs.data, "base64")
  const decompressed = gunzipSync(payload)
  const { logEvents } = JSON.parse(decompressed.toString())

  const command = new SendMessageCommand({
    QueueUrl: queueUrl,
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
