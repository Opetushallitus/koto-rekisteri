import { CloudWatchLogsEvent } from "aws-lambda"
import { gunzipSync } from "zlib"

export const parse = (event: CloudWatchLogsEvent) => {
  const decoded = Buffer.from(event.awslogs.data, "base64")
  const decompressed = gunzipSync(decoded)
  return JSON.parse(decompressed.toString())
}
