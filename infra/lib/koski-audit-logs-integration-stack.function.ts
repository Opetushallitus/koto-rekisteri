import { gunzipSync } from "node:zlib"
import { CloudWatchLogsEvent } from "aws-lambda"

export const handler = async (event: CloudWatchLogsEvent) => {
  const payload = Buffer.from(event.awslogs.data, "base64")
  const decompressed = gunzipSync(payload)
  const { logEvents } = JSON.parse(decompressed.toString())

  // TODO: Send to SQS

  return {
    statusCode: 200,
    message: "ok",
  }
}
